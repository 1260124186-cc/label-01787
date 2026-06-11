package com.xiaoan.bookstore.service;

import com.xiaoan.bookstore.entity.FileDownloadLog;
import com.xiaoan.bookstore.mapper.FileDownloadLogMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class FileDownloadLogService {

    private static final Logger log = LoggerFactory.getLogger(FileDownloadLogService.class);
    private static final String DOWNLOAD_TOKEN_KEY_PREFIX = "download:token:";
    private static final String DOWNLOAD_USER_KEY_PREFIX = "download:user:";

    private final FileDownloadLogMapper downloadLogMapper;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.file.max-downloads-per-hour:10}")
    private int maxDownloadsPerTokenPerHour;

    @Value("${app.file.max-downloads-per-day:100}")
    private int maxDownloadsPerUserPerDay;

    public void logDownload(FileDownloadLog log) {
        downloadLogMapper.insert(log);
        incrementTokenCount(log.getFileToken());
        incrementUserCount(log.getUserId(), log.getUserType());
    }

    public boolean isTokenLimitExceeded(String fileToken) {
        String key = DOWNLOAD_TOKEN_KEY_PREFIX + fileToken;
        String countStr = redisTemplate.opsForValue().get(key);
        if (countStr == null) {
            return false;
        }
        int count = Integer.parseInt(countStr);
        return count >= maxDownloadsPerTokenPerHour;
    }

    public boolean isUserLimitExceeded(Long userId, Integer userType) {
        String key = DOWNLOAD_USER_KEY_PREFIX + userType + ":" + userId;
        String countStr = redisTemplate.opsForValue().get(key);
        if (countStr == null) {
            return false;
        }
        int count = Integer.parseInt(countStr);
        return count >= maxDownloadsPerUserPerDay;
    }

    private void incrementTokenCount(String fileToken) {
        try {
            String key = DOWNLOAD_TOKEN_KEY_PREFIX + fileToken;
            Long count = redisTemplate.opsForValue().increment(key, 1);
            if (count != null && count == 1) {
                redisTemplate.expire(key, 1, TimeUnit.HOURS);
            }
        } catch (Exception e) {
            log.warn("Redis increment token count failed, fallback to DB", e);
        }
    }

    private void incrementUserCount(Long userId, Integer userType) {
        try {
            String key = DOWNLOAD_USER_KEY_PREFIX + userType + ":" + userId;
            Long count = redisTemplate.opsForValue().increment(key, 1);
            if (count != null && count == 1) {
                redisTemplate.expire(key, 1, TimeUnit.DAYS);
            }
        } catch (Exception e) {
            log.warn("Redis increment user count failed, fallback to DB", e);
        }
    }

    @Deprecated
    public int countByTokenFromDb(String fileToken) {
        LocalDateTime since = LocalDateTime.now().minusHours(1);
        return downloadLogMapper.countByTokenSince(fileToken, since);
    }

    @Deprecated
    public int countByUserFromDb(Long userId, Integer userType) {
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        return downloadLogMapper.countByUserSince(userId, userType, since);
    }
}
