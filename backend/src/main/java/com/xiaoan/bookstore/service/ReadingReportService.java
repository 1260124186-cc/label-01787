package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoan.bookstore.dto.ReadingReportVO;
import com.xiaoan.bookstore.entity.Annotation;
import com.xiaoan.bookstore.entity.Book;
import com.xiaoan.bookstore.entity.ReadingReport;
import com.xiaoan.bookstore.entity.User;
import com.xiaoan.bookstore.mapper.AnnotationMapper;
import com.xiaoan.bookstore.mapper.BookMapper;
import com.xiaoan.bookstore.mapper.ReadingRecordMapper;
import com.xiaoan.bookstore.mapper.ReadingReportMapper;
import com.xiaoan.bookstore.mapper.UserMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReadingReportService {

    private static final Logger log = LoggerFactory.getLogger(ReadingReportService.class);
    private final ReadingReportMapper readingReportMapper;
    private final ReadingRecordMapper readingRecordMapper;
    private final AnnotationMapper annotationMapper;
    private final UserMapper userMapper;
    private final BookMapper bookMapper;
    private final ReadingGoalService readingGoalService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReadingReportVO generateReport(Long userId, String period) {
        LocalDate now = LocalDate.now();
        LocalDateTime start;
        LocalDateTime end;
        String periodStart;
        String periodEnd;
        String reportType;

        switch (period) {
            case "week":
                LocalDate weekStart = now.with(DayOfWeek.MONDAY);
                start = weekStart.atStartOfDay();
                end = weekStart.plusDays(7).atStartOfDay();
                periodStart = weekStart.toString();
                periodEnd = weekStart.plusDays(6).toString();
                reportType = "weekly";
                break;
            case "month":
                LocalDate monthStart = now.with(TemporalAdjusters.firstDayOfMonth());
                LocalDate monthEnd = now.with(TemporalAdjusters.lastDayOfMonth());
                start = monthStart.atStartOfDay();
                end = monthEnd.atTime(LocalTime.MAX);
                periodStart = monthStart.toString();
                periodEnd = monthEnd.toString();
                reportType = "monthly";
                break;
            case "year":
                LocalDate yearStart = now.with(TemporalAdjusters.firstDayOfYear());
                LocalDate yearEnd = now.with(TemporalAdjusters.lastDayOfYear());
                start = yearStart.atStartOfDay();
                end = yearEnd.atTime(LocalTime.MAX);
                periodStart = yearStart.toString();
                periodEnd = yearEnd.toString();
                reportType = "yearly";
                break;
            default:
                throw new com.xiaoan.bookstore.exception.BusinessException("无效的统计周期，支持 week/month/year");
        }

        ReadingReportVO vo = new ReadingReportVO();
        vo.setReportType(reportType);
        vo.setPeriodStart(periodStart);
        vo.setPeriodEnd(periodEnd);

        Long totalDuration = readingRecordMapper.sumDuration(userId, start, end);
        vo.setTotalDuration(totalDuration != null ? totalDuration : 0L);
        vo.setTotalDurationText(formatDuration(vo.getTotalDuration()));

        Integer bookCount = readingRecordMapper.countBooks(userId, start, end);
        vo.setBookCount(bookCount != null ? bookCount : 0);

        Long annotationCount = annotationMapper.selectCount(
                new LambdaQueryWrapper<Annotation>()
                        .eq(Annotation::getUserId, userId)
                        .ge(Annotation::getCreatedAt, start)
                        .lt(Annotation::getCreatedAt, end)
        );
        vo.setAnnotationCount(annotationCount != null ? annotationCount.intValue() : 0);

        List<Map<String, Object>> bookRank = readingRecordMapper.bookRank(userId, start, end);
        if (bookRank != null && bookRank.size() > 5) {
            bookRank = bookRank.subList(0, 5);
        }
        vo.setBookRank(bookRank);

        vo.setCategoryStats(readingRecordMapper.categoryStats(userId, start, end));

        vo.setDailyData(readingRecordMapper.dailyDuration(userId, start, end));

        Integer readingDays = readingRecordMapper.countReadingDays(userId, start, end);
        vo.setReadingDays(readingDays != null ? readingDays : 0);

        int currentStreak = readingGoalService.calculateCurrentStreak(userId);
        vo.setCurrentStreakDays(currentStreak);

        User user = userMapper.selectById(userId);
        vo.setMaxStreakDays(user != null && user.getMaxStreakDays() != null ? user.getMaxStreakDays() : 0);

        List<Map<String, Object>> dailyList = vo.getDailyData();
        if (dailyList != null && !dailyList.isEmpty()) {
            long totalDays = dailyList.size();
            long avgSeconds = vo.getTotalDuration() / totalDays;
            vo.setAvgDailyDuration(formatDuration(avgSeconds));
        } else {
            vo.setAvgDailyDuration("0分钟");
        }

        Map<String, Object> maxDay = readingRecordMapper.maxDayDuration(userId, start, end);
        if (maxDay != null && maxDay.get("total") != null) {
            vo.setMaxDayDuration(formatDuration(((Number) maxDay.get("total")).longValue()));
            vo.setMaxDayDate(String.valueOf(maxDay.get("date")));
        } else {
            vo.setMaxDayDuration("0分钟");
            vo.setMaxDayDate("");
        }

        vo.setFinishedBookCount(calculateFinishedBooks(userId, start, end));

        saveReport(userId, vo);

        return vo;
    }

    private int calculateFinishedBooks(Long userId, LocalDateTime start, LocalDateTime end) {
        List<Book> userBooks = bookMapper.selectList(
                new LambdaQueryWrapper<Book>()
                        .eq(Book::getUserId, userId)
                        .eq(Book::getStatus, 1)
        );

        int finishedCount = 0;
        for (Book book : userBooks) {
            if (book.getPageCount() != null && book.getPageCount() > 0
                    && book.getLastPage() != null && book.getLastPage() >= book.getPageCount()) {
                finishedCount++;
            }
        }
        return finishedCount;
    }

    private void saveReport(Long userId, ReadingReportVO vo) {
        try {
            ReadingReport report = new ReadingReport();
            report.setUserId(userId);
            report.setReportType(vo.getReportType());
            report.setPeriodStart(LocalDate.parse(vo.getPeriodStart()));
            report.setPeriodEnd(LocalDate.parse(vo.getPeriodEnd()));
            report.setTotalDuration(vo.getTotalDuration());
            report.setBookCount(vo.getBookCount());
            report.setAnnotationCount(vo.getAnnotationCount());
            report.setMaxStreakDays(vo.getMaxStreakDays());
            report.setReadingDays(vo.getReadingDays());
            report.setReportData(objectMapper.writeValueAsString(vo));
            report.setStatus(1);
            readingReportMapper.insert(report);
        } catch (Exception e) {
            log.error("保存阅读报告失败", e);
        }
    }

    public List<ReadingReport> getReportList(Long userId, Integer page, Integer size) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;

        List<ReadingReport> reports = readingReportMapper.selectList(
                new LambdaQueryWrapper<ReadingReport>()
                        .eq(ReadingReport::getUserId, userId)
                        .orderByDesc(ReadingReport::getCreatedAt)
                        .last("LIMIT " + (page - 1) * size + "," + size)
        );

        return reports != null ? reports : new ArrayList<>();
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
