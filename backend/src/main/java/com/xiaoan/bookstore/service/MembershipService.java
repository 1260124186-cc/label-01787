package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.dto.MemberUserVO;
import com.xiaoan.bookstore.dto.MembershipPlanDTO;
import com.xiaoan.bookstore.dto.OrderCreateDTO;
import com.xiaoan.bookstore.dto.QuotaVO;
import com.xiaoan.bookstore.entity.*;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.*;
import com.xiaoan.bookstore.util.WxUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private static final Logger log = LoggerFactory.getLogger(MembershipService.class);
    private final MembershipPlanMapper membershipPlanMapper;
    private final UserMembershipMapper userMembershipMapper;
    private final OrderMapper orderMapper;
    private final BookMapper bookMapper;
    private final UserMapper userMapper;
    private final WxUtil wxUtil;
    private final PointsService pointsService;

    public MembershipPlan getPlan(String planCode) {
        return membershipPlanMapper.selectOne(
                new LambdaQueryWrapper<MembershipPlan>().eq(MembershipPlan::getCode, planCode)
        );
    }

    public List<MembershipPlan> listPlans() {
        return membershipPlanMapper.selectList(
                new LambdaQueryWrapper<MembershipPlan>()
                        .eq(MembershipPlan::getStatus, Constants.STATUS_ENABLED)
                        .orderByAsc(MembershipPlan::getSortOrder)
        );
    }

    public MembershipPlan createPlan(MembershipPlanDTO dto) {
        MembershipPlan plan = new MembershipPlan();
        plan.setCode(dto.getCode());
        plan.setName(dto.getName());
        plan.setDescription(dto.getDescription());
        plan.setPrice(dto.getPrice());
        plan.setDurationDays(dto.getDurationDays());
        plan.setMaxBooks(dto.getMaxBooks());
        plan.setMaxStorage(dto.getMaxStorage());
        plan.setAiDailyLimit(dto.getAiDailyLimit());
        plan.setPriorityQueue(dto.getPriorityQueue());
        plan.setAdvancedStats(dto.getAdvancedStats());
        plan.setSortOrder(dto.getSortOrder());
        plan.setStatus(Constants.STATUS_ENABLED);
        membershipPlanMapper.insert(plan);
        return plan;
    }

    public void updatePlan(Long planId, MembershipPlanDTO dto) {
        MembershipPlan plan = membershipPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException("会员方案不存在");
        }
        plan.setCode(dto.getCode());
        plan.setName(dto.getName());
        plan.setDescription(dto.getDescription());
        plan.setPrice(dto.getPrice());
        plan.setDurationDays(dto.getDurationDays());
        plan.setMaxBooks(dto.getMaxBooks());
        plan.setMaxStorage(dto.getMaxStorage());
        plan.setAiDailyLimit(dto.getAiDailyLimit());
        plan.setPriorityQueue(dto.getPriorityQueue());
        plan.setAdvancedStats(dto.getAdvancedStats());
        plan.setSortOrder(dto.getSortOrder());
        membershipPlanMapper.updateById(plan);
    }

    public UserMembership getOrCreateUserMembership(Long userId) {
        UserMembership membership = userMembershipMapper.selectOne(
                new LambdaQueryWrapper<UserMembership>().eq(UserMembership::getUserId, userId)
        );
        if (membership == null) {
            membership = new UserMembership();
            membership.setUserId(userId);
            membership.setPlanCode(Constants.PLAN_FREE);
            membership.setExtraStorage(0L);
            membership.setAiUsedToday(0);
            membership.setAiUsageDate("");
            userMembershipMapper.insert(membership);
        }
        return membership;
    }

    public boolean isVip(Long userId) {
        UserMembership membership = getOrCreateUserMembership(userId);
        return Constants.PLAN_VIP.equals(membership.getPlanCode())
                && (membership.getExpireAt() == null || membership.getExpireAt().isAfter(LocalDateTime.now()));
    }

    public String getCurrentPlanCode(Long userId) {
        if (isVip(userId)) {
            return Constants.PLAN_VIP;
        }
        return Constants.PLAN_FREE;
    }

    public QuotaVO getQuota(Long userId) {
        UserMembership membership = getOrCreateUserMembership(userId);
        String effectivePlanCode = Constants.PLAN_VIP.equals(membership.getPlanCode())
                && (membership.getExpireAt() == null || membership.getExpireAt().isAfter(LocalDateTime.now()))
                ? Constants.PLAN_VIP : Constants.PLAN_FREE;
        MembershipPlan plan = getPlan(effectivePlanCode);

        Long currentBookCount = bookMapper.selectCount(
                new LambdaQueryWrapper<Book>().eq(Book::getUserId, userId).eq(Book::getStatus, Constants.STATUS_ENABLED)
        );

        List<Book> books = bookMapper.selectList(
                new LambdaQueryWrapper<Book>().eq(Book::getUserId, userId).eq(Book::getStatus, Constants.STATUS_ENABLED)
        );
        long usedStorage = books.stream().mapToLong(b -> b.getFileSize() != null ? b.getFileSize() : 0L).sum();

        QuotaVO vo = new QuotaVO();
        vo.setPlanCode(effectivePlanCode);
        vo.setPlanName(plan != null ? plan.getName() : "");
        vo.setMaxBooks(plan != null ? plan.getMaxBooks() : 0);
        vo.setCurrentBooks(currentBookCount != null ? currentBookCount.intValue() : 0);
        long totalStorage = (plan != null ? plan.getMaxStorage() : 0L) + (membership.getExtraStorage() != null ? membership.getExtraStorage() : 0L);
        vo.setMaxStorage(totalStorage);
        vo.setUsedStorage(usedStorage);
        vo.setAiDailyLimit(plan != null ? plan.getAiDailyLimit() : 0);
        String today = LocalDate.now().toString();
        int aiUsed = 0;
        if (today.equals(membership.getAiUsageDate()) && membership.getAiUsedToday() != null) {
            aiUsed = membership.getAiUsedToday();
        }
        vo.setAiUsedToday(aiUsed);
        vo.setPriorityQueue(plan != null && plan.getPriorityQueue() != null && plan.getPriorityQueue() == 1);
        vo.setAdvancedStats(plan != null && plan.getAdvancedStats() != null && plan.getAdvancedStats() == 1);
        vo.setIsVip(Constants.PLAN_VIP.equals(effectivePlanCode));
        vo.setExpireAt(membership.getExpireAt() != null ? membership.getExpireAt().toString() : null);
        return vo;
    }

    public void checkBookQuota(Long userId) {
        QuotaVO quota = getQuota(userId);
        if (quota.getMaxBooks() > 0 && quota.getCurrentBooks() >= quota.getMaxBooks()) {
            throw new BusinessException("书籍数量已达上限，请升级会员");
        }
    }

    public void checkStorageQuota(Long userId, long additionalBytes) {
        QuotaVO quota = getQuota(userId);
        if (quota.getUsedStorage() + additionalBytes > quota.getMaxStorage()) {
            throw new BusinessException("存储空间不足，请升级会员或购买存储包");
        }
    }

    public Map<String, Object> createOrder(Long userId, OrderCreateDTO dto) {
        MembershipPlan plan = membershipPlanMapper.selectById(dto.getPlanId());
        if (plan == null) {
            throw new BusinessException("会员方案不存在");
        }
        String orderNo = "MP" + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(10000));

        OrderEntity order = new OrderEntity();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setPlanId(plan.getId());
        order.setOrderType(dto.getOrderType());
        order.setAmount(plan.getPrice());
        order.setStatus(Constants.ORDER_STATUS_PENDING);
        order.setExpiredAt(LocalDateTime.now().plusMinutes(Constants.ORDER_EXPIRE_MINUTES));
        if (dto.getOrderType() == Constants.ORDER_TYPE_STORAGE && dto.getStorageGB() != null) {
            order.setStorageGB(dto.getStorageGB());
        }
        orderMapper.insert(order);

        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("orderId", order.getId());
        result.put("amount", plan.getPrice());

        try {
            Map<String, String> prepayResult = wxUtil.createPrepay(orderNo, plan.getPrice(), "购买" + plan.getName(), "");
            if (prepayResult != null && prepayResult.containsKey("prepayId")) {
                order.setWxPrepayId(prepayResult.get("prepayId"));
                result.put("timeStamp", prepayResult.get("timeStamp"));
                result.put("nonceStr", prepayResult.get("nonceStr"));
                result.put("package", prepayResult.get("package"));
                result.put("paySign", prepayResult.get("paySign"));
                result.put("signType", "RSA");
            } else {
                order.setWxPrepayId("");
            }
            orderMapper.updateById(order);
        } catch (Exception e) {
            log.warn("创建微信预支付失败: {}", e.getMessage());
        }

        return result;
    }

    public void handlePayCallback(String orderNo, String transactionId) {
        OrderEntity order = orderMapper.selectOne(
                new LambdaQueryWrapper<OrderEntity>().eq(OrderEntity::getOrderNo, orderNo)
        );
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != Constants.ORDER_STATUS_PENDING) {
            return;
        }
        order.setStatus(Constants.ORDER_STATUS_PAID);
        order.setWxTransactionId(transactionId);
        order.setPaidAt(LocalDateTime.now());
        orderMapper.updateById(order);

        if (Constants.ORDER_TYPE_MEMBERSHIP == order.getOrderType()) {
            MembershipPlan plan = membershipPlanMapper.selectById(order.getPlanId());
            if (plan != null) {
                activateMembership(order.getUserId(), plan);
            }
        } else if (Constants.ORDER_TYPE_STORAGE == order.getOrderType()) {
            UserMembership membership = getOrCreateUserMembership(order.getUserId());
            int gb = order.getStorageGB() != null ? order.getStorageGB() : 10;
            membership.setExtraStorage((membership.getExtraStorage() != null ? membership.getExtraStorage() : 0L) + (long) gb * Constants.BYTES_PER_GB);
            userMembershipMapper.updateById(membership);
        }
    }

    private void activateMembership(Long userId, MembershipPlan plan) {
        UserMembership membership = getOrCreateUserMembership(userId);
        LocalDateTime now = LocalDateTime.now();
        if (Constants.PLAN_VIP.equals(membership.getPlanCode()) && membership.getExpireAt() != null && membership.getExpireAt().isAfter(now)) {
            membership.setExpireAt(membership.getExpireAt().plusDays(plan.getDurationDays()));
        } else {
            membership.setPlanCode(Constants.PLAN_VIP);
            membership.setExpireAt(now.plusDays(plan.getDurationDays()));
        }
        userMembershipMapper.updateById(membership);
    }

    public Page<OrderEntity> adminOrderList(int page, int size, Integer status, String orderNo) {
        LambdaQueryWrapper<OrderEntity> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(OrderEntity::getStatus, status);
        }
        if (orderNo != null && !orderNo.isEmpty()) {
            wrapper.like(OrderEntity::getOrderNo, orderNo);
        }
        wrapper.orderByDesc(OrderEntity::getCreatedAt);
        return orderMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Page<MemberUserVO> adminMemberList(int page, int size, String keyword) {
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            userWrapper.like(User::getNickname, keyword);
        }
        userWrapper.orderByDesc(User::getCreatedAt);
        Page<User> userPage = userMapper.selectPage(new Page<>(page, size), userWrapper);

        Page<MemberUserVO> result = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        List<MemberUserVO> voList = new ArrayList<>();
        for (User user : userPage.getRecords()) {
            MemberUserVO vo = new MemberUserVO();
            vo.setUserId(user.getId());
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());

            UserMembership membership = userMembershipMapper.selectOne(
                    new LambdaQueryWrapper<UserMembership>().eq(UserMembership::getUserId, user.getId())
            );
            if (membership != null) {
                vo.setPlanCode(membership.getPlanCode());
                MembershipPlan plan = getPlan(membership.getPlanCode());
                vo.setPlanName(plan != null ? plan.getName() : "");
                vo.setExpireAt(membership.getExpireAt() != null ? membership.getExpireAt().toString() : null);
                vo.setExtraStorage(membership.getExtraStorage());
            } else {
                vo.setPlanCode(Constants.PLAN_FREE);
                vo.setPlanName("免费版");
            }
            PointsAccount pointsAccount = pointsService.getOrCreateAccount(user.getId());
            vo.setPointsBalance(pointsAccount.getBalance());
            vo.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
            voList.add(vo);
        }
        result.setRecords(voList);
        return result;
    }

    public void cancelExpiredOrders() {
        List<OrderEntity> expiredOrders = orderMapper.selectList(
                new LambdaQueryWrapper<OrderEntity>()
                        .eq(OrderEntity::getStatus, Constants.ORDER_STATUS_PENDING)
                        .lt(OrderEntity::getExpiredAt, LocalDateTime.now())
        );
        for (OrderEntity order : expiredOrders) {
            order.setStatus(Constants.ORDER_STATUS_CANCELLED);
            orderMapper.updateById(order);
        }
    }

    public void checkAiQuota(Long userId) {
        QuotaVO quota = getQuota(userId);
        if (quota.getAiDailyLimit() > 0 && quota.getAiUsedToday() >= quota.getAiDailyLimit()) {
            throw new BusinessException("今日AI使用次数已达上限，请升级会员");
        }
    }

    public void incrementAiUsage(Long userId) {
        UserMembership membership = getOrCreateUserMembership(userId);
        String today = LocalDate.now().toString();
        if (today.equals(membership.getAiUsageDate()) && membership.getAiUsedToday() != null) {
            membership.setAiUsedToday(membership.getAiUsedToday() + 1);
        } else {
            membership.setAiUsedToday(1);
            membership.setAiUsageDate(today);
        }
        userMembershipMapper.updateById(membership);
    }
}
