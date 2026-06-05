package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoan.bookstore.entity.SensitiveConfirmToken;
import com.xiaoan.bookstore.mapper.SensitiveConfirmTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SensitiveOperationService {

    private final SensitiveConfirmTokenMapper confirmTokenMapper;

    private static final int TOKEN_VALIDITY_MINUTES = 5;

    public String generateConfirmToken(Long adminId, String operation) {
        SensitiveConfirmToken token = new SensitiveConfirmToken();
        token.setAdminId(adminId);
        token.setToken(UUID.randomUUID().toString().replace("-", ""));
        token.setOperation(operation);
        token.setExpiredAt(LocalDateTime.now().plusMinutes(TOKEN_VALIDITY_MINUTES));
        token.setUsed(0);
        confirmTokenMapper.insert(token);
        return token.getToken();
    }

    public boolean validateAndConsumeConfirmToken(Long adminId, String operation, String confirmToken) {
        if (confirmToken == null || confirmToken.isEmpty()) return false;
        SensitiveConfirmToken token = confirmTokenMapper.selectOne(
                new LambdaQueryWrapper<SensitiveConfirmToken>()
                        .eq(SensitiveConfirmToken::getToken, confirmToken)
                        .eq(SensitiveConfirmToken::getAdminId, adminId)
                        .eq(SensitiveConfirmToken::getOperation, operation)
                        .eq(SensitiveConfirmToken::getUsed, 0)
                        .gt(SensitiveConfirmToken::getExpiredAt, LocalDateTime.now())
        );
        if (token == null) return false;
        token.setUsed(1);
        confirmTokenMapper.updateById(token);
        return true;
    }
}
