package com.xiaoan.bookstore.service;

import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.common.TenantValidator;
import com.xiaoan.bookstore.dto.QuotaVO;
import com.xiaoan.bookstore.dto.ReadingSummaryVO;
import com.xiaoan.bookstore.entity.ReadingRecord;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.ReadingRecordMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.dto.ReadingHistoryVO;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReadingService {

    private static final Logger log = LoggerFactory.getLogger(ReadingService.class);
    private final ReadingRecordMapper readingRecordMapper;
    private final MembershipService membershipService;
    private final ReadingGoalService readingGoalService;

    public ReadingRecord startReading(Long userId, Long bookId) {
        ReadingRecord record = new ReadingRecord();
        record.setUserId(userId);
        record.setBookId(bookId);
        record.setStartTime(LocalDateTime.now());
        record.setDuration(0);
        readingRecordMapper.insert(record);
        log.info("开始阅读: userId={}, bookId={}", userId, bookId);
        return record;
    }

    public void endReading(Long userId, Long recordId, Integer lastPage) {
        ReadingRecord record = readingRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("阅读记录不存在");
        }
        TenantValidator.validateCrossTenant(record.getUserId(), TenantContext.getTenantId());
        record.setEndTime(LocalDateTime.now());
        long seconds = java.time.Duration.between(record.getStartTime(), record.getEndTime()).getSeconds();
        record.setDuration((int) seconds);
        if (lastPage != null) {
            record.setLastPage(lastPage);
        }
        readingRecordMapper.updateById(record);
        log.info("结束阅读: recordId={}, duration={}s", recordId, seconds);

        if (seconds > 60) {
            readingGoalService.updateStreak(userId, record.getStartTime().toLocalDate());
        }
    }

    public ReadingSummaryVO summary(Long userId, String period) {
        QuotaVO quota = membershipService.getQuota(userId);
        boolean isVip = quota.getIsVip();

        LocalDate now = LocalDate.now();
        LocalDateTime start;
        LocalDateTime end;
        String periodStart;
        String periodEnd;

        switch (period) {
            case "week":
                LocalDate weekStart = now.with(DayOfWeek.MONDAY);
                start = weekStart.atStartOfDay();
                end = weekStart.plusDays(7).atStartOfDay();
                periodStart = weekStart.toString();
                periodEnd = weekStart.plusDays(6).toString();
                break;
            case "month":
                LocalDate monthStart = now.with(TemporalAdjusters.firstDayOfMonth());
                LocalDate monthEnd = now.with(TemporalAdjusters.lastDayOfMonth());
                start = monthStart.atStartOfDay();
                end = monthEnd.atTime(LocalTime.MAX);
                periodStart = monthStart.toString();
                periodEnd = monthEnd.toString();
                break;
            case "year":
                if (!isVip) {
                    throw new BusinessException("年度统计为会员专属功能，请升级会员");
                }
                LocalDate yearStart = now.with(TemporalAdjusters.firstDayOfYear());
                LocalDate yearEnd = now.with(TemporalAdjusters.lastDayOfYear());
                start = yearStart.atStartOfDay();
                end = yearEnd.atTime(LocalTime.MAX);
                periodStart = yearStart.toString();
                periodEnd = yearEnd.toString();
                break;
            default:
                throw new BusinessException("无效的统计周期，支持 week/month/year");
        }

        Long tenantId = TenantContext.getTenantId();
        ReadingSummaryVO vo = new ReadingSummaryVO();
        vo.setTotalDuration(readingRecordMapper.sumDuration(tenantId, start, end));
        vo.setBookCount(readingRecordMapper.countBooks(tenantId, start, end));
        vo.setDailyData(readingRecordMapper.dailyDuration(tenantId, start, end));
        vo.setPeriod(period);
        vo.setPeriodStart(periodStart);
        vo.setPeriodEnd(periodEnd);
        vo.setIsVip(isVip);

        if (isVip) {
            vo.setBookRank(readingRecordMapper.bookRank(tenantId, start, end));
            vo.setCategoryStats(readingRecordMapper.categoryStats(tenantId, start, end));
            vo.setReadingDays(readingRecordMapper.countReadingDays(tenantId, start, end));

            List<Map<String, Object>> dailyList = vo.getDailyData();
            if (dailyList != null && !dailyList.isEmpty()) {
                long totalDays = dailyList.size();
                long avgSeconds = vo.getTotalDuration() / totalDays;
                vo.setAvgDailyDuration(formatDuration(avgSeconds));
            } else {
                vo.setAvgDailyDuration("0分钟");
            }

            Map<String, Object> maxDay = readingRecordMapper.maxDayDuration(tenantId, start, end);
            if (maxDay != null && maxDay.get("total") != null) {
                vo.setMaxDayDuration(formatDuration(((Number) maxDay.get("total")).longValue()));
                vo.setMaxDayDate(String.valueOf(maxDay.get("date")));
            } else {
                vo.setMaxDayDuration("0分钟");
                vo.setMaxDayDate("");
            }
        }

        return vo;
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

    public List<Map<String, Object>> continueReadingList(Long userId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 5;
        }
        return readingRecordMapper.continueReadingList(userId, limit);
    }

    public Page<ReadingHistoryVO> readingHistoryPage(Long userId, int page, int size) {
        if (page <= 0) page = 1;
        if (size <= 0 || size > 100) size = 20;

        int offset = (page - 1) * size;
        List<Map<String, Object>> records = readingRecordMapper.readingHistoryPage(userId, offset, size);
        Integer total = readingRecordMapper.countReadingHistoryBooks(userId);

        List<ReadingHistoryVO> voList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Map<String, Object> record : records) {
            ReadingHistoryVO vo = new ReadingHistoryVO();
            vo.setBookId(((Number) record.get("bookId")).longValue());
            vo.setBookTitle((String) record.get("bookTitle"));
            vo.setBookAuthor((String) record.get("bookAuthor"));
            vo.setBookFormat((String) record.get("bookFormat"));
            vo.setCoverThumbnail((String) record.get("coverThumbnail"));
            vo.setPageCount(record.get("pageCount") != null ? ((Number) record.get("pageCount")).intValue() : 0);
            vo.setChapterCount(record.get("chapterCount") != null ? ((Number) record.get("chapterCount")).intValue() : 0);
            vo.setLastPage(record.get("lastPage") != null ? ((Number) record.get("lastPage")).intValue() : 0);
            vo.setLastChapter(record.get("lastChapter") != null ? ((Number) record.get("lastChapter")).intValue() : 0);
            vo.setSessionDuration(record.get("sessionDuration") != null ? ((Number) record.get("sessionDuration")).intValue() : 0);
            vo.setTotalDuration(record.get("totalDuration") != null ? ((Number) record.get("totalDuration")).longValue() : 0L);
            vo.setReadCount(record.get("readCount") != null ? ((Number) record.get("readCount")).intValue() : 0);

            Object lastReadTimeObj = record.get("lastReadTime");
            if (lastReadTimeObj != null) {
                if (lastReadTimeObj instanceof LocalDateTime) {
                    vo.setLastReadTime(((LocalDateTime) lastReadTimeObj).format(formatter));
                } else {
                    vo.setLastReadTime(String.valueOf(lastReadTimeObj));
                }
            }

            String format = vo.getBookFormat() == null ? "pdf" : vo.getBookFormat();
            double progress = 0;
            if ("epub".equals(format)) {
                int totalChapters = Math.max(1, vo.getChapterCount());
                int lastChapter = vo.getLastChapter() == null ? 0 : vo.getLastChapter();
                progress = Math.min(100.0, Math.round((double) lastChapter / totalChapters * 1000) / 10.0);
            } else {
                int totalPages = Math.max(1, vo.getPageCount());
                int lastPage = vo.getLastPage() == null ? 0 : vo.getLastPage();
                progress = Math.min(100.0, Math.round((double) lastPage / totalPages * 1000) / 10.0);
            }
            vo.setProgress(progress);

            voList.add(vo);
        }

        Page<ReadingHistoryVO> result = new Page<>(page, size);
        result.setRecords(voList);
        result.setTotal(total != null ? total.longValue() : 0L);
        return result;
    }

    public List<Map<String, Object>> readingTimeline(Long userId, String period) {
        LocalDate now = LocalDate.now();
        LocalDateTime start;
        LocalDateTime end;

        switch (period != null ? period : "month") {
            case "week":
                LocalDate weekStart = now.with(DayOfWeek.MONDAY);
                start = weekStart.atStartOfDay();
                end = weekStart.plusDays(7).atStartOfDay();
                break;
            case "year":
                LocalDate yearStart = now.with(TemporalAdjusters.firstDayOfYear());
                LocalDate yearEnd = now.with(TemporalAdjusters.lastDayOfYear());
                start = yearStart.atStartOfDay();
                end = yearEnd.atTime(LocalTime.MAX);
                break;
            default:
                LocalDate monthStart = now.with(TemporalAdjusters.firstDayOfMonth());
                LocalDate monthEnd = now.with(TemporalAdjusters.lastDayOfMonth());
                start = monthStart.atStartOfDay();
                end = monthEnd.atTime(LocalTime.MAX);
                break;
        }

        List<Map<String, Object>> flatList = readingRecordMapper.readingTimeline(userId, start, end);

        java.util.LinkedHashMap<String, java.util.List<Map<String, Object>>> dayMap = new java.util.LinkedHashMap<>();
        for (Map<String, Object> item : flatList) {
            String date = String.valueOf(item.get("date"));
            Map<String, Object> bookInfo = new java.util.HashMap<>();
            bookInfo.put("bookId", item.get("bookId"));
            bookInfo.put("bookTitle", item.get("bookTitle"));
            bookInfo.put("bookAuthor", item.get("bookAuthor"));
            bookInfo.put("duration", item.get("duration"));
            bookInfo.put("lastPage", item.get("lastPage"));

            dayMap.computeIfAbsent(date, k -> new java.util.ArrayList<>()).add(bookInfo);
        }

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Map.Entry<String, java.util.List<Map<String, Object>>> entry : dayMap.entrySet()) {
            Map<String, Object> dayItem = new java.util.HashMap<>();
            dayItem.put("date", entry.getKey());
            dayItem.put("books", entry.getValue());
            result.add(dayItem);
        }
        return result;
    }
}
