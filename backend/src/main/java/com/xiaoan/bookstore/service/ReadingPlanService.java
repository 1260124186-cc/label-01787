package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.common.TenantValidator;
import com.xiaoan.bookstore.dto.*;
import com.xiaoan.bookstore.entity.Book;
import com.xiaoan.bookstore.entity.ReadingPlan;
import com.xiaoan.bookstore.entity.ReadingPlanBadge;
import com.xiaoan.bookstore.entity.ReadingPlanCheckin;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.BookMapper;
import com.xiaoan.bookstore.mapper.ReadingPlanBadgeMapper;
import com.xiaoan.bookstore.mapper.ReadingPlanCheckinMapper;
import com.xiaoan.bookstore.mapper.ReadingPlanMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReadingPlanService {

    private static final Logger log = LoggerFactory.getLogger(ReadingPlanService.class);

    private static final Map<Integer, String> STREAK_BADGES = new LinkedHashMap<>();
    static {
        STREAK_BADGES.put(3, "streak_3");
        STREAK_BADGES.put(7, "streak_7");
        STREAK_BADGES.put(14, "streak_14");
        STREAK_BADGES.put(30, "streak_30");
        STREAK_BADGES.put(100, "streak_100");
    }

    private static final Map<Integer, String> STREAK_BADGE_NAMES = new LinkedHashMap<>();
    static {
        STREAK_BADGE_NAMES.put(3, "三日笃学");
        STREAK_BADGE_NAMES.put(7, "七日坚持");
        STREAK_BADGE_NAMES.put(14, "两周不懈");
        STREAK_BADGE_NAMES.put(30, "月度书虫");
        STREAK_BADGE_NAMES.put(100, "百日书圣");
    }

    private final ReadingPlanMapper readingPlanMapper;
    private final ReadingPlanCheckinMapper checkinMapper;
    private final ReadingPlanBadgeMapper badgeMapper;
    private final BookMapper bookMapper;

    @Transactional
    public ReadingPlanVO createPlan(Long userId, ReadingPlanCreateDTO dto) {
        Book book = bookMapper.selectById(dto.getBookId());
        if (book == null) {
            throw new BusinessException("书籍不存在");
        }
        TenantValidator.validateCrossTenant(book.getUserId(), userId);

        Long activeCount = readingPlanMapper.selectCount(
            new LambdaQueryWrapper<ReadingPlan>()
                .eq(ReadingPlan::getUserId, userId)
                .eq(ReadingPlan::getStatus, 1)
        );
        if (activeCount >= 10) {
            throw new BusinessException("最多同时进行10个阅读计划");
        }

        Long sameBookActive = readingPlanMapper.selectCount(
            new LambdaQueryWrapper<ReadingPlan>()
                .eq(ReadingPlan::getUserId, userId)
                .eq(ReadingPlan::getBookId, dto.getBookId())
                .eq(ReadingPlan::getStatus, 1)
        );
        if (sameBookActive > 0) {
            throw new BusinessException("该书籍已有进行中的阅读计划");
        }

        int totalPages = book.getPageCount() != null && book.getPageCount() > 0 ? book.getPageCount() : 0;
        int dailyMin = dto.getDailyMinDuration() != null ? dto.getDailyMinDuration() : 600;

        ReadingPlan plan = new ReadingPlan();
        plan.setUserId(userId);
        plan.setBookId(dto.getBookId());
        plan.setTargetDays(dto.getTargetDays());
        plan.setDailyMinDuration(dailyMin);
        plan.setReminderTime(dto.getReminderTime() != null ? dto.getReminderTime() : "");
        plan.setReadPages(0);
        plan.setTotalPages(totalPages);
        plan.setStreakDays(0);
        plan.setMaxStreakDays(0);
        plan.setStatus(1);
        plan.setStartDate(LocalDate.now());
        plan.setEndDate(LocalDate.now().plusDays(dto.getTargetDays()));
        readingPlanMapper.insert(plan);

        log.info("创建阅读计划: userId={}, bookId={}, planId={}", userId, dto.getBookId(), plan.getId());
        return toVO(plan, book);
    }

    public List<ReadingPlanVO> listPlans(Long userId, Integer status) {
        LambdaQueryWrapper<ReadingPlan> wrapper = new LambdaQueryWrapper<ReadingPlan>()
            .eq(ReadingPlan::getUserId, userId)
            .orderByDesc(ReadingPlan::getStatus)
            .orderByDesc(ReadingPlan::getCreatedAt);
        if (status != null) {
            wrapper.eq(ReadingPlan::getStatus, status);
        }
        List<ReadingPlan> plans = readingPlanMapper.selectList(wrapper);
        return plans.stream().map(plan -> {
            Book book = bookMapper.selectById(plan.getBookId());
            return toVO(plan, book);
        }).collect(Collectors.toList());
    }

    public ReadingPlanProgressVO getProgress(Long userId, Long planId) {
        ReadingPlan plan = readingPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException("阅读计划不存在");
        }
        TenantValidator.validateCrossTenant(plan.getUserId(), userId);

        return buildProgressVO(plan);
    }

    @Transactional
    public ReadingPlanCheckin checkin(Long userId, ReadingPlanCheckinDTO dto) {
        ReadingPlan plan = readingPlanMapper.selectById(dto.getPlanId());
        if (plan == null) {
            throw new BusinessException("阅读计划不存在");
        }
        TenantValidator.validateCrossTenant(plan.getUserId(), userId);
        if (plan.getStatus() != 1) {
            throw new BusinessException("阅读计划已结束，无法打卡");
        }

        LocalDate today = LocalDate.now();
        ReadingPlanCheckin existing = checkinMapper.findByPlanAndDate(plan.getId(), today);
        if (existing != null) {
            existing.setDuration(existing.getDuration() + (dto.getDuration() != null ? dto.getDuration() : 0));
            existing.setPagesRead(existing.getPagesRead() + (dto.getPagesRead() != null ? dto.getPagesRead() : 0));
            checkinMapper.updateById(existing);
            log.info("更新打卡记录: planId={}, date={}", plan.getId(), today);
        } else {
            existing = new ReadingPlanCheckin();
            existing.setPlanId(plan.getId());
            existing.setUserId(userId);
            existing.setCheckinDate(today);
            existing.setDuration(dto.getDuration() != null ? dto.getDuration() : 0);
            existing.setPagesRead(dto.getPagesRead() != null ? dto.getPagesRead() : 0);
            checkinMapper.insert(existing);
            log.info("新增打卡记录: planId={}, date={}", plan.getId(), today);
        }

        updatePlanProgress(plan);
        checkAndAwardBadges(userId, plan);
        checkPlanCompletion(plan);

        return existing;
    }

    public List<LocalDate> getCheckinCalendar(Long userId, Long planId) {
        ReadingPlan plan = readingPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException("阅读计划不存在");
        }
        TenantValidator.validateCrossTenant(plan.getUserId(), userId);
        return checkinMapper.listCheckinDates(planId);
    }

    public List<ReadingPlanBadge> listBadges(Long userId) {
        return badgeMapper.listByUserId(userId);
    }

    @Transactional
    public void abandonPlan(Long userId, Long planId) {
        ReadingPlan plan = readingPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException("阅读计划不存在");
        }
        TenantValidator.validateCrossTenant(plan.getUserId(), userId);
        if (plan.getStatus() != 1) {
            throw new BusinessException("只有进行中的计划可以放弃");
        }
        plan.setStatus(0);
        readingPlanMapper.updateById(plan);
        log.info("放弃阅读计划: userId={}, planId={}", userId, planId);
    }

    public Map<String, Object> adminStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activePlans", readingPlanMapper.countActivePlans());
        stats.put("completedPlans", readingPlanMapper.countCompletedPlans());

        Long totalPlans = readingPlanMapper.selectCount(new LambdaQueryWrapper<>());
        stats.put("totalPlans", totalPlans);

        Long completedPlans = readingPlanMapper.countCompletedPlans();
        double completionRate = totalPlans > 0 ? (double) completedPlans / totalPlans * 100 : 0;
        stats.put("completionRate", Math.round(completionRate * 100.0) / 100.0);

        stats.put("activePlanUsers", readingPlanMapper.countActivePlanUsers());
        stats.put("totalPlanUsers", readingPlanMapper.countTotalPlanUsers());

        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDateTime monthStart = startOfMonth.atStartOfDay();
        LocalDateTime monthEnd = today.plusDays(1).atStartOfDay();

        stats.put("monthCreated", readingPlanMapper.countCreatedBetween(monthStart, monthEnd));
        stats.put("monthCompleted", readingPlanMapper.countCompletedBetween(monthStart, monthEnd));

        Long monthCheckinUsers = checkinMapper.countCheckinUsersBetween(startOfMonth, today);
        stats.put("monthCheckinUsers", monthCheckinUsers);

        stats.put("dailyCreation", readingPlanMapper.dailyCreationStats(monthStart, monthEnd));
        stats.put("dailyCompletion", readingPlanMapper.dailyCompletionStats(monthStart, monthEnd));
        stats.put("dailyCheckin", checkinMapper.dailyCheckinStats(startOfMonth, today));

        return stats;
    }

    private void updatePlanProgress(ReadingPlan plan) {
        Integer totalPagesRead = checkinMapper.sumPagesRead(plan.getId());
        if (totalPagesRead == null) totalPagesRead = 0;
        plan.setReadPages(totalPagesRead);

        int newStreak = calculateStreak(plan);
        plan.setStreakDays(newStreak);
        if (newStreak > plan.getMaxStreakDays()) {
            plan.setMaxStreakDays(newStreak);
        }

        readingPlanMapper.updateProgress(plan.getId(), totalPagesRead, newStreak);
    }

    private int calculateStreak(ReadingPlan plan) {
        List<LocalDate> checkinDates = checkinMapper.listCheckinDates(plan.getId());
        if (checkinDates.isEmpty()) return 0;

        Collections.sort(checkinDates);
        LocalDate today = LocalDate.now();
        int streak = 0;
        LocalDate checkDate = today;

        for (int i = checkinDates.size() - 1; i >= 0; i--) {
            if (checkinDates.get(i).equals(checkDate)) {
                streak++;
                checkDate = checkDate.minusDays(1);
            } else if (checkinDates.get(i).isBefore(checkDate)) {
                break;
            }
        }
        return streak;
    }

    private void checkAndAwardBadges(Long userId, ReadingPlan plan) {
        for (Map.Entry<Integer, String> entry : STREAK_BADGES.entrySet()) {
            int threshold = entry.getKey();
            String badgeType = entry.getValue();
            if (plan.getStreakDays() >= threshold) {
                int existing = badgeMapper.countByUserAndType(userId, badgeType);
                if (existing == 0) {
                    ReadingPlanBadge badge = new ReadingPlanBadge();
                    badge.setUserId(userId);
                    badge.setPlanId(plan.getId());
                    badge.setBadgeType(badgeType);
                    badge.setBadgeName(STREAK_BADGE_NAMES.get(threshold));
                    badge.setBadgeIcon(badgeType);
                    badgeMapper.insert(badge);
                    log.info("颁发徽章: userId={}, badge={}", userId, badgeType);
                }
            }
        }
    }

    private void checkPlanCompletion(ReadingPlan plan) {
        if (plan.getTotalPages() > 0 && plan.getReadPages() >= plan.getTotalPages()) {
            plan.setStatus(2);
            plan.setCompletedAt(LocalDateTime.now());
            readingPlanMapper.updateById(plan);
            log.info("阅读计划完成: planId={}", plan.getId());

            int existing = badgeMapper.countByUserAndType(plan.getUserId(), "plan_complete");
            if (existing == 0) {
                ReadingPlanBadge badge = new ReadingPlanBadge();
                badge.setUserId(plan.getUserId());
                badge.setPlanId(plan.getId());
                badge.setBadgeType("plan_complete");
                badge.setBadgeName("计划达成");
                badge.setBadgeIcon("plan_complete");
                badgeMapper.insert(badge);
            }
        }
    }

    private ReadingPlanVO toVO(ReadingPlan plan, Book book) {
        ReadingPlanVO vo = new ReadingPlanVO();
        vo.setId(plan.getId());
        vo.setBookId(plan.getBookId());
        vo.setBookTitle(book != null ? book.getTitle() : "");
        vo.setBookAuthor(book != null ? book.getAuthor() : "");
        vo.setBookFormat(book != null ? book.getBookFormat() : "");
        vo.setTargetDays(plan.getTargetDays());
        vo.setDailyMinDuration(plan.getDailyMinDuration());
        vo.setReminderTime(plan.getReminderTime());
        vo.setReadPages(plan.getReadPages());
        vo.setTotalPages(plan.getTotalPages());
        vo.setStreakDays(plan.getStreakDays());
        vo.setMaxStreakDays(plan.getMaxStreakDays());
        vo.setStatus(plan.getStatus());
        vo.setStartDate(plan.getStartDate() != null ? plan.getStartDate().toString() : "");
        vo.setEndDate(plan.getEndDate() != null ? plan.getEndDate().toString() : "");

        if (plan.getTotalPages() > 0) {
            vo.setProgress(Math.round((double) plan.getReadPages() / plan.getTotalPages() * 1000.0) / 10.0);
        } else {
            vo.setProgress(0.0);
        }

        ReadingPlanProgressVO progress = buildProgressVO(plan);
        vo.setEstimatedEndDate(progress.getEstimatedEndDate());
        vo.setAvgDailyPages(progress.getAvgDailyPages());

        List<ReadingPlanBadge> badges = badgeMapper.listByUserId(plan.getUserId());
        vo.setBadges(badges.stream().map(ReadingPlanBadge::getBadgeType).collect(Collectors.toList()));

        return vo;
    }

    private ReadingPlanProgressVO buildProgressVO(ReadingPlan plan) {
        ReadingPlanProgressVO vo = new ReadingPlanProgressVO();
        vo.setPlanId(plan.getId());
        vo.setReadPages(plan.getReadPages());
        vo.setTotalPages(plan.getTotalPages());
        vo.setStreakDays(plan.getStreakDays());
        vo.setMaxStreakDays(plan.getMaxStreakDays());

        if (plan.getTotalPages() > 0) {
            vo.setProgress(Math.round((double) plan.getReadPages() / plan.getTotalPages() * 1000.0) / 10.0);
        } else {
            vo.setProgress(0.0);
        }

        LocalDate weekAgo = LocalDate.now().minusDays(7);
        List<ReadingPlanCheckin> recentCheckins = checkinMapper.listByPlanId(plan.getId()).stream()
            .filter(c -> c.getCheckinDate().isAfter(weekAgo) || c.getCheckinDate().isEqual(weekAgo))
            .collect(Collectors.toList());

        int avgDailyPages = 0;
        if (!recentCheckins.isEmpty()) {
            int totalPagesRecent = recentCheckins.stream().mapToInt(ReadingPlanCheckin::getPagesRead).sum();
            long distinctDays = recentCheckins.stream().map(ReadingPlanCheckin::getCheckinDate).distinct().count();
            if (distinctDays > 0) {
                avgDailyPages = (int) Math.ceil((double) totalPagesRecent / distinctDays);
            }
        }
        vo.setAvgDailyPages(avgDailyPages);

        int remainingPages = Math.max(0, plan.getTotalPages() - plan.getReadPages());
        vo.setRemainingPages(remainingPages);

        if (avgDailyPages > 0 && remainingPages > 0) {
            int remainingDays = (int) Math.ceil((double) remainingPages / avgDailyPages);
            vo.setRemainingDays(remainingDays);
            vo.setEstimatedEndDate(LocalDate.now().plusDays(remainingDays).toString());
        } else if (remainingPages == 0) {
            vo.setRemainingDays(0);
            vo.setEstimatedEndDate(LocalDate.now().toString());
        } else {
            vo.setRemainingDays(null);
            vo.setEstimatedEndDate(null);
        }

        List<LocalDate> checkinDates = checkinMapper.listCheckinDates(plan.getId());
        vo.setCheckinDates(checkinDates.stream().map(LocalDate::toString).collect(Collectors.toList()));

        return vo;
    }
}
