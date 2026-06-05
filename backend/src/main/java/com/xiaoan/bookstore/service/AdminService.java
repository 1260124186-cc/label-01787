package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.common.Constants;
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
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    private final AdminUserMapper adminUserMapper;
    private final UserMapper userMapper;
    private final BookMapper bookMapper;
    private final ReadingRecordMapper readingRecordMapper;
    private final OperationLogMapper operationLogMapper;
    private final JwtUtil jwtUtil;
    private final RbacService rbacService;

    /**
     * 解码前端 Base64 编码的密码
     */
    private String decodePassword(String encoded) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encoded);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 如果解码失败，则视为明文密码
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
        // 解码前端传来的 Base64 密码后校验
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
        // 全平台总阅读时长（秒）
        Long totalDuration = readingRecordMapper.sumDuration(null,
                java.time.LocalDate.of(2020, 1, 1).atStartOfDay(),
                java.time.LocalDateTime.now());
        data.put("totalReadingSeconds", totalDuration != null ? totalDuration : 0L);
        data.put("logCount", operationLogMapper.selectCount(null));
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

    public Page<Book> bookList(int page, int size, String keyword) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Book::getTitle, keyword);
        }
        wrapper.eq(Book::getStatus, Constants.STATUS_ENABLED);
        wrapper.orderByDesc(Book::getCreatedAt);
        return bookMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Page<OperationLog> logList(int page, int size) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(OperationLog::getCreatedAt);
        return operationLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Page<AdminUser> adminList(int page, int size, String keyword) {
        LambdaQueryWrapper<AdminUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(AdminUser::getId, AdminUser::getUsername, AdminUser::getNickname,
                       AdminUser::getRoleId, AdminUser::getStatus, AdminUser::getCreatedAt);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(AdminUser::getNickname, keyword)
                             .or().like(AdminUser::getUsername, keyword));
        }
        wrapper.orderByDesc(AdminUser::getCreatedAt);
        return adminUserMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public void updateAdminNickname(Long adminId, String nickname) {
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
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(Constants.STATUS_DISABLED);
        userMapper.updateById(user);
        log.info("禁用用户: userId={}", userId);
    }

    public void deleteBook(Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException("书籍不存在");
        }
        book.setStatus(Constants.STATUS_DISABLED);
        bookMapper.updateById(book);
        log.info("删除书籍: bookId={}", bookId);
    }

    public void deleteAdmin(Long adminId) {
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
}
