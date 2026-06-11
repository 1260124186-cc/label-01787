package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.common.TenantValidator;
import com.xiaoan.bookstore.dto.*;
import com.xiaoan.bookstore.entity.*;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FamilyService {

    private static final Logger log = LoggerFactory.getLogger(FamilyService.class);
    private static final String INVITE_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int INVITE_CODE_LENGTH = 6;

    private final FamilyMapper familyMapper;
    private final FamilyMemberMapper familyMemberMapper;
    private final FamilySharedBookMapper familySharedBookMapper;
    private final UserMapper userMapper;
    private final BookMapper bookMapper;
    private final ReadingRecordMapper readingRecordMapper;
    private final AnnotationMapper annotationMapper;
    private final MembershipService membershipService;

    private String generateInviteCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            sb.append(INVITE_CODE_CHARS.charAt(random.nextInt(INVITE_CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            code = generateInviteCode();
        } while (familyMapper.selectCount(new LambdaQueryWrapper<Family>()
                .eq(Family::getInviteCode, code)) > 0);
        return code;
    }

    @Transactional
    public Family createFamily(Long userId, FamilyCreateDTO dto) {
        Long existingCount = familyMemberMapper.selectCount(new LambdaQueryWrapper<FamilyMember>()
                .eq(FamilyMember::getUserId, userId));
        if (existingCount > 0) {
            throw new BusinessException("您已加入家庭，无法重复创建");
        }

        Family family = new Family();
        family.setName(dto.getName());
        family.setInviteCode(generateUniqueInviteCode());
        family.setOwnerId(userId);
        family.setMemberCount(1);
        family.setMaxMembers(Constants.FAMILY_MAX_MEMBERS_DEFAULT);
        family.setSharedStorage(0L);
        family.setStatus(Constants.STATUS_ENABLED);
        familyMapper.insert(family);

        FamilyMember member = new FamilyMember();
        member.setFamilyId(family.getId());
        member.setUserId(userId);
        member.setRole(Constants.FAMILY_ROLE_PARENT);
        member.setNickname("");
        member.setJoinedAt(LocalDateTime.now());
        familyMemberMapper.insert(member);

        QuotaVO quota = membershipService.getQuota(userId);
        family.setSharedStorage(quota.getMaxStorage());
        familyMapper.updateById(family);

        log.info("创建家庭: userId={}, familyId={}, familyName={}", userId, family.getId(), family.getName());
        return family;
    }

    @Transactional
    public FamilyMember joinFamily(Long userId, FamilyJoinDTO dto) {
        Family family = familyMapper.selectOne(new LambdaQueryWrapper<Family>()
                .eq(Family::getInviteCode, dto.getInviteCode())
                .eq(Family::getStatus, Constants.STATUS_ENABLED));
        if (family == null) {
            throw new BusinessException("邀请码无效或家庭已解散");
        }

        Long existingCount = familyMemberMapper.selectCount(new LambdaQueryWrapper<FamilyMember>()
                .eq(FamilyMember::getUserId, userId));
        if (existingCount > 0) {
            throw new BusinessException("您已加入家庭，无法重复加入");
        }

        if (family.getMemberCount() >= family.getMaxMembers()) {
            throw new BusinessException("家庭成员已达上限(" + family.getMaxMembers() + "人)");
        }

        FamilyMember member = new FamilyMember();
        member.setFamilyId(family.getId());
        member.setUserId(userId);
        member.setRole(Constants.FAMILY_ROLE_CHILD);
        member.setNickname("");
        member.setJoinedAt(LocalDateTime.now());
        familyMemberMapper.insert(member);

        family.setMemberCount(family.getMemberCount() + 1);
        familyMapper.updateById(family);

        log.info("加入家庭: userId={}, familyId={}", userId, family.getId());
        return member;
    }

    @Transactional
    public void leaveFamily(Long userId, Long familyId) {
        FamilyMember member = familyMemberMapper.selectOne(new LambdaQueryWrapper<FamilyMember>()
                .eq(FamilyMember::getFamilyId, familyId)
                .eq(FamilyMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException("您不是该家庭成员");
        }

        Family family = familyMapper.selectById(familyId);
        if (family == null) {
            throw new BusinessException("家庭不存在");
        }

        if (member.getRole() == Constants.FAMILY_ROLE_PARENT) {
            Long childCount = familyMemberMapper.selectCount(new LambdaQueryWrapper<FamilyMember>()
                    .eq(FamilyMember::getFamilyId, familyId)
                    .eq(FamilyMember::getRole, Constants.FAMILY_ROLE_CHILD));
            if (childCount > 0) {
                throw new BusinessException("家长不能直接退出家庭，请先移除所有子女或解散家庭");
            }
            family.setStatus(Constants.STATUS_DISABLED);
            familyMapper.updateById(family);
            familyMemberMapper.deleteById(member.getId());
            log.info("家长解散家庭: userId={}, familyId={}", userId, familyId);
            return;
        }

        familySharedBookMapper.delete(new LambdaQueryWrapper<FamilySharedBook>()
                .eq(FamilySharedBook::getFamilyId, familyId)
                .eq(FamilySharedBook::getSharedBy, userId));

        familyMemberMapper.deleteById(member.getId());
        family.setMemberCount(Math.max(0, family.getMemberCount() - 1));
        familyMapper.updateById(family);

        log.info("退出家庭: userId={}, familyId={}", userId, familyId);
    }

    @Transactional
    public void dissolveFamily(Long userId, Long familyId) {
        Family family = familyMapper.selectById(familyId);
        if (family == null) {
            throw new BusinessException("家庭不存在");
        }
        if (!family.getOwnerId().equals(userId)) {
            throw new BusinessException("只有家长可以解散家庭");
        }

        familySharedBookMapper.delete(new LambdaQueryWrapper<FamilySharedBook>()
                .eq(FamilySharedBook::getFamilyId, familyId));

        familyMemberMapper.delete(new LambdaQueryWrapper<FamilyMember>()
                .eq(FamilyMember::getFamilyId, familyId));

        family.setStatus(Constants.STATUS_DISABLED);
        family.setMemberCount(0);
        familyMapper.updateById(family);

        log.info("解散家庭: userId={}, familyId={}", userId, familyId);
    }

    @Transactional
    public void removeMember(Long userId, Long familyId, Long targetUserId) {
        Family family = familyMapper.selectById(familyId);
        if (family == null) {
            throw new BusinessException("家庭不存在");
        }
        if (!family.getOwnerId().equals(userId)) {
            throw new BusinessException("只有家长可以移除成员");
        }
        if (userId.equals(targetUserId)) {
            throw new BusinessException("不能移除自己，请使用解散家庭功能");
        }

        FamilyMember targetMember = familyMemberMapper.selectOne(new LambdaQueryWrapper<FamilyMember>()
                .eq(FamilyMember::getFamilyId, familyId)
                .eq(FamilyMember::getUserId, targetUserId));
        if (targetMember == null) {
            throw new BusinessException("该用户不是家庭成员");
        }

        familySharedBookMapper.delete(new LambdaQueryWrapper<FamilySharedBook>()
                .eq(FamilySharedBook::getFamilyId, familyId)
                .eq(FamilySharedBook::getSharedBy, targetUserId));

        familyMemberMapper.deleteById(targetMember.getId());
        family.setMemberCount(Math.max(0, family.getMemberCount() - 1));
        familyMapper.updateById(family);

        log.info("移除家庭成员: operatorId={}, targetUserId={}, familyId={}", userId, targetUserId, familyId);
    }

    public List<Family> myFamilies(Long userId) {
        return familyMapper.selectFamiliesByUserId(userId);
    }

    public FamilyVO getFamilyDetail(Long userId, Long familyId) {
        Family family = familyMapper.selectById(familyId);
        if (family == null) {
            throw new BusinessException("家庭不存在");
        }
        if (family.getStatus() == Constants.STATUS_DISABLED) {
            throw new BusinessException("家庭已解散");
        }

        FamilyMember myMember = familyMemberMapper.selectOne(new LambdaQueryWrapper<FamilyMember>()
                .eq(FamilyMember::getFamilyId, familyId)
                .eq(FamilyMember::getUserId, userId));
        if (myMember == null) {
            throw new BusinessException("您不是该家庭成员");
        }

        User owner = userMapper.selectById(family.getOwnerId());

        FamilyVO vo = new FamilyVO();
        vo.setId(family.getId());
        vo.setName(family.getName());
        vo.setInviteCode(family.getInviteCode());
        vo.setOwnerId(family.getOwnerId());
        vo.setOwnerNickname(owner != null ? owner.getNickname() : "");
        vo.setOwnerAvatar(owner != null ? owner.getAvatar() : "");
        vo.setMemberCount(family.getMemberCount());
        vo.setMaxMembers(family.getMaxMembers());
        vo.setSharedStorage(family.getSharedStorage());
        vo.setStatus(family.getStatus());
        vo.setMyRole(myMember.getRole());
        vo.setCreatedAt(family.getCreatedAt());

        List<Map<String, Object>> memberMaps = familyMemberMapper.selectMembersWithUserInfo(familyId);
        List<FamilyMemberVO> memberVOS = memberMaps.stream().map(m -> {
            FamilyMemberVO mvo = new FamilyMemberVO();
            mvo.setUserId(((Number) m.get("user_id")).longValue());
            mvo.setNickname((String) m.get("nickname"));
            mvo.setAvatar((String) m.get("avatar"));
            mvo.setRole(((Number) m.get("role")).intValue());
            mvo.setFamilyNickname((String) m.get("family_nickname"));
            mvo.setJoinedAt((LocalDateTime) m.get("joined_at"));
            return mvo;
        }).collect(Collectors.toList());
        vo.setMembers(memberVOS);

        return vo;
    }

    @Transactional
    public FamilySharedBook shareBook(Long userId, Long familyId, FamilySharedBookDTO dto) {
        FamilyMember member = familyMemberMapper.selectOne(new LambdaQueryWrapper<FamilyMember>()
                .eq(FamilyMember::getFamilyId, familyId)
                .eq(FamilyMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException("您不是该家庭成员");
        }

        Book book = bookMapper.selectById(dto.getBookId());
        if (book == null) {
            throw new BusinessException("书籍不存在");
        }
        TenantValidator.validateCrossTenant(book.getUserId(), userId);

        Long existing = familySharedBookMapper.selectCount(new LambdaQueryWrapper<FamilySharedBook>()
                .eq(FamilySharedBook::getFamilyId, familyId)
                .eq(FamilySharedBook::getBookId, dto.getBookId()));
        if (existing > 0) {
            throw new BusinessException("该书籍已在家庭书架中");
        }

        FamilySharedBook sharedBook = new FamilySharedBook();
        sharedBook.setFamilyId(familyId);
        sharedBook.setBookId(dto.getBookId());
        sharedBook.setSharedBy(userId);
        sharedBook.setSharedAt(LocalDateTime.now());
        familySharedBookMapper.insert(sharedBook);

        log.info("共享书籍到家庭书架: userId={}, familyId={}, bookId={}", userId, familyId, dto.getBookId());
        return sharedBook;
    }

    @Transactional
    public void unshareBook(Long userId, Long familyId, Long bookId) {
        FamilyMember member = familyMemberMapper.selectOne(new LambdaQueryWrapper<FamilyMember>()
                .eq(FamilyMember::getFamilyId, familyId)
                .eq(FamilyMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException("您不是该家庭成员");
        }

        FamilySharedBook sharedBook = familySharedBookMapper.selectOne(new LambdaQueryWrapper<FamilySharedBook>()
                .eq(FamilySharedBook::getFamilyId, familyId)
                .eq(FamilySharedBook::getBookId, bookId));
        if (sharedBook == null) {
            throw new BusinessException("该书籍不在家庭书架中");
        }

        Family family = familyMapper.selectById(familyId);
        if (!sharedBook.getSharedBy().equals(userId) && !family.getOwnerId().equals(userId)) {
            throw new BusinessException("只有共享者或家长可以取消共享");
        }

        familySharedBookMapper.deleteById(sharedBook.getId());
        log.info("取消家庭书架共享: userId={}, familyId={}, bookId={}", userId, familyId, bookId);
    }

    public List<FamilySharedBookVO> getSharedBooks(Long userId, Long familyId) {
        FamilyMember member = familyMemberMapper.selectOne(new LambdaQueryWrapper<FamilyMember>()
                .eq(FamilyMember::getFamilyId, familyId)
                .eq(FamilyMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException("您不是该家庭成员");
        }

        List<Map<String, Object>> bookMaps = familySharedBookMapper.selectSharedBooksWithInfo(familyId);
        return bookMaps.stream().map(m -> {
            FamilySharedBookVO vo = new FamilySharedBookVO();
            vo.setId(((Number) m.get("id")).longValue());
            vo.setBookId(((Number) m.get("book_id")).longValue());
            vo.setBookTitle((String) m.get("book_title"));
            vo.setBookAuthor((String) m.get("book_author"));
            vo.setBookFormat((String) m.get("book_format"));
            vo.setCoverThumbnail((String) m.get("cover_thumbnail"));
            vo.setSharedBy(((Number) m.get("shared_by")).longValue());
            vo.setSharedByNickname((String) m.get("shared_by_nickname"));
            vo.setSharedAt((LocalDateTime) m.get("shared_at"));
            return vo;
        }).collect(Collectors.toList());
    }

    public List<FamilyReadingReportVO> getChildReadingReports(Long userId, Long familyId, String period) {
        Family family = familyMapper.selectById(familyId);
        if (family == null) {
            throw new BusinessException("家庭不存在");
        }
        if (!family.getOwnerId().equals(userId)) {
            throw new BusinessException("只有家长可以查看子女阅读报告");
        }

        LocalDateTime start;
        LocalDateTime end = LocalDateTime.now();

        switch (period != null ? period : "week") {
            case "week":
                LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
                start = weekStart.atStartOfDay();
                break;
            case "month":
                LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
                start = monthStart.atStartOfDay();
                break;
            case "year":
                LocalDate yearStart = LocalDate.now().withDayOfYear(1);
                start = yearStart.atStartOfDay();
                break;
            default:
                throw new BusinessException("无效的统计周期，支持 week/month/year");
        }

        List<FamilyMember> children = familyMemberMapper.selectList(new LambdaQueryWrapper<FamilyMember>()
                .eq(FamilyMember::getFamilyId, familyId)
                .eq(FamilyMember::getRole, Constants.FAMILY_ROLE_CHILD));

        List<FamilyReadingReportVO> reports = new ArrayList<>();
        for (FamilyMember child : children) {
            User childUser = userMapper.selectById(child.getUserId());
            if (childUser == null) continue;

            FamilyReadingReportVO report = new FamilyReadingReportVO();
            report.setUserId(childUser.getId());
            report.setNickname(childUser.getNickname());
            report.setAvatar(childUser.getAvatar());

            Long totalDuration = readingRecordMapper.sumDuration(child.getUserId(), start, end);
            report.setTotalDuration(totalDuration != null ? totalDuration : 0L);
            report.setTotalDurationText(formatDuration(report.getTotalDuration()));

            Integer bookCount = readingRecordMapper.countBooks(child.getUserId(), start, end);
            report.setBookCount(bookCount != null ? bookCount : 0);

            Integer readingDays = readingRecordMapper.countReadingDays(child.getUserId(), start, end);
            report.setReadingDays(readingDays != null ? readingDays : 0);

            Long annotationCount = annotationMapper.selectCount(
                    new LambdaQueryWrapper<Annotation>()
                            .eq(Annotation::getUserId, child.getUserId())
                            .ge(Annotation::getCreatedAt, start)
                            .lt(Annotation::getCreatedAt, end)
            );
            report.setAnnotationCount(annotationCount != null ? annotationCount.intValue() : 0);

            List<Map<String, Object>> bookRank = readingRecordMapper.bookRank(child.getUserId(), start, end);
            if (bookRank != null && bookRank.size() > 5) {
                bookRank = bookRank.subList(0, 5);
            }
            report.setBookRank(bookRank);

            List<Map<String, Object>> dailyData = readingRecordMapper.dailyDuration(child.getUserId(), start, end);
            report.setDailyData(dailyData);

            reports.add(report);
        }

        return reports;
    }

    public QuotaVO getFamilySharedStorage(Long userId, Long familyId) {
        FamilyMember member = familyMemberMapper.selectOne(new LambdaQueryWrapper<FamilyMember>()
                .eq(FamilyMember::getFamilyId, familyId)
                .eq(FamilyMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException("您不是该家庭成员");
        }

        Family family = familyMapper.selectById(familyId);
        if (family == null) {
            throw new BusinessException("家庭不存在");
        }

        List<FamilyMember> members = familyMemberMapper.selectList(new LambdaQueryWrapper<FamilyMember>()
                .eq(FamilyMember::getFamilyId, familyId));

        long totalUsed = 0L;
        for (FamilyMember fm : members) {
            List<Book> books = bookMapper.selectList(
                    new LambdaQueryWrapper<Book>()
                            .eq(Book::getUserId, fm.getUserId())
                            .eq(Book::getStatus, Constants.STATUS_ENABLED));
            totalUsed += books.stream()
                    .mapToLong(b -> b.getFileSize() != null ? b.getFileSize() : 0L)
                    .sum();
        }

        QuotaVO vo = new QuotaVO();
        vo.setMaxStorage(family.getSharedStorage() != null ? family.getSharedStorage() : 0L);
        vo.setUsedStorage(totalUsed);
        return vo;
    }

    public IPage<Family> adminFamilyList(int page, int size, String keyword) {
        Page<Family> pageParam = new Page<>(page, size);
        return familyMapper.selectAdminPage(pageParam, keyword);
    }

    public Map<String, Object> adminDashboard() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalFamilies", familyMapper.countActiveFamilies());
        return result;
    }

    private String formatDuration(long seconds) {
        if (seconds <= 0) return "0分钟";
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours > 0) {
            return hours + "小时" + minutes + "分钟";
        } else {
            return minutes + "分钟";
        }
    }
}
