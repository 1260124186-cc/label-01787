package com.xiaoan.bookstore.service.metadata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoan.bookstore.dto.BookMetadataVO;
import com.xiaoan.bookstore.dto.MetadataSearchQuery;
import com.xiaoan.bookstore.service.SysConfigService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class BookMetadataCache {

    private static final Logger log = LoggerFactory.getLogger(BookMetadataCache.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SysConfigService sysConfigService;

    private Cache<String, BookMetadataVO> localCache;

    @PostConstruct
    public void init() {
        int maxLocalSize = sysConfigService.getInt("metadata.cache.local_max_size", 1000);
        int localExpireMinutes = sysConfigService.getInt("metadata.cache.local_expire_minutes", 60);
        localCache = Caffeine.newBuilder()
                .maximumSize(maxLocalSize)
                .expireAfterWrite(localExpireMinutes, TimeUnit.MINUTES)
                .build();
    }

    private String buildCacheKey(String source, MetadataSearchQuery query) {
        StringBuilder sb = new StringBuilder();
        sb.append("metadata:cache:").append(source).append(":");
        if (query.getIsbn() != null && !query.getIsbn().isBlank()) {
            sb.append("isbn:").append(query.getIsbn().trim());
        } else {
            StringBuilder raw = new StringBuilder();
            if (query.getTitle() != null) raw.append("t:").append(query.getTitle().trim().toLowerCase());
            if (query.getAuthor() != null) raw.append("a:").append(query.getAuthor().trim().toLowerCase());
            sb.append("title:").append(md5(raw.toString()));
        }
        return sb.toString();
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }

    public BookMetadataVO get(String source, MetadataSearchQuery query) {
        String key = buildCacheKey(source, query);

        BookMetadataVO local = localCache.getIfPresent(key);
        if (local != null) {
            return local;
        }

        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null && !json.isBlank()) {
                BookMetadataVO vo = objectMapper.readValue(json, BookMetadataVO.class);
                localCache.put(key, vo);
                return vo;
            }
        } catch (JsonProcessingException e) {
            log.warn("解析缓存的元数据失败: key={}", key, e);
        } catch (Exception e) {
            log.warn("读取Redis缓存失败: key={}", key, e);
        }

        return null;
    }

    public void put(String source, MetadataSearchQuery query, BookMetadataVO vo) {
        if (vo == null) return;
        String key = buildCacheKey(source, query);

        localCache.put(key, vo);

        try {
            int expireHours = sysConfigService.getInt("metadata.cache.redis_expire_hours", 24 * 7);
            String json = objectMapper.writeValueAsString(vo);
            redisTemplate.opsForValue().set(key, json, expireHours, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("写入Redis缓存失败: key={}", key, e);
        }
    }

    public void evict(String source, MetadataSearchQuery query) {
        String key = buildCacheKey(source, query);
        localCache.invalidate(key);
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("清除Redis缓存失败: key={}", key, e);
        }
    }

    public void clearLocal() {
        localCache.invalidateAll();
    }
}
