package com.xiaoan.bookstore.service;

import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.common.TenantValidator;
import com.xiaoan.bookstore.dto.ReadingSummaryVO;
import com.xiaoan.bookstore.entity.ReadingRecord;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.ReadingRecordMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

@Service
@RequiredArgsConstructor
public class ReadingService {

    private static final Logger log = LoggerFactory.getLogger(ReadingService.class);
    private final ReadingRecordMapper readingRecordMapper;

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
    }

    public ReadingSummaryVO summary(Long userId, String period) {
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
        return vo;
    }
}
