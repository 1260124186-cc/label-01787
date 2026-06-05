package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoan.bookstore.entity.FileDownloadLog;
import com.xiaoan.bookstore.mapper.FileDownloadLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FileDownloadLogService {

    private final FileDownloadLogMapper downloadLogMapper;

    private static final int MAX_DOWNLOADS_PER_TOKEN_PER_HOUR = 10;
    private static final int MAX_DOWNLOADS_PER_USER_PER_DAY = 100;

    public void logDownload(FileDownloadLog log) {
        downloadLogMapper.insert(log);
    }

    public boolean isTokenLimitExceeded(String fileToken) {
        LocalDateTime since = LocalDateTime.now().minusHours(1);
        return downloadLogMapper.countByTokenSince(fileToken, since) >= MAX_DOWNLOADS_PER_TOKEN_PER_HOUR;
    }

    public boolean isUserLimitExceeded(Long userId, Integer userType) {
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        return downloadLogMapper.countByUserSince(userId, userType, since) >= MAX_DOWNLOADS_PER_USER_PER_DAY;
    }
}
