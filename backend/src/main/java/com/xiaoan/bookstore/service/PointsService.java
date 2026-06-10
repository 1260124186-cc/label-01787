package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.dto.PointsVO;
import com.xiaoan.bookstore.entity.*;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointsService {

    private static final Logger log = LoggerFactory.getLogger(PointsService.class);

    private final PointsAccountMapper pointsAccountMapper;
    private final PointsRecordMapper pointsRecordMapper;
    private final PointsRuleMapper pointsRuleMapper;
    private final PointsExchangeMapper pointsExchangeMapper;
    private final UserMembershipMapper userMembershipMapper;
    private final MembershipPlanMapper membershipPlanMapper;

    public PointsAccount getOrCreateAccount(Long userId) {
        LambdaQueryWrapper<PointsAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsAccount::getUserId, userId);
        PointsAccount account = pointsAccountMapper.selectOne(wrapper);
        if (account == null) {
            account = new PointsAccount();
            account.setUserId(userId);
            account.setBalance(0);
            account.setTotalEarned(0);
            account.setTotalConsumed(0);
            pointsAccountMapper.insert(account);
        }
        return account;
    }

    public PointsVO getPointsInfo(Long userId) {
        PointsAccount account = getOrCreateAccount(userId);
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        LambdaQueryWrapper<PointsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsRecord::getUserId, userId)
                .eq(PointsRecord::getCategory, Constants.POINTS_CATEGORY_DAILY_CHECKIN)
                .ge(PointsRecord::getCreatedAt, todayStart)
                .le(PointsRecord::getCreatedAt, todayEnd);
        Long checkInCount = pointsRecordMapper.selectCount(wrapper);
        PointsVO vo = new PointsVO();
        vo.setBalance(account.getBalance());
        vo.setTotalEarned(account.getTotalEarned());
        vo.setTotalConsumed(account.getTotalConsumed());
        vo.setTodayCheckedIn(checkInCount != null && checkInCount > 0);
        return vo;
    }

    public boolean dailyCheckIn(Long userId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        LambdaQueryWrapper<PointsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsRecord::getUserId, userId)
                .eq(PointsRecord::getCategory, Constants.POINTS_CATEGORY_DAILY_CHECKIN)
                .ge(PointsRecord::getCreatedAt, todayStart)
                .le(PointsRecord::getCreatedAt, todayEnd);
        Long count = pointsRecordMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            return false;
        }
        LambdaQueryWrapper<PointsRule> ruleWrapper = new LambdaQueryWrapper<>();
        ruleWrapper.eq(PointsRule::getCode, Constants.POINTS_CATEGORY_DAILY_CHECKIN);
        PointsRule rule = pointsRuleMapper.selectOne(ruleWrapper);
        if (rule == null || rule.getStatus() != Constants.STATUS_ENABLED) {
            throw new BusinessException("签到功能暂未开放");
        }
        earnPoints(userId, rule.getCategory(), rule.getPoints(), "每日打卡", "");
        return true;
    }

    public void earnPoints(Long userId, String category, int points, String description, String refId) {
        LambdaQueryWrapper<PointsRule> ruleWrapper = new LambdaQueryWrapper<>();
        ruleWrapper.eq(PointsRule::getCategory, category);
        PointsRule rule = pointsRuleMapper.selectOne(ruleWrapper);
        if (rule != null && rule.getDailyLimit() != null && rule.getDailyLimit() > 0) {
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
            LambdaQueryWrapper<PointsRecord> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.eq(PointsRecord::getUserId, userId)
                    .eq(PointsRecord::getCategory, category)
                    .ge(PointsRecord::getCreatedAt, todayStart)
                    .le(PointsRecord::getCreatedAt, todayEnd);
            Long todayCount = pointsRecordMapper.selectCount(countWrapper);
            if (todayCount != null && todayCount >= rule.getDailyLimit()) {
                throw new BusinessException("今日该类积分已达上限");
            }
            if (rule.getPoints() != null && rule.getPoints() != points) {
                points = rule.getPoints();
            }
        }
        PointsAccount account = getOrCreateAccount(userId);
        account.setBalance(account.getBalance() + points);
        account.setTotalEarned(account.getTotalEarned() + points);
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setType(Constants.POINTS_TYPE_EARN);
        record.setCategory(category);
        record.setPoints(points);
        record.setBalanceAfter(account.getBalance());
        record.setDescription(description);
        record.setRefId(refId);
        pointsRecordMapper.insert(record);
        pointsAccountMapper.updateById(account);
    }

    public void consumePoints(Long userId, String category, int points, String description, String refId) {
        PointsAccount account = getOrCreateAccount(userId);
        if (account.getBalance() < points) {
            throw new BusinessException("积分不足");
        }
        account.setBalance(account.getBalance() - points);
        account.setTotalConsumed(account.getTotalConsumed() + points);
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setType(Constants.POINTS_TYPE_CONSUME);
        record.setCategory(category);
        record.setPoints(points);
        record.setBalanceAfter(account.getBalance());
        record.setDescription(description);
        record.setRefId(refId);
        pointsRecordMapper.insert(record);
        pointsAccountMapper.updateById(account);
    }

    public void exchangeVipDays(Long userId, int days) {
        int cost = days * 100;
        PointsAccount account = getOrCreateAccount(userId);
        if (account.getBalance() < cost) {
            throw new BusinessException("积分不足");
        }
        consumePoints(userId, Constants.POINTS_CATEGORY_EXCHANGE_VIP, cost, "兑换VIP " + days + "天", "");
        UserMembership membership = getOrCreateMembership(userId);
        LocalDateTime now = LocalDateTime.now();
        if (Constants.PLAN_VIP.equals(membership.getPlanCode()) && membership.getExpireAt() != null && membership.getExpireAt().isAfter(now)) {
            membership.setExpireAt(membership.getExpireAt().plusDays(days));
        } else {
            membership.setPlanCode(Constants.PLAN_VIP);
            membership.setExpireAt(now.plusDays(days));
        }
        userMembershipMapper.updateById(membership);
        PointsExchange exchange = new PointsExchange();
        exchange.setUserId(userId);
        exchange.setExchangeType(Constants.EXCHANGE_TYPE_VIP_DAYS);
        exchange.setPointsCost(cost);
        exchange.setValue(days);
        pointsExchangeMapper.insert(exchange);
    }

    public void exchangeStorage(Long userId, int mb) {
        int cost = mb * 200;
        consumePoints(userId, Constants.POINTS_CATEGORY_EXCHANGE_STORAGE, cost, "兑换存储 " + mb + "GB", "");
        UserMembership membership = getOrCreateMembership(userId);
        long bytesToAdd = (long) mb * Constants.BYTES_PER_GB;
        membership.setExtraStorage(membership.getExtraStorage() != null ? membership.getExtraStorage() + bytesToAdd : bytesToAdd);
        userMembershipMapper.updateById(membership);
        PointsExchange exchange = new PointsExchange();
        exchange.setUserId(userId);
        exchange.setExchangeType(Constants.EXCHANGE_TYPE_STORAGE);
        exchange.setPointsCost(cost);
        exchange.setValue(mb);
        pointsExchangeMapper.insert(exchange);
    }

    public Page<PointsRecord> pointsHistory(Long userId, int page, int size) {
        LambdaQueryWrapper<PointsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsRecord::getUserId, userId)
                .orderByDesc(PointsRecord::getCreatedAt);
        return pointsRecordMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public List<PointsRule> listRules() {
        LambdaQueryWrapper<PointsRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsRule::getStatus, Constants.STATUS_ENABLED)
                .orderByAsc(PointsRule::getId);
        return pointsRuleMapper.selectList(wrapper);
    }

    public void updateRule(Long ruleId, int points, int dailyLimit) {
        PointsRule rule = pointsRuleMapper.selectById(ruleId);
        if (rule == null) {
            throw new BusinessException("积分规则不存在");
        }
        rule.setPoints(points);
        rule.setDailyLimit(dailyLimit);
        pointsRuleMapper.updateById(rule);
    }

    public void adminAdjustPoints(Long userId, int points, String description) {
        if (points > 0) {
            earnPoints(userId, Constants.POINTS_CATEGORY_ADMIN_ADJUST, points, description, "");
        } else if (points < 0) {
            consumePoints(userId, Constants.POINTS_CATEGORY_ADMIN_ADJUST, Math.abs(points), description, "");
        }
    }

    private UserMembership getOrCreateMembership(Long userId) {
        LambdaQueryWrapper<UserMembership> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserMembership::getUserId, userId);
        UserMembership membership = userMembershipMapper.selectOne(wrapper);
        if (membership == null) {
            membership = new UserMembership();
            membership.setUserId(userId);
            membership.setPlanCode(Constants.PLAN_FREE);
            membership.setAutoRenew(0);
            membership.setExtraStorage(0L);
            userMembershipMapper.insert(membership);
        }
        return membership;
    }
}
