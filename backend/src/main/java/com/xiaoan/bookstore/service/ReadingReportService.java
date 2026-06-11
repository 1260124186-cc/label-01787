package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoan.bookstore.dto.ReadingReportVO;
import com.xiaoan.bookstore.entity.Annotation;
import com.xiaoan.bookstore.entity.Book;
import com.xiaoan.bookstore.entity.ReadingReport;
import com.xiaoan.bookstore.entity.User;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.AnnotationMapper;
import com.xiaoan.bookstore.mapper.BookMapper;
import com.xiaoan.bookstore.mapper.ReadingRecordMapper;
import com.xiaoan.bookstore.mapper.ReadingReportMapper;
import com.xiaoan.bookstore.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
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
    private final MembershipService membershipService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReadingReportVO generateReport(Long userId, String period) {
        checkReportPermission(userId, period);

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
                throw new BusinessException("无效的统计周期，支持 week/month/year");
        }

        ReadingReportVO vo = buildReportVO(userId, start, end, periodStart, periodEnd, reportType);

        ReadingReportVO lastPeriodVo = getLastPeriodReport(userId, period);
        if (lastPeriodVo != null) {
            Map<String, Object> compareData = calculateCompareData(vo, lastPeriodVo);
            vo.setCompareData(compareData);
        }

        saveReport(userId, vo);

        return vo;
    }

    private void checkReportPermission(Long userId, String period) {
        if ("week".equals(period)) {
            return;
        }
        boolean isVip = membershipService.isVip(userId);
        if (!isVip) {
            throw new BusinessException("月/年度报告为会员专属功能，请升级会员后查看");
        }
    }

    private ReadingReportVO buildReportVO(Long userId, LocalDateTime start, LocalDateTime end,
                                           String periodStart, String periodEnd, String reportType) {
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

        return vo;
    }

    private ReadingReportVO getLastPeriodReport(Long userId, String period) {
        LocalDate now = LocalDate.now();
        LocalDateTime lastStart;
        LocalDateTime lastEnd;

        switch (period) {
            case "week":
                LocalDate lastWeekStart = now.with(DayOfWeek.MONDAY).minusWeeks(1);
                lastStart = lastWeekStart.atStartOfDay();
                lastEnd = lastWeekStart.plusDays(7).atStartOfDay();
                break;
            case "month":
                LocalDate lastMonth = now.minusMonths(1);
                LocalDate lastMonthStart = lastMonth.with(TemporalAdjusters.firstDayOfMonth());
                LocalDate lastMonthEnd = lastMonth.with(TemporalAdjusters.lastDayOfMonth());
                lastStart = lastMonthStart.atStartOfDay();
                lastEnd = lastMonthEnd.atTime(LocalTime.MAX);
                break;
            case "year":
                LocalDate lastYear = now.minusYears(1);
                LocalDate lastYearStart = lastYear.with(TemporalAdjusters.firstDayOfYear());
                LocalDate lastYearEnd = lastYear.with(TemporalAdjusters.lastDayOfYear());
                lastStart = lastYearStart.atStartOfDay();
                lastEnd = lastYearEnd.atTime(LocalTime.MAX);
                break;
            default:
                return null;
        }

        try {
            return buildReportVO(userId, lastStart, lastEnd,
                    lastStart.toLocalDate().toString(),
                    lastEnd.toLocalDate().toString(),
                    "last_" + period);
        } catch (Exception e) {
            log.warn("计算上期报告数据失败", e);
            return null;
        }
    }

    private Map<String, Object> calculateCompareData(ReadingReportVO current, ReadingReportVO last) {
        Map<String, Object> compare = new HashMap<>();

        long currentDuration = current.getTotalDuration() != null ? current.getTotalDuration() : 0L;
        long lastDuration = last.getTotalDuration() != null ? last.getTotalDuration() : 0L;
        compare.put("totalDurationChange", calculateChangePercent(currentDuration, lastDuration));
        compare.put("totalDurationChangeText", formatChangeText(currentDuration, lastDuration, "时长"));

        int currentBooks = current.getBookCount() != null ? current.getBookCount() : 0;
        int lastBooks = last.getBookCount() != null ? last.getBookCount() : 0;
        compare.put("bookCountChange", calculateChangePercent(currentBooks, lastBooks));
        compare.put("bookCountChangeText", formatChangeText(currentBooks, lastBooks, "本书"));

        int currentAnnotations = current.getAnnotationCount() != null ? current.getAnnotationCount() : 0;
        int lastAnnotations = last.getAnnotationCount() != null ? last.getAnnotationCount() : 0;
        compare.put("annotationCountChange", calculateChangePercent(currentAnnotations, lastAnnotations));
        compare.put("annotationCountChangeText", formatChangeText(currentAnnotations, lastAnnotations, "条批注"));

        int currentReadingDays = current.getReadingDays() != null ? current.getReadingDays() : 0;
        int lastReadingDays = last.getReadingDays() != null ? last.getReadingDays() : 0;
        compare.put("readingDaysChange", calculateChangePercent(currentReadingDays, lastReadingDays));
        compare.put("readingDaysChangeText", formatChangeText(currentReadingDays, lastReadingDays, "天"));

        return compare;
    }

    private double calculateChangePercent(Number current, Number last) {
        double curr = current != null ? current.doubleValue() : 0;
        double lst = last != null ? last.doubleValue() : 0;
        if (lst == 0) {
            return curr > 0 ? 100.0 : 0.0;
        }
        BigDecimal change = BigDecimal.valueOf((curr - lst) / lst * 100);
        return change.setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private String formatChangeText(Number current, Number last, String unit) {
        double curr = current != null ? current.doubleValue() : 0;
        double lst = last != null ? last.doubleValue() : 0;
        double diff = curr - lst;
        if (diff > 0) {
            return "↑ 增加" + formatNumber(diff) + unit;
        } else if (diff < 0) {
            return "↓ 减少" + formatNumber(Math.abs(diff)) + unit;
        } else {
            return "持平";
        }
    }

    private String formatNumber(double num) {
        if (num == (long) num) {
            return String.valueOf((long) num);
        }
        return String.format("%.1f", num);
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
            ReadingReport existing = readingReportMapper.selectOne(
                    new LambdaQueryWrapper<ReadingReport>()
                            .eq(ReadingReport::getUserId, userId)
                            .eq(ReadingReport::getReportType, vo.getReportType())
                            .eq(ReadingReport::getPeriodStart, LocalDate.parse(vo.getPeriodStart()))
                            .eq(ReadingReport::getPeriodEnd, LocalDate.parse(vo.getPeriodEnd()))
                            .orderByDesc(ReadingReport::getCreatedAt)
                            .last("LIMIT 1")
            );

            ReadingReport report;
            if (existing != null) {
                report = existing;
            } else {
                report = new ReadingReport();
                report.setUserId(userId);
                report.setReportType(vo.getReportType());
                report.setPeriodStart(LocalDate.parse(vo.getPeriodStart()));
                report.setPeriodEnd(LocalDate.parse(vo.getPeriodEnd()));
                report.setShareCount(0);
            }
            report.setTotalDuration(vo.getTotalDuration());
            report.setBookCount(vo.getBookCount());
            report.setAnnotationCount(vo.getAnnotationCount());
            report.setMaxStreakDays(vo.getMaxStreakDays());
            report.setReadingDays(vo.getReadingDays());
            report.setReportData(objectMapper.writeValueAsString(vo));
            report.setStatus(1);

            if (existing != null) {
                readingReportMapper.updateById(report);
            } else {
                readingReportMapper.insert(report);
            }

            vo.setId(report.getId());
        } catch (Exception e) {
            log.error("保存阅读报告失败", e);
        }
    }

    public ReadingReportVO getReportDetail(Long userId, Long reportId) {
        ReadingReport report = readingReportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("报告不存在");
        }
        if (!report.getUserId().equals(userId)) {
            throw new BusinessException("无权查看此报告");
        }
        try {
            ReadingReportVO vo = objectMapper.readValue(report.getReportData(),
                    new TypeReference<ReadingReportVO>() {});
            vo.setId(report.getId());
            vo.setShareCount(report.getShareCount() != null ? report.getShareCount() : 0);
            return vo;
        } catch (Exception e) {
            log.error("解析报告数据失败", e);
            throw new BusinessException("报告数据解析失败");
        }
    }

    public void deleteReport(Long userId, Long reportId) {
        ReadingReport report = readingReportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("报告不存在");
        }
        if (!report.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此报告");
        }
        readingReportMapper.deleteById(reportId);
    }

    public void incrementShareCount(Long userId, Long reportId) {
        ReadingReport report = readingReportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("报告不存在");
        }
        if (!report.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此报告");
        }
        report.setShareCount((report.getShareCount() != null ? report.getShareCount() : 0) + 1);
        readingReportMapper.updateById(report);
    }

    public List<ReadingReportVO> getReportList(Long userId, Integer page, Integer size) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;

        List<ReadingReport> reports = readingReportMapper.selectList(
                new LambdaQueryWrapper<ReadingReport>()
                        .eq(ReadingReport::getUserId, userId)
                        .eq(ReadingReport::getStatus, 1)
                        .orderByDesc(ReadingReport::getCreatedAt)
                        .last("LIMIT " + (page - 1) * size + "," + size)
        );

        List<ReadingReportVO> voList = new ArrayList<>();
        for (ReadingReport report : reports) {
            try {
                ReadingReportVO vo = objectMapper.readValue(report.getReportData(),
                        new TypeReference<ReadingReportVO>() {});
                vo.setId(report.getId());
                vo.setShareCount(report.getShareCount() != null ? report.getShareCount() : 0);
                voList.add(vo);
            } catch (Exception e) {
                log.warn("解析报告数据失败，reportId={}", report.getId(), e);
            }
        }
        return voList;
    }

    public Map<String, Object> adminStats(Integer days) {
        if (days == null || days < 1) days = 30;
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(days);

        Map<String, Object> result = new HashMap<>();

        Long totalReports = readingReportMapper.selectCount(
                new LambdaQueryWrapper<ReadingReport>()
                        .ge(ReadingReport::getCreatedAt, start)
                        .lt(ReadingReport::getCreatedAt, end)
        );
        result.put("totalReports", totalReports != null ? totalReports : 0L);

        Long totalShares = 0;
        List<ReadingReport> reports = readingReportMapper.selectList(
                new LambdaQueryWrapper<ReadingReport>()
                        .ge(ReadingReport::getCreatedAt, start)
                        .lt(ReadingReport::getCreatedAt, end)
        );
        if (reports != null) {
            for (ReadingReport report : reports) {
                totalShares += report.getShareCount() != null ? report.getShareCount() : 0;
            }
        }
        result.put("totalShares", totalShares);

        Map<String, Long> reportTypeCount = new HashMap<>();
        reportTypeCount.put("weekly", 0L);
        reportTypeCount.put("monthly", 0L);
        reportTypeCount.put("yearly", 0L);
        if (reports != null) {
            for (ReadingReport report : reports) {
                String type = report.getReportType();
                if (reportTypeCount.containsKey(type)) {
                    reportTypeCount.put(type, reportTypeCount.get(type) + 1);
                }
            }
        }
        result.put("reportTypeCount", reportTypeCount);

        Long uniqueUsers = readingRecordMapper.countActiveUsers(start, end);
        result.put("activeUsers", uniqueUsers != null ? uniqueUsers : 0L);

        List<Map<String, Object>> dailyStats = readingRecordMapper.dailyReadingStats(start, end);
        result.put("dailyStats", dailyStats);

        return result;
    }

    public IPage<ReadingReport> adminReportList(Integer page, Integer size, String reportType, Long userId) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;

        LambdaQueryWrapper<ReadingReport> wrapper = new LambdaQueryWrapper<>();
        if (reportType != null && !reportType.isEmpty()) {
            wrapper.eq(ReadingReport::getReportType, reportType);
        }
        if (userId != null) {
            wrapper.eq(ReadingReport::getUserId, userId);
        }
        wrapper.orderByDesc(ReadingReport::getCreatedAt);

        return readingReportMapper.selectPage(new Page<>(page, size), wrapper);
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
