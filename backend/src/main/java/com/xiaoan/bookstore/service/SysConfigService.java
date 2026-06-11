package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoan.bookstore.entity.SysConfig;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.SysConfigMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SysConfigService {

    private static final Logger log = LoggerFactory.getLogger(SysConfigService.class);

    private final SysConfigMapper sysConfigMapper;
    private final Map<String, SysConfig> configCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        refreshCache();
    }

    public void refreshCache() {
        try {
            List<SysConfig> configs = sysConfigMapper.selectList(null);
            configCache.clear();
            for (SysConfig config : configs) {
                configCache.put(config.getConfigKey(), config);
            }
            log.info("系统配置缓存已刷新，共加载 {} 条配置", configCache.size());
        } catch (Exception e) {
            log.error("刷新系统配置缓存失败", e);
        }
    }

    public String getString(String key, String defaultValue) {
        SysConfig config = configCache.get(key);
        if (config == null || config.getConfigValue() == null) {
            return defaultValue;
        }
        return config.getConfigValue();
    }

    public int getInt(String key, int defaultValue) {
        String value = getString(key, null);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("配置项 {} 不是有效的整数: {}", key, value);
            return defaultValue;
        }
    }

    public long getLong(String key, long defaultValue) {
        String value = getString(key, null);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            log.warn("配置项 {} 不是有效的长整数: {}", key, value);
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getString(key, null);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value.trim()) || "1".equals(value.trim()) || "yes".equalsIgnoreCase(value.trim());
    }

    public double getDouble(String key, double defaultValue) {
        String value = getString(key, null);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            log.warn("配置项 {} 不是有效的浮点数: {}", key, value);
            return defaultValue;
        }
    }

    public List<SysConfig> listByCategory(String category) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isBlank()) {
            wrapper.eq(SysConfig::getCategory, category);
        }
        wrapper.orderByAsc(SysConfig::getConfigKey);
        return sysConfigMapper.selectList(wrapper);
    }

    public Map<String, Object> getConfigMapByCategory(String category) {
        List<SysConfig> configs = listByCategory(category);
        Map<String, Object> result = new HashMap<>();
        for (SysConfig config : configs) {
            Object value = convertToType(config.getConfigValue(), config.getConfigType());
            result.put(config.getConfigKey(), value);
        }
        return result;
    }

    private Object convertToType(String value, String type) {
        if (value == null) return null;
        try {
            switch (type != null ? type : "string") {
                case "number":
                    if (value.contains(".")) {
                        return Double.parseDouble(value.trim());
                    }
                    return Long.parseLong(value.trim());
                case "boolean":
                    return "true".equalsIgnoreCase(value.trim()) || "1".equals(value.trim());
                case "json":
                    return value;
                case "string":
                default:
                    return value;
            }
        } catch (Exception e) {
            return value;
        }
    }

    @Transactional
    public void updateConfig(String key, String value) {
        SysConfig config = sysConfigMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key)
        );
        if (config == null) {
            throw new BusinessException("配置项不存在: " + key);
        }
        if (config.getIsEditable() != null && config.getIsEditable() == 0) {
            throw new BusinessException("该配置项不允许修改: " + key);
        }
        config.setConfigValue(value);
        sysConfigMapper.updateById(config);
        configCache.put(key, config);
        log.info("系统配置已更新: {} = {}", key, value);
    }

    @Transactional
    public void batchUpdateConfigs(Map<String, String> updates) {
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            updateConfig(entry.getKey(), entry.getValue());
        }
    }

    public int getPdfRenderDpi() {
        return getInt("pdf.render.dpi", 150);
    }

    public int getPdfThumbnailDpi() {
        return getInt("pdf.thumbnail.dpi", 72);
    }

    public int getPdfPrerenderPages() {
        return getInt("pdf.prerender.pages", 10);
    }

    public boolean isPdfPrerenderEnabled() {
        return getBoolean("pdf.prerender.enabled", true);
    }

    public boolean isPdfCacheEnabled() {
        return getBoolean("pdf.cache.enabled", true);
    }

    public int getPdfCacheExpireHours() {
        return getInt("pdf.cache.expire_hours", 24);
    }

    public int getReaderPreloadOffset() {
        return getInt("reader.preload.offset", 2);
    }

    public boolean isReaderPreloadEnabled() {
        return getBoolean("reader.preload.enabled", true);
    }

    public boolean isReaderSkeletonEnabled() {
        return getBoolean("reader.skeleton.enabled", true);
    }

    public int getWeakNetworkThresholdKb() {
        return getInt("reader.weaknetwork.threshold_kb", 50);
    }
}
