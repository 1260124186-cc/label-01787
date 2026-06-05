package com.xiaoan.bookstore.controller;

import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.entity.FileDownloadLog;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.service.FileDownloadLogService;
import com.xiaoan.bookstore.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileTokenController {

    private final FileDownloadLogService downloadLogService;
    private final JwtUtil jwtUtil;

    @Value("${app.jwt.mp.secret}")
    private String signingSecret;

    @Value("${app.upload.path}")
    private String uploadPath;

    @Value("${app.file.signed-url-expiration:300}")
    private int signedUrlExpiration;

    @Value("${app.file.max-downloads-per-hour:10}")
    private int maxDownloadsPerHour;

    @Value("${app.file.max-downloads-per-day:100}")
    private int maxDownloadsPerDay;

    private static final String HMAC_ALGO = "HmacSHA256";

    @PostMapping("/signed-url")
    public Result<Map<String, Object>> generateSignedUrl(
            @RequestBody Map<String, String> body) {

        Long userId = TenantContext.getTenantId();
        Integer userType = TenantContext.getUserType();

        String filePath = body.get("path");
        if (filePath == null || filePath.isEmpty()) {
            throw new BusinessException("文件路径不能为空");
        }

        if (filePath.contains("..") || filePath.startsWith("/")) {
            throw new BusinessException("非法文件路径");
        }

        if (downloadLogService.isUserLimitExceeded(userId, userType)) {
            throw new BusinessException("今日下载次数已达上限");
        }

        long expiresAt = System.currentTimeMillis() / 1000 + signedUrlExpiration;
        String data = filePath + "|" + userId + "|" + userType + "|" + expiresAt;
        String signature = hmacSha256(data);

        Map<String, Object> result = new HashMap<>();
        result.put("url", "/api/file/download?path=" + urlEncode(filePath)
                + "&uid=" + userId
                + "&ut=" + userType
                + "&expires=" + expiresAt
                + "&sig=" + urlEncode(signature));
        result.put("expiresIn", signedUrlExpiration);
        return Result.success(result);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(
            @RequestParam String path,
            @RequestParam Long uid,
            @RequestParam Integer ut,
            @RequestParam Long expires,
            @RequestParam String sig,
            HttpServletRequest request) {

        if (System.currentTimeMillis() / 1000 > expires) {
            throw new BusinessException(410, "下载链接已过期");
        }

        String data = path + "|" + uid + "|" + ut + "|" + expires;
        String expectedSig = hmacSha256(data);
        if (!expectedSig.equals(sig)) {
            throw new BusinessException(403, "签名验证失败");
        }

        if (path.contains("..") || path.startsWith("/")) {
            throw new BusinessException("非法文件路径");
        }

        String referer = request.getHeader("Referer");
        if (referer != null && !isRefererAllowed(referer)) {
            throw new BusinessException(403, "防盗链: 来源不受信任");
        }

        String fileToken = sig.substring(0, Math.min(sig.length(), 16)) + expires;
        if (downloadLogService.isTokenLimitExceeded(fileToken)) {
            throw new BusinessException("该链接下载次数已达上限");
        }

        Path realPath = Paths.get(uploadPath).resolve(path).normalize();
        if (!realPath.startsWith(Paths.get(uploadPath).normalize())) {
            throw new BusinessException("非法文件路径");
        }

        Resource resource = new FileSystemResource(realPath);
        if (!resource.exists()) {
            throw new BusinessException("文件不存在");
        }

        logDownload(uid, ut, fileToken, path, request);

        String contentType = guessContentType(path);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + realPath.getFileName() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(resource);
    }

    private void logDownload(Long userId, Integer userType, String fileToken, String filePath, HttpServletRequest request) {
        FileDownloadLog log = new FileDownloadLog();
        log.setUserId(userId);
        log.setUserType(userType);
        log.setFileToken(fileToken);
        log.setFilePath(filePath);
        log.setIp(getClientIp(request));
        log.setReferer(request.getHeader("Referer"));
        log.setUserAgent(request.getHeader("User-Agent"));
        downloadLogService.logDownload(log);
    }

    private String hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC签名失败", e);
        }
    }

    private boolean isRefererAllowed(String referer) {
        return referer.contains("localhost")
                || referer.contains("127.0.0.1")
                || referer.contains("xiaoan-bookstore");
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }

    private String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private String guessContentType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }
}
