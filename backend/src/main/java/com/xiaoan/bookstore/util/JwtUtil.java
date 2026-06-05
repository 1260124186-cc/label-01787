package com.xiaoan.bookstore.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${app.jwt.admin.secret}")
    private String adminSecret;

    @Value("${app.jwt.admin.expiration}")
    private long adminExpiration;

    @Value("${app.jwt.admin.issuer}")
    private String adminIssuer;

    @Value("${app.jwt.admin.audience}")
    private String adminAudience;

    @Value("${app.jwt.mp.secret}")
    private String mpSecret;

    @Value("${app.jwt.mp.expiration}")
    private long mpExpiration;

    @Value("${app.jwt.mp.issuer}")
    private String mpIssuer;

    @Value("${app.jwt.mp.audience}")
    private String mpAudience;

    private SecretKey getAdminSigningKey() {
        return Keys.hmacShaKeyFor(adminSecret.getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey getMpSigningKey() {
        return Keys.hmacShaKeyFor(mpSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAdminToken(Long userId, Long roleId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("userType", 1);
        claims.put("roleId", roleId);

        return Jwts.builder()
                .claims(claims)
                .issuer(adminIssuer)
                .audience().add(adminAudience).and()
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + adminExpiration))
                .signWith(getAdminSigningKey())
                .compact();
    }

    public String generateMpToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("userType", 2);

        return Jwts.builder()
                .claims(claims)
                .issuer(mpIssuer)
                .audience().add(mpAudience).and()
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + mpExpiration))
                .signWith(getMpSigningKey())
                .compact();
    }

    public Claims parseAdminToken(String token) {
        return Jwts.parser()
                .verifyWith(getAdminSigningKey())
                .requireIssuer(adminIssuer)
                .requireAudience(adminAudience)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Claims parseMpToken(String token) {
        return Jwts.parser()
                .verifyWith(getMpSigningKey())
                .requireIssuer(mpIssuer)
                .requireAudience(mpAudience)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(Claims claims) {
        return claims.get("userId", Long.class);
    }

    public Integer getUserType(Claims claims) {
        return claims.get("userType", Integer.class);
    }

    public Long getRoleId(Claims claims) {
        return claims.get("roleId", Long.class);
    }

    public boolean isAdminTokenValid(String token) {
        try {
            Claims claims = parseAdminToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isMpTokenValid(String token) {
        try {
            Claims claims = parseMpToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    @Deprecated
    public String generateToken(Long userId, int userType) {
        if (userType == 1) {
            return generateAdminToken(userId, null);
        }
        return generateMpToken(userId);
    }

    @Deprecated
    public Claims parseToken(String token) {
        try {
            return parseAdminToken(token);
        } catch (Exception e) {
            return parseMpToken(token);
        }
    }

    @Deprecated
    public Long getUserId(String token) {
        return getUserId(parseToken(token));
    }

    @Deprecated
    public Integer getUserType(String token) {
        return getUserType(parseToken(token));
    }

    @Deprecated
    public boolean isValid(String token) {
        return isAdminTokenValid(token) || isMpTokenValid(token);
    }
}
