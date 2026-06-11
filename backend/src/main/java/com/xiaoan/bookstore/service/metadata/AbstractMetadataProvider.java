package com.xiaoan.bookstore.service.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.dto.BookMetadataVO;
import com.xiaoan.bookstore.dto.MetadataSearchQuery;
import com.xiaoan.bookstore.dto.MetadataSourceStatusVO;
import com.xiaoan.bookstore.service.SysConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class AbstractMetadataProvider implements BookMetadataProvider {

    protected static final Logger log = LoggerFactory.getLogger(AbstractMetadataProvider.class);

    protected final SysConfigService sysConfigService;
    protected final StringRedisTemplate redisTemplate;
    protected final ObjectMapper objectMapper;
    protected final BookMetadataCache cache;

    protected AbstractMetadataProvider(SysConfigService sysConfigService,
                                       StringRedisTemplate redisTemplate,
                                       ObjectMapper objectMapper,
                                       BookMetadataCache cache) {
        this.sysConfigService = sysConfigService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cache = cache;
    }

    protected String getConfigKey(String suffix) {
        return "metadata." + getSourceCode() + "." + suffix;
    }

    protected String getApiKey() {
        return sysConfigService.getString(getConfigKey("api_key"), "");
    }

    protected int getDailyLimit() {
        return sysConfigService.getInt(getConfigKey("daily_limit"), 1000);
    }

    protected int getTimeoutMs() {
        return sysConfigService.getInt(getConfigKey("timeout_ms"), 5000);
    }

    protected boolean isProviderEnabled() {
        return sysConfigService.getBoolean(getConfigKey("enabled"), true);
    }

    protected int getProviderPriority() {
        return sysConfigService.getInt(getConfigKey("priority"), 100);
    }

    protected String getDailyCallsKey() {
        String date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        return "metadata:calls:" + getSourceCode() + ":" + date;
    }

    protected String getTotalCallsKey() {
        return "metadata:calls:" + getSourceCode() + ":total";
    }

    protected String getSuccessCountKey() {
        return "metadata:calls:" + getSourceCode() + ":success";
    }

    protected String getFailCountKey() {
        return "metadata:calls:" + getSourceCode() + ":fail";
    }

    protected String getLastCallKey() {
        return "metadata:calls:" + getSourceCode() + ":last";
    }

    protected boolean checkRateLimit() {
        String key = getDailyCallsKey();
        String countStr = redisTemplate.opsForValue().get(key);
        long count = countStr == null ? 0 : Long.parseLong(countStr);
        return count < getDailyLimit();
    }

    protected void recordCall(boolean success) {
        try {
            String dailyKey = getDailyCallsKey();
            redisTemplate.opsForValue().increment(dailyKey);
            redisTemplate.expire(dailyKey, 2, TimeUnit.DAYS);

            redisTemplate.opsForValue().increment(getTotalCallsKey());
            if (success) {
                redisTemplate.opsForValue().increment(getSuccessCountKey());
            } else {
                redisTemplate.opsForValue().increment(getFailCountKey());
            }
            redisTemplate.opsForValue().set(getLastCallKey(),
                    java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } catch (Exception e) {
            log.warn("记录元数据调用统计失败: source={}", getSourceCode(), e);
        }
    }

    public MetadataSourceStatusVO getStatus() {
        MetadataSourceStatusVO vo = new MetadataSourceStatusVO();
        vo.setSourceCode(getSourceCode());
        vo.setSourceName(getSourceName());
        vo.setEnabled(isEnabled());
        vo.setPriority(getPriority());
        vo.setHasApiKey(getApiKey() != null && !getApiKey().isBlank());

        try {
            String dailyStr = redisTemplate.opsForValue().get(getDailyCallsKey());
            vo.setDailyCallsUsed(dailyStr == null ? 0 : Long.parseLong(dailyStr));
        } catch (Exception e) {
            vo.setDailyCallsUsed(0);
        }
        vo.setDailyCallsLimit(getDailyLimit());

        try {
            String totalStr = redisTemplate.opsForValue().get(getTotalCallsKey());
            vo.setTotalCalls(totalStr == null ? 0 : Long.parseLong(totalStr));
        } catch (Exception e) {
            vo.setTotalCalls(0);
        }

        try {
            String succStr = redisTemplate.opsForValue().get(getSuccessCountKey());
            vo.setSuccessCount(succStr == null ? 0 : Long.parseLong(succStr));
        } catch (Exception e) {
            vo.setSuccessCount(0);
        }

        try {
            String failStr = redisTemplate.opsForValue().get(getFailCountKey());
            vo.setFailCount(failStr == null ? 0 : Long.parseLong(failStr));
        } catch (Exception e) {
            vo.setFailCount(0);
        }

        try {
            vo.setLastCalledAt(redisTemplate.opsForValue().get(getLastCallKey()));
        } catch (Exception e) {
            vo.setLastCalledAt(null);
        }

        return vo;
    }

    @Override
    public boolean isEnabled() {
        return isProviderEnabled();
    }

    @Override
    public int getPriority() {
        return getProviderPriority();
    }

    @Override
    public BookMetadataVO searchOne(MetadataSearchQuery query) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        if (!isEnabled()) {
            return null;
        }

        BookMetadataVO cached = cache.get(getSourceCode(), query);
        if (cached != null) {
            return cached;
        }

        if (!checkRateLimit()) {
            log.warn("元数据源调用超限: source={}", getSourceCode());
            return null;
        }

        try {
            BookMetadataVO result = doSearchOne(query);
            recordCall(result != null);
            if (result != null) {
                cache.put(getSourceCode(), query, result);
            }
            return result;
        } catch (Exception e) {
            log.error("搜索元数据失败: source={}, query={}", getSourceCode(), query, e);
            recordCall(false);
            return null;
        }
    }

    @Override
    public List<BookMetadataVO> search(MetadataSearchQuery query, int limit) {
        if (query == null || query.isEmpty()) {
            return Collections.emptyList();
        }
        if (!isEnabled()) {
            return Collections.emptyList();
        }
        if (!checkRateLimit()) {
            log.warn("元数据源调用超限: source={}", getSourceCode());
            return Collections.emptyList();
        }

        try {
            List<BookMetadataVO> results = doSearch(query, limit);
            recordCall(results != null && !results.isEmpty());
            return results != null ? results : Collections.emptyList();
        } catch (Exception e) {
            log.error("搜索元数据列表失败: source={}, query={}", getSourceCode(), query, e);
            recordCall(false);
            return Collections.emptyList();
        }
    }

    protected abstract BookMetadataVO doSearchOne(MetadataSearchQuery query) throws Exception;

    protected abstract List<BookMetadataVO> doSearch(MetadataSearchQuery query, int limit) throws Exception;
}
