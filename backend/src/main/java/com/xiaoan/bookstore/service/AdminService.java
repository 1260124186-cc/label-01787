package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.annotation.TenantIgnore;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.AdminLoginDTO;
import com.xiaoan.bookstore.entity.*;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.*;
import com.xiaoan.bookstore.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@TenantIgnore
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    private final AdminUserMapper adminUserMapper;
    private final UserMapper userMapper;
    private final BookMapper bookMapper;
    private final ReadingRecordMapper readingRecordMapper;
    private final OperationLogMapper operationLogMapper;
    private final FileDownloadLogMapper fileDownloadLogMapper;
    private final JwtUtil jwtUtil;
    private final RbacService rbacService;
    private final GroupService groupService;
    private final com.xiaoan.bookstore.service.reader.PdfReaderAdapter pdfReaderAdapter;

    private String decodePassword(String encoded) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encoded);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return encoded;
        }
    }

    public Map<String, Object> login(AdminLoginDTO dto) {
        AdminUser admin = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, dto.getUsername())
        );
        if (admin == null) {
            throw new BusinessException("用户名或密码错误");
        }
        String rawPassword = decodePassword(dto.getPassword());
        if (!verifyPassword(rawPassword, admin.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (admin.getStatus() != Constants.STATUS_ENABLED) {
            throw new BusinessException("账号已被禁用");
        }

        String token = jwtUtil.generateAdminToken(admin.getId(), admin.getRoleId());
        String roleCode = null;
        java.util.List<String> permissions = java.util.Collections.emptyList();
        if (admin.getRoleId() != null) {
            Role role = rbacService.getRoleWithPermissions(admin.getRoleId());
            if (role != null) {
                roleCode = role.getCode();
                permissions = role.getPermissions() != null
                        ? role.getPermissions().stream().map(Permission::getCode).collect(java.util.stream.Collectors.toList())
                        : java.util.Collections.emptyList();
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("nickname", admin.getNickname());
        result.put("username", admin.getUsername());
        result.put("roleId", admin.getRoleId());
        result.put("roleCode", roleCode);
        result.put("permissions", permissions);
        log.info("管理员登录成功: {}", admin.getUsername());
        return result;
    }

    private boolean verifyPassword(String raw, String encoded) {
        if (encoded.startsWith("$2a$")) {
            return PASSWORD_ENCODER.matches(raw, encoded);
        }
        return encoded.equals(raw);
    }

    public Map<String, Object> dashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("userCount", userMapper.selectCount(null));
        data.put("bookCount", bookMapper.selectCount(
                new LambdaQueryWrapper<Book>().eq(Book::getStatus, Constants.STATUS_ENABLED)
        ));
        Long totalDuration = readingRecordMapper.sumDuration(null,
                java.time.LocalDate.of(2020, 1, 1).atStartOfDay(),
                java.time.LocalDateTime.now());
        data.put("totalReadingSeconds", totalDuration != null ? totalDuration : 0L);
        data.put("logCount", operationLogMapper.selectCount(null));
        data.putAll(groupService.adminDashboard());
        return data;
    }

    public Page<User> userList(int page, int size, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(User::getNickname, keyword);
        }
        wrapper.orderByDesc(User::getCreatedAt);
        return userMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Page<Book> bookList(int page, int size, String keyword, String format) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Book::getTitle, keyword);
        }
        if (format != null && !format.isEmpty()) {
            wrapper.eq(Book::getBookFormat, format);
        }
        wrapper.ne(Book::getStatus, Constants.STATUS_DISABLED);
        wrapper.orderByDesc(Book::getCreatedAt);
        return bookMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Map<String, Object> bookFormatStats() {
        Map<String, Object> result = new HashMap<>();
        long totalCount = bookMapper.selectCount(
                new LambdaQueryWrapper<Book>().ne(Book::getStatus, Constants.STATUS_DISABLED)
        );
        result.put("total", totalCount);

        List<Map<String, Object>> formatStats = new ArrayList<>();
        String[] formats = {Constants.FORMAT_PDF, Constants.FORMAT_EPUB, Constants.FORMAT_MOBI};
        for (String fmt : formats) {
            long count = bookMapper.selectCount(
                    new LambdaQueryWrapper<Book>()
                            .eq(Book::getBookFormat, fmt)
                            .ne(Book::getStatus, Constants.STATUS_DISABLED)
            );
            Map<String, Object> stat = new HashMap<>();
            stat.put("format", fmt);
            stat.put("count", count);
            stat.put("percentage", totalCount > 0 ? String.format("%.1f", (double) count / totalCount * 100) : "0.0");
            formatStats.add(stat);
        }
        result.put("formats", formatStats);
        return result;
    }

    public Page<OperationLog> logList(int page, int size, String action, String ip,
                                       LocalDateTime startTime, LocalDateTime endTime, Integer userType) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (action != null && !action.trim().isEmpty()) {
            wrapper.like(OperationLog::getAction, action.trim());
        }
        if (ip != null && !ip.trim().isEmpty()) {
            wrapper.like(OperationLog::getIp, ip.trim());
        }
        if (startTime != null) {
            wrapper.ge(OperationLog::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(OperationLog::getCreatedAt, endTime);
        }
        if (userType != null) {
            wrapper.eq(OperationLog::getUserType, userType);
        }
        wrapper.orderByDesc(OperationLog::getCreatedAt);
        return operationLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public OperationLog logDetail(Long id) {
        OperationLog log = operationLogMapper.selectById(id);
        if (log == null) {
            throw new BusinessException("日志不存在");
        }
        return log;
    }

    public void exportLogsCsv(jakarta.servlet.http.HttpServletResponse response, String action, String ip,
                               LocalDateTime startTime, LocalDateTime endTime, Integer userType) throws java.io.IOException {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (action != null && !action.trim().isEmpty()) {
            wrapper.like(OperationLog::getAction, action.trim());
        }
        if (ip != null && !ip.trim().isEmpty()) {
            wrapper.like(OperationLog::getIp, ip.trim());
        }
        if (startTime != null) {
            wrapper.ge(OperationLog::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(OperationLog::getCreatedAt, endTime);
        }
        if (userType != null) {
            wrapper.eq(OperationLog::getUserType, userType);
        }
        wrapper.orderByDesc(OperationLog::getCreatedAt);
        List<OperationLog> logs = operationLogMapper.selectList(wrapper);

        response.setContentType("text/csv;charset=UTF-8");
        String filename = "operation_logs_" + System.currentTimeMillis() + ".csv";
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        response.setCharacterEncoding("UTF-8");

        java.io.PrintWriter writer = response.getWriter();
        writer.write('\uFEFF');
        writer.println("ID,操作类型,目标,操作人类型,操作人ID,IP地址,操作时间,详情");
        for (OperationLog log : logs) {
            String userTypeStr = log.getUserType() != null
                    ? (log.getUserType() == 1 ? "管理员" : "用户")
                    : "";
            String detail = log.getDetail() != null ? log.getDetail().replace("\"", "\"\"").replace("\n", " ") : "";
            writer.printf("%d,\"%s\",\"%s\",%s,%s,\"%s\",%s,\"%s\"%n",
                    log.getId() != null ? log.getId() : 0,
                    log.getAction() != null ? log.getAction() : "",
                    log.getTarget() != null ? log.getTarget() : "",
                    userTypeStr,
                    log.getUserId() != null ? log.getUserId() : "",
                    log.getIp() != null ? log.getIp() : "",
                    log.getCreatedAt() != null ? log.getCreatedAt().toString() : "",
                    detail
            );
        }
        writer.flush();
    }

    public Page<FileDownloadLog> downloadLogList(int page, int size, Long userId, Integer userType) {
        LambdaQueryWrapper<FileDownloadLog> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(FileDownloadLog::getUserId, userId);
        }
        if (userType != null) {
            wrapper.eq(FileDownloadLog::getUserType, userType);
        }
        wrapper.orderByDesc(FileDownloadLog::getCreatedAt);
        return fileDownloadLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Page<AdminUser> adminList(int page, int size, String keyword) {
        LambdaQueryWrapper<AdminUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(AdminUser::getNickname, keyword)
                             .or().like(AdminUser::getUsername, keyword));
        }
        wrapper.orderByDesc(AdminUser::getCreatedAt);
        Page<AdminUser> pageResult = adminUserMapper.selectPage(new Page<>(page, size), wrapper);
        List<AdminUser> records = pageResult.getRecords();
        List<Long> roleIds = records.stream()
                .map(AdminUser::getRoleId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!roleIds.isEmpty()) {
            Map<Long, Role> roleMap = rbacService.listRoles().stream()
                    .filter(r -> roleIds.contains(r.getId()))
                    .collect(Collectors.toMap(Role::getId, r -> r));
            records.forEach(admin -> {
                if (admin.getRoleId() != null) {
                    admin.setRole(roleMap.get(admin.getRoleId()));
                }
            });
        }
        return pageResult;
    }

    public Map<String, Object> createAdmin(String username, String nickname, Long roleId, String initPassword) {
        requireSuperAdmin();
        if (username == null || username.trim().isEmpty()) {
            throw new BusinessException("用户名不能为空");
        }
        if (username.length() < 3 || username.length() > 50) {
            throw new BusinessException("用户名长度需在3-50个字符之间");
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new BusinessException("用户名只能包含字母、数字和下划线");
        }
        Long existCount = adminUserMapper.selectCount(
                new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, username.trim())
        );
        if (existCount != null && existCount > 0) {
            throw new BusinessException("用户名已存在");
        }
        if (roleId != null) {
            Role role = rbacService.getRoleWithPermissions(roleId);
            if (role == null || role.getStatus() != Constants.STATUS_ENABLED) {
                throw new BusinessException("所选角色不存在或已禁用");
            }
        }
        String passwordToUse;
        boolean isRandom = false;
        if (initPassword != null && !initPassword.trim().isEmpty()) {
            passwordToUse = initPassword.trim();
            if (passwordToUse.length() < 8) {
                throw new BusinessException("密码长度不能少于8位");
            }
        } else {
            passwordToUse = generateRandomPassword();
            isRandom = true;
        }
        AdminUser admin = new AdminUser();
        admin.setUsername(username.trim());
        admin.setNickname(nickname != null && !nickname.trim().isEmpty() ? nickname.trim() : username.trim());
        admin.setRoleId(roleId);
        admin.setPassword(PASSWORD_ENCODER.encode(passwordToUse));
        admin.setStatus(Constants.STATUS_ENABLED);
        adminUserMapper.insert(admin);
        log.info("创建管理员: username={}, roleId={}, randomPwd={}", username, roleId, isRandom);
        Map<String, Object> result = new HashMap<>();
        result.put("id", admin.getId());
        result.put("username", admin.getUsername());
        result.put("nickname", admin.getNickname());
        result.put("initPassword", passwordToUse);
        result.put("isRandomPassword", isRandom);
        return result;
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public void updateAdminRole(Long adminId, Long roleId) {
        requireSuperAdmin();
        AdminUser admin = adminUserMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException("管理员不存在");
        }
        if (Constants.ROLE_SUPER_ADMIN.equals(getRoleCode(admin.getRoleId()))) {
            long superAdminCount = adminUserMapper.selectCount(
                    new LambdaQueryWrapper<AdminUser>()
                            .eq(AdminUser::getRoleId, admin.getRoleId())
                            .eq(AdminUser::getStatus, Constants.STATUS_ENABLED)
            );
            if (superAdminCount <= 1 && !roleId.equals(admin.getRoleId())) {
                throw new BusinessException("不能修改最后一个超级管理员的角色");
            }
        }
        if (roleId != null) {
            Role role = rbacService.getRoleWithPermissions(roleId);
            if (role == null || role.getStatus() != Constants.STATUS_ENABLED) {
                throw new BusinessException("所选角色不存在或已禁用");
            }
        }
        admin.setRoleId(roleId);
        adminUserMapper.updateById(admin);
        log.info("修改管理员角色: adminId={}, roleId={}", adminId, roleId);
    }

    public Map<String, String> resetAdminPassword(Long adminId) {
        requireSuperAdmin();
        AdminUser admin = adminUserMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException("管理员不存在");
        }
        String newPassword = generateRandomPassword();
        admin.setPassword(PASSWORD_ENCODER.encode(newPassword));
        adminUserMapper.updateById(admin);
        log.info("重置管理员密码: adminId={}", adminId);
        Map<String, String> result = new HashMap<>();
        result.put("newPassword", newPassword);
        return result;
    }

    public void toggleAdminStatus(Long adminId, Integer status) {
        requireNotAuditor();
        if (status == null || (status != Constants.STATUS_ENABLED && status != Constants.STATUS_DISABLED)) {
            throw new BusinessException("状态参数无效");
        }
        AdminUser admin = adminUserMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException("管理员不存在");
        }
        if (Constants.ROLE_SUPER_ADMIN.equals(getRoleCode(admin.getRoleId()))
                && status == Constants.STATUS_DISABLED) {
            long superAdminCount = adminUserMapper.selectCount(
                    new LambdaQueryWrapper<AdminUser>()
                            .eq(AdminUser::getRoleId, admin.getRoleId())
                            .eq(AdminUser::getStatus, Constants.STATUS_ENABLED)
            );
            if (superAdminCount <= 1) {
                throw new BusinessException("不能禁用最后一个超级管理员");
            }
        }
        admin.setStatus(status);
        adminUserMapper.updateById(admin);
        log.info("修改管理员状态: adminId={}, status={}", adminId, status);
    }

    public void updateAdminNickname(Long adminId, String nickname) {
        requireNotAuditor();
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new BusinessException("昵称不能为空");
        }
        AdminUser admin = adminUserMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException("管理员不存在");
        }
        admin.setNickname(nickname.trim());
        adminUserMapper.updateById(admin);
        log.info("更新管理员昵称: adminId={}, nickname={}", adminId, nickname);
    }

    public void disableUser(Long userId) {
        requireNotAuditor();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(Constants.STATUS_DISABLED);
        userMapper.updateById(user);
        log.info("禁用用户: userId={}", userId);
    }

    public void deleteBook(Long bookId) {
        requireNotAuditor();
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException("书籍不存在");
        }
        book.setStatus(Constants.STATUS_DISABLED);
        bookMapper.updateById(book);
        log.info("删除书籍: bookId={}", bookId);
    }

    public Map<String, Object> batchDeleteBooks(List<Long> ids) {
        requireNotAuditor();
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的书籍");
        }
        int successCount = 0;
        List<Long> failedIds = new ArrayList<>();
        for (Long id : ids) {
            try {
                Book book = bookMapper.selectById(id);
                if (book != null && book.getStatus() != Constants.STATUS_DISABLED) {
                    book.setStatus(Constants.STATUS_DISABLED);
                    bookMapper.updateById(book);
                    successCount++;
                }
            } catch (Exception e) {
                failedIds.add(id);
                log.error("批量删除书籍失败: bookId={}", id, e);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failedIds", failedIds);
        result.put("failedCount", failedIds.size());
        log.info("批量删除书籍: total={}, success={}, failed={}", ids.size(), successCount, failedIds.size());
        return result;
    }

    public Map<String, Object> batchTakeDownBooks(List<Long> ids) {
        requireNotAuditor();
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要下架的书籍");
        }
        int successCount = 0;
        List<Long> failedIds = new ArrayList<>();
        for (Long id : ids) {
            try {
                Book book = bookMapper.selectById(id);
                if (book != null && book.getStatus() == Constants.STATUS_ENABLED) {
                    book.setStatus(Constants.STATUS_TAKEN_DOWN);
                    bookMapper.updateById(book);
                    successCount++;
                }
            } catch (Exception e) {
                failedIds.add(id);
                log.error("批量下架书籍失败: bookId={}", id, e);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failedIds", failedIds);
        result.put("failedCount", failedIds.size());
        log.info("批量下架书籍: total={}, success={}, failed={}", ids.size(), successCount, failedIds.size());
        return result;
    }

    public Map<String, Object> getBookUploader(Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException("书籍不存在");
        }
        User user = userMapper.selectById(book.getUserId());
        Map<String, Object> result = new HashMap<>();
        result.put("bookId", book.getId());
        result.put("bookTitle", book.getTitle());
        if (user != null) {
            result.put("userId", user.getId());
            result.put("nickname", user.getNickname());
            result.put("avatar", user.getAvatar());
            result.put("userStatus", user.getStatus());
        } else {
            result.put("userId", book.getUserId());
            result.put("nickname", "未知用户");
        }
        return result;
    }

    public Map<String, Object> previewBookPdf(Long bookId, int pages) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException("书籍不存在");
        }
        if (!Constants.FORMAT_PDF.equals(book.getBookFormat())) {
            throw new BusinessException("仅支持PDF格式预览");
        }
        int previewPages = Math.max(1, Math.min(pages, 10));
        int totalPages = book.getPageCount() != null ? book.getPageCount() : 0;
        int actualPages = Math.min(previewPages, totalPages > 0 ? totalPages : previewPages);

        List<Map<String, Object>> pageImages = new ArrayList<>();
        try {
            for (int i = 1; i <= actualPages; i++) {
                byte[] imgBytes = pdfReaderAdapter.getUnitImage(book.getFilePath(), i);
                if (imgBytes != null && imgBytes.length > 0) {
                    String base64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(imgBytes);
                    Map<String, Object> pageData = new HashMap<>();
                    pageData.put("page", i);
                    pageData.put("image", base64);
                    pageImages.add(pageData);
                }
            }
        } catch (Exception e) {
            log.error("PDF预览失败: bookId={}", bookId, e);
            throw new BusinessException("PDF预览失败: " + e.getMessage());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("bookId", book.getId());
        result.put("title", book.getTitle());
        result.put("totalPages", totalPages);
        result.put("previewPages", pageImages.size());
        result.put("pages", pageImages);
        return result;
    }

    public void deleteAdmin(Long adminId) {
        requireSuperAdmin();
        AdminUser admin = adminUserMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException("管理员不存在");
        }
        if (Constants.ROLE_SUPER_ADMIN.equals(getRoleCode(admin.getRoleId()))) {
            long superAdminCount = adminUserMapper.selectCount(
                    new LambdaQueryWrapper<AdminUser>()
                            .eq(AdminUser::getRoleId, admin.getRoleId())
                            .eq(AdminUser::getStatus, Constants.STATUS_ENABLED)
            );
            if (superAdminCount <= 1) {
                throw new BusinessException("不能删除最后一个超级管理员");
            }
        }
        admin.setStatus(Constants.STATUS_DISABLED);
        adminUserMapper.updateById(admin);
        log.info("删除管理员: adminId={}", adminId);
    }

    private String getRoleCode(Long roleId) {
        if (roleId == null) return null;
        Role role = rbacService.getRoleWithPermissions(roleId);
        return role != null ? role.getCode() : null;
    }

    private String getCurrentRoleCode() {
        Long roleId = TenantContext.getRoleId();
        return getRoleCode(roleId);
    }

    private void requireNotAuditor() {
        if (Constants.ROLE_AUDITOR.equals(getCurrentRoleCode())) {
            throw new BusinessException(403, "审计角色无权执行此操作");
        }
    }

    private void requireSuperAdmin() {
        if (!Constants.ROLE_SUPER_ADMIN.equals(getCurrentRoleCode())) {
            throw new BusinessException(403, "仅超级管理员可执行此操作");
        }
    }

    public Map<String, Object> readingStats(Integer days) {
        if (days == null || days <= 0) {
            days = 7;
        }
        LocalDateTime start = LocalDate.now().minusDays(days).atStartOfDay();
        LocalDateTime end = LocalDateTime.now();

        Map<String, Object> data = new HashMap<>();
        Long activeUsers = readingRecordMapper.countActiveUsers(start, end);
        Long avgDuration = readingRecordMapper.avgDailyDurationPerUser(start, end);
        java.util.List<Map<String, Object>> dailyStats = readingRecordMapper.dailyReadingStats(start, end);

        data.put("activeUsers", activeUsers != null ? activeUsers : 0L);
        data.put("avgDurationPerUser", avgDuration != null ? avgDuration : 0L);
        data.put("dailyStats", dailyStats);
        data.put("days", days);

        long totalSeconds = 0;
        long totalBookReads = 0;
        if (dailyStats != null) {
            for (Map<String, Object> stat : dailyStats) {
                if (stat.get("totalDuration") != null) {
                    totalSeconds += ((Number) stat.get("totalDuration")).longValue();
                }
                if (stat.get("bookCount") != null) {
                    totalBookReads += ((Number) stat.get("bookCount")).longValue();
                }
            }
        }
        data.put("totalDuration", totalSeconds);
        data.put("avgDurationPerDay", days > 0 ? totalSeconds / days : 0L);
        data.put("totalBookReads", totalBookReads);
        data.put("avgBooksPerDay", days > 0 ? (double) totalBookReads / days : 0);

        log.info("管理端阅读统计: days={}, activeUsers={}, avgDuration={}s", days, activeUsers, avgDuration);
        return data;
    }
}
