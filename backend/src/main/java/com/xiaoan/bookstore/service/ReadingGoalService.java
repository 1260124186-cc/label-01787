package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoan.bookstore.dto.ReadingGoalDTO;
import com.xiaoan.bookstore.dto.ReadingGoalProgressVO;
import com.xiaoan.bookstore.entity.ReadingGoal;
import com.xiaoan.bookstore.entity.User;
import com.xiaoan.bookstore.mapper.ReadingGoalMapper;
import com.xiaoan.bookstore.mapper.ReadingRecordMapper;
import com.xiaoan.bookstore.mapper.UserMapper;
import com.xiaoan.bookstore.mapper.AnnotationMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReadingGoalService {

    private static final Logger log = LoggerFactory.getLogger(ReadingGoalService.class);
    private final ReadingGoalMapper readingGoalMapper;
    private final ReadingRecordMapper readingRecordMapper;
    private final UserMapper userMapper;
    private final AnnotationMapper annotationMapper;

    public ReadingGoal getGoal(Long userId) {
        ReadingGoal goal = readingGoalMapper.selectOne(
                new LambdaQueryWrapper<ReadingGoal>().eq(ReadingGoal::getUserId, userId)
        );
        if (goal == null) {
            goal = createDefaultGoal(userId);
        }
        return goal;
    }

    public ReadingGoal updateGoal(Long userId, ReadingGoalDTO dto) {
        ReadingGoal goal = readingGoalMapper.selectOne(
                new LambdaQueryWrapper<ReadingGoal>().eq(ReadingGoal::getUserId, userId)
        );
        if (goal == null) {
            goal = new ReadingGoal();
            goal.setUserId(userId);
            goal.setDailyGoalMinutes(30);
            goal.setWeeklyGoalMinutes(210);
            goal.setGoalType(1);
            goal.setRemindEnabled(0);
            goal.setRemindTime("");
        }

        if (dto.getDailyGoalMinutes() != null) {
            goal.setDailyGoalMinutes(dto.getDailyGoalMinutes());
        }
        if (dto.getWeeklyGoalMinutes() != null) {
            goal.setWeeklyGoalMinutes(dto.getWeeklyGoalMinutes());
        }
        if (dto.getGoalType() != null) {
            goal.setGoalType(dto.getGoalType());
        }
        if (dto.getRemindEnabled() != null) {
            goal.setRemindEnabled(dto.getRemindEnabled());
        }
        if (dto.getRemindTime() != null) {
            goal.setRemindTime(dto.getRemindTime());
        }

        if (goal.getId() == null) {
            readingGoalMapper.insert(goal);
        } else {
            readingGoalMapper.updateById(goal);
        }

        log.info("更新阅读目标: userId={}, daily={}分钟, weekly={}分钟",
                userId, goal.getDailyGoalMinutes(), goal.getWeeklyGoalMinutes());
        return goal;
    }

    public ReadingGoalProgressVO getProgress(Long userId) {
        ReadingGoal goal = getGoal(userId);
        User user = userMapper.selectById(userId);

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);

        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);
        LocalDateTime weekStartDateTime = weekStart.atStartOfDay();
        LocalDateTime weekEndDateTime = weekStart.plusDays(7).atStartOfDay();

        Long todayDuration = readingRecordMapper.sumDuration(userId, todayStart, todayEnd);
        Long weekDuration = readingRecordMapper.sumDuration(userId, weekStartDateTime, weekEndDateTime);

        if (todayDuration == null) todayDuration = 0L;
        if (weekDuration == null) weekDuration = 0L;

        double dailyGoalSeconds = goal.getDailyGoalMinutes() * 60.0;
        double weeklyGoalSeconds = goal.getWeeklyGoalMinutes() * 60.0;

        double dailyProgress = dailyGoalSeconds > 0 ? Math.min(todayDuration / dailyGoalSeconds * 100, 100) : 0;
        double weeklyProgress = weeklyGoalSeconds > 0 ? Math.min(weekDuration / weeklyGoalSeconds * 100, 100) : 0;

        ReadingGoalProgressVO vo = new ReadingGoalProgressVO();
        vo.setDailyGoalMinutes(goal.getDailyGoalMinutes());
        vo.setWeeklyGoalMinutes(goal.getWeeklyGoalMinutes());
        vo.setGoalType(goal.getGoalType());
        vo.setTodayDuration(todayDuration);
        vo.setWeekDuration(weekDuration);
        vo.setDailyProgress(Math.round(dailyProgress * 10.0) / 10.0);
        vo.setWeeklyProgress(Math.round(weeklyProgress * 10.0) / 10.0);
        vo.setDailyCompleted(dailyProgress >= 100);
        vo.setWeeklyCompleted(weeklyProgress >= 100);

        if (user != null) {
            vo.setCurrentStreakDays(user.getCurrentStreakDays() != null ? user.getCurrentStreakDays() : 0);
            vo.setMaxStreakDays(user.getMaxStreakDays() != null ? user.getMaxStreakDays() : 0);
        } else {
            vo.setCurrentStreakDays(0);
            vo.setMaxStreakDays(0);
        }

        List<Map<String, Object>> bookRank = readingRecordMapper.bookRank(userId, weekStartDateTime, weekEndDateTime);
        if (bookRank != null && bookRank.size() > 5) {
            bookRank = bookRank.subList(0, 5);
        }
        vo.setBookRank(bookRank);

        vo.setCategoryStats(readingRecordMapper.categoryStats(userId, weekStartDateTime, weekEndDateTime));

        int annotationCount = annotationMapper.selectCount(
                new LambdaQueryWrapper<com.xiaoan.bookstore.entity.Annotation>()
                        .eq(com.xiaoan.bookstore.entity.Annotation::getUserId, userId)
        ).intValue();
        vo.setAnnotationCount(annotationCount);

        vo.setFinishedBookCount(0);

        return vo;
    }

    public void updateStreak(Long userId, LocalDate readDate) {
        User user = userMapper.selectById(userId);
        if (user == null) return;

        LocalDate lastReadDate = user.getLastReadDate();
        int currentStreak = user.getCurrentStreakDays() != null ? user.getCurrentStreakDays() : 0;
        int maxStreak = user.getMaxStreakDays() != null ? user.getMaxStreakDays() : 0;

        if (lastReadDate == null) {
            currentStreak = 1;
        } else if (lastReadDate.equals(readDate)) {
            return;
        } else if (lastReadDate.plusDays(1).equals(readDate)) {
            currentStreak++;
        } else {
            currentStreak = 1;
        }

        if (currentStreak > maxStreak) {
            maxStreak = currentStreak;
        }

        user.setLastReadDate(readDate);
        user.setCurrentStreakDays(currentStreak);
        user.setMaxStreakDays(maxStreak);
        userMapper.updateById(user);

        log.info("更新连续阅读: userId={}, streak={}天, max={}天", userId, currentStreak, maxStreak);
    }

    private ReadingGoal createDefaultGoal(Long userId) {
        ReadingGoal goal = new ReadingGoal();
        goal.setUserId(userId);
        goal.setDailyGoalMinutes(30);
        goal.setWeeklyGoalMinutes(210);
        goal.setGoalType(1);
        goal.setRemindEnabled(0);
        goal.setRemindTime("");
        readingGoalMapper.insert(goal);
        return goal;
    }

    public int calculateCurrentStreak(Long userId) {
        List<Map<String, Object>> dailyList = readingRecordMapper.dailyDuration(
                userId,
                LocalDate.now().minusDays(365).atStartOfDay(),
                LocalDateTime.now()
        );

        if (dailyList == null || dailyList.isEmpty()) {
            return 0;
        }

        java.util.Set<LocalDate> readDates = new java.util.HashSet<>();
        for (Map<String, Object> day : dailyList) {
            String dateStr = String.valueOf(day.get("date"));
            readDates.add(LocalDate.parse(dateStr));
        }

        LocalDate today = LocalDate.now();
        int streak = 0;
        LocalDate checkDate = today;

        if (!readDates.contains(today)) {
            checkDate = today.minusDays(1);
            if (!readDates.contains(checkDate)) {
                return 0;
            }
        }

        while (readDates.contains(checkDate)) {
            streak++;
            checkDate = checkDate.minusDays(1);
        }

        return streak;
    }
}
