package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.common.TenantValidator;
import com.xiaoan.bookstore.dto.*;
import com.xiaoan.bookstore.entity.*;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private static final Logger log = LoggerFactory.getLogger(GroupService.class);
    private static final String INVITE_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int INVITE_CODE_LENGTH = 6;

    private final BookGroupMapper bookGroupMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final GroupReadingPlanMapper groupReadingPlanMapper;
    private final GroupPlanMemberMapper groupPlanMemberMapper;
    private final GroupDynamicMapper groupDynamicMapper;
    private final GroupDynamicLikeMapper groupDynamicLikeMapper;
    private final UserMapper userMapper;
    private final BookMapper bookMapper;
    private final ReadingRecordMapper readingRecordMapper;

    private String generateInviteCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            sb.append(INVITE_CODE_CHARS.charAt(random.nextInt(INVITE_CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            code = generateInviteCode();
        } while (bookGroupMapper.selectCount(new LambdaQueryWrapper<BookGroup>()
                .eq(BookGroup::getInviteCode, code)) > 0);
        return code;
    }

    @Transactional
    public BookGroup createGroup(Long userId, GroupCreateDTO dto) {
        BookGroup group = new BookGroup();
        group.setName(dto.getName());
        group.setDescription(dto.getDescription() != null ? dto.getDescription() : "");
        group.setInviteCode(generateUniqueInviteCode());
        group.setCreatorId(userId);
        group.setMemberCount(1);
        group.setStatus(1);
        bookGroupMapper.insert(group);

        GroupMember member = new GroupMember();
        member.setGroupId(group.getId());
        member.setUserId(userId);
        member.setRole(1);
        member.setReadingPublic(0);
        member.setJoinedAt(LocalDateTime.now());
        groupMemberMapper.insert(member);

        log.info("创建小组: userId={}, groupId={}, groupName={}", userId, group.getId(), group.getName());
        return group;
    }

    @Transactional
    public GroupMember joinGroup(Long userId, GroupJoinDTO dto) {
        BookGroup group = bookGroupMapper.selectOne(new LambdaQueryWrapper<BookGroup>()
                .eq(BookGroup::getInviteCode, dto.getInviteCode()));
        if (group == null) {
            throw new BusinessException("邀请码无效");
        }
        if (group.getStatus() == 0) {
            throw new BusinessException("该小组已被封禁");
        }

        Long count = groupMemberMapper.selectCount(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, group.getId())
                .eq(GroupMember::getUserId, userId));
        if (count > 0) {
            throw new BusinessException("您已加入该小组");
        }

        GroupMember member = new GroupMember();
        member.setGroupId(group.getId());
        member.setUserId(userId);
        member.setRole(2);
        member.setReadingPublic(0);
        member.setJoinedAt(LocalDateTime.now());
        groupMemberMapper.insert(member);

        group.setMemberCount(group.getMemberCount() + 1);
        bookGroupMapper.updateById(group);

        log.info("加入小组: userId={}, groupId={}", userId, group.getId());
        return member;
    }

    @Transactional
    public void leaveGroup(Long userId, Long groupId) {
        GroupMember member = groupMemberMapper.selectOne(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException("您不是该小组成员");
        }

        BookGroup group = bookGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException("小组不存在");
        }

        if (member.getRole() == 1 && group.getMemberCount() > 1) {
            throw new BusinessException("组长不能退出小组，请先转让组长");
        }

        groupMemberMapper.deleteById(member.getId());
        group.setMemberCount(Math.max(0, group.getMemberCount() - 1));
        bookGroupMapper.updateById(group);

        log.info("退出小组: userId={}, groupId={}", userId, groupId);
    }

    public List<BookGroup> myGroups(Long userId) {
        return bookGroupMapper.selectGroupsByUserId(userId);
    }

    public GroupVO getGroupDetail(Long userId, Long groupId) {
        BookGroup group = bookGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException("小组不存在");
        }

        GroupMember myMember = groupMemberMapper.selectOne(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId));
        if (myMember == null) {
            throw new BusinessException("您不是该小组成员");
        }

        User creator = userMapper.selectById(group.getCreatorId());

        GroupVO vo = new GroupVO();
        vo.setId(group.getId());
        vo.setName(group.getName());
        vo.setDescription(group.getDescription());
        vo.setInviteCode(myMember != null ? group.getInviteCode() : null);
        vo.setCreatorId(group.getCreatorId());
        vo.setCreatorNickname(creator != null ? creator.getNickname() : "");
        vo.setCreatorAvatar(creator != null ? creator.getAvatar() : "");
        vo.setMemberCount(group.getMemberCount());
        vo.setStatus(group.getStatus());
        vo.setMyRole(myMember != null ? myMember.getRole() : null);
        vo.setMyReadingPublic(myMember != null ? myMember.getReadingPublic() : null);
        vo.setCreatedAt(group.getCreatedAt());

        List<Map<String, Object>> memberMaps = groupMemberMapper.selectMembersWithUserInfo(groupId);
        List<GroupMemberVO> memberVOS = memberMaps.stream().map(m -> {
            GroupMemberVO mvo = new GroupMemberVO();
            mvo.setUserId(((Number) m.get("user_id")).longValue());
            mvo.setNickname((String) m.get("nickname"));
            mvo.setAvatar((String) m.get("avatar"));
            mvo.setRole(((Number) m.get("role")).intValue());
            mvo.setReadingPublic(((Number) m.get("reading_public")).intValue());
            mvo.setJoinedAt((LocalDateTime) m.get("joined_at"));
            return mvo;
        }).collect(Collectors.toList());
        vo.setMembers(memberVOS);

        return vo;
    }

    public void updateMemberSetting(Long userId, Long groupId, GroupMemberUpdateDTO dto) {
        GroupMember member = groupMemberMapper.selectOne(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException("您不是该小组成员");
        }
        member.setReadingPublic(dto.getReadingPublic());
        groupMemberMapper.updateById(member);
    }

    public List<GroupRankVO> getWeekRank(Long userId, Long groupId) {
        GroupMember member = groupMemberMapper.selectOne(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException("您不是该小组成员");
        }

        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDateTime start = weekStart.atStartOfDay();
        LocalDateTime end = weekStart.plusDays(7).atStartOfDay();

        List<Map<String, Object>> rankList = groupMemberMapper.selectWeekRank(groupId, start, end);

        List<GroupRankVO> result = new ArrayList<>();
        int rank = 1;
        for (Map<String, Object> m : rankList) {
            GroupRankVO vo = new GroupRankVO();
            vo.setUserId(((Number) m.get("user_id")).longValue());
            vo.setNickname((String) m.get("nickname"));
            vo.setAvatar((String) m.get("avatar"));
            Long duration = m.get("total_duration") != null ? ((Number) m.get("total_duration")).longValue() : 0L;
            vo.setTotalDuration(duration);
            vo.setFormattedDuration(formatDuration(duration));

            Integer readingPublic = m.get("reading_public") != null ? ((Number) m.get("reading_public")).intValue() : 0;
            if (readingPublic == 1 && m.get("book_title") != null) {
                vo.setBookTitle((String) m.get("book_title"));
            }

            vo.setRank(rank++);
            result.add(vo);
        }
        return result;
    }

    private String formatDuration(long seconds) {
        if (seconds <= 0) return "0分钟";
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours > 0) {
            return hours + "小时" + minutes + "分钟";
        } else {
            return minutes + "分钟";
        }
    }

    @Transactional
    public GroupReadingPlan createPlan(Long userId, Long groupId, GroupPlanCreateDTO dto) {
        GroupMember member = groupMemberMapper.selectOne(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException("您不是该小组成员");
        }
        if (member.getRole() != 1) {
            throw new BusinessException("只有组长可以创建共读计划");
        }

        GroupReadingPlan plan = new GroupReadingPlan();
        plan.setGroupId(groupId);
        plan.setBookTitle(dto.getBookTitle());
        plan.setBookAuthor(dto.getBookAuthor() != null ? dto.getBookAuthor() : "");
        plan.setCreatorId(userId);
        plan.setStartDate(dto.getStartDate());
        plan.setEndDate(dto.getEndDate());
        plan.setDescription(dto.getDescription() != null ? dto.getDescription() : "");
        plan.setStatus(1);
        groupReadingPlanMapper.insert(plan);

        log.info("创建共读计划: userId={}, groupId={}, planId={}", userId, groupId, plan.getId());
        return plan;
    }

    public List<GroupReadingPlan> listPlans(Long userId, Long groupId) {
        GroupMember member = groupMemberMapper.selectOne(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException("您不是该小组成员");
        }
        return groupReadingPlanMapper.selectByGroupId(groupId);
    }

    public GroupPlanVO getPlanDetail(Long userId, Long planId) {
        GroupReadingPlan plan = groupReadingPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException("共读计划不存在");
        }

        GroupMember member = groupMemberMapper.selectOne(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, plan.getGroupId())
                .eq(GroupMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException("您不是该小组成员");
        }

        User creator = userMapper.selectById(plan.getCreatorId());

        GroupPlanVO vo = new GroupPlanVO();
        vo.setId(plan.getId());
        vo.setGroupId(plan.getGroupId());
        vo.setBookTitle(plan.getBookTitle());
        vo.setBookAuthor(plan.getBookAuthor());
        vo.setCreatorId(plan.getCreatorId());
        vo.setCreatorNickname(creator != null ? creator.getNickname() : "");
        vo.setStartDate(plan.getStartDate());
        vo.setEndDate(plan.getEndDate());
        vo.setDescription(plan.getDescription());
        vo.setStatus(plan.getStatus());
        vo.setCreatedAt(plan.getCreatedAt());

        List<Map<String, Object>> memberMaps = groupPlanMemberMapper.selectByPlanIdWithUserInfo(planId);
        List<GroupPlanMemberVO> memberVOS = memberMaps.stream().map(m -> {
            GroupPlanMemberVO mvo = new GroupPlanMemberVO();
            mvo.setUserId(((Number) m.get("user_id")).longValue());
            mvo.setNickname((String) m.get("nickname"));
            mvo.setAvatar((String) m.get("avatar"));
            if (m.get("book_id") != null) {
                mvo.setBookId(((Number) m.get("book_id")).longValue());
            }
            Integer duration = m.get("total_duration") != null ? ((Number) m.get("total_duration")).intValue() : 0;
            mvo.setTotalDuration(duration);
            mvo.setFormattedDuration(formatDuration(duration));
            mvo.setLastReadAt((LocalDateTime) m.get("last_read_at"));
            mvo.setJoinedAt((LocalDateTime) m.get("joined_at"));
            return mvo;
        }).collect(Collectors.toList());
        vo.setMembers(memberVOS);

        return vo;
    }

    @Transactional
    public GroupPlanMember joinPlan(Long userId, Long planId, GroupPlanJoinDTO dto) {
        GroupReadingPlan plan = groupReadingPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException("共读计划不存在");
        }

        GroupMember member = groupMemberMapper.selectOne(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, plan.getGroupId())
                .eq(GroupMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException("您不是该小组成员");
        }

        Book book = bookMapper.selectById(dto.getBookId());
        if (book == null) {
            throw new BusinessException("书籍不存在");
        }
        TenantValidator.validateCrossTenant(book.getUserId(), userId);

        if (!book.getTitle().equalsIgnoreCase(plan.getBookTitle())) {
            throw new BusinessException("请上传与共读书名《" + plan.getBookTitle() + "》一致的书籍");
        }

        GroupPlanMember existing = groupPlanMemberMapper.selectByPlanAndUser(planId, userId);
        if (existing != null) {
            throw new BusinessException("您已参与该共读计划");
        }

        GroupPlanMember planMember = new GroupPlanMember();
        planMember.setPlanId(planId);
        planMember.setUserId(userId);
        planMember.setBookId(dto.getBookId());
        planMember.setTotalDuration(0);
        planMember.setJoinedAt(LocalDateTime.now());
        groupPlanMemberMapper.insert(planMember);

        log.info("加入共读计划: userId={}, planId={}", userId, planId);
        return planMember;
    }

    @Transactional
    public GroupDynamic createDynamic(Long userId, Long groupId, GroupDynamicCreateDTO dto) {
        GroupMember member = groupMemberMapper.selectOne(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException("您不是该小组成员");
        }

        if (dto.getType() != 1 && dto.getType() != 2) {
            throw new BusinessException("无效的动态类型");
        }

        GroupDynamic dynamic = new GroupDynamic();
        dynamic.setGroupId(groupId);
        dynamic.setUserId(userId);
        dynamic.setType(dto.getType());
        dynamic.setContent(dto.getContent() != null ? dto.getContent() : "");
        dynamic.setBookTitle(dto.getBookTitle() != null ? dto.getBookTitle() : "");
        dynamic.setExcerptText(dto.getExcerptText() != null ? dto.getExcerptText() : "");
        dynamic.setDuration(dto.getDuration() != null ? dto.getDuration() : 0);
        dynamic.setLikes(0);
        groupDynamicMapper.insert(dynamic);

        log.info("发布小组动态: userId={}, groupId={}, dynamicId={}, type={}", userId, groupId, dynamic.getId(), dto.getType());
        return dynamic;
    }

    public IPage<GroupDynamicVO> listDynamics(Long userId, Long groupId, int page, int size) {
        GroupMember member = groupMemberMapper.selectOne(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException("您不是该小组成员");
        }

        Page<GroupDynamic> pageParam = new Page<>(page, size);
        IPage<Map<String, Object>> pageResult = groupDynamicMapper.selectByGroupIdWithUserInfo(pageParam, groupId);

        List<Long> dynamicIds = pageResult.getRecords().stream()
                .map(m -> ((Number) m.get("id")).longValue())
                .collect(Collectors.toList());

        Set<Long> likedIds = new HashSet<>();
        if (!dynamicIds.isEmpty()) {
            String idsStr = dynamicIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            likedIds = new HashSet<>(groupDynamicLikeMapper.selectLikedDynamicIds(userId, idsStr));
        }

        final Set<Long> finalLikedIds = likedIds;
        List<GroupDynamicVO> vos = pageResult.getRecords().stream().map(m -> {
            GroupDynamicVO vo = new GroupDynamicVO();
            vo.setId(((Number) m.get("id")).longValue());
            vo.setGroupId(((Number) m.get("group_id")).longValue());
            vo.setUserId(((Number) m.get("user_id")).longValue());
            vo.setNickname((String) m.get("nickname"));
            vo.setAvatar((String) m.get("avatar"));
            vo.setType(((Number) m.get("type")).intValue());
            vo.setContent((String) m.get("content"));
            vo.setBookTitle((String) m.get("book_title"));
            vo.setExcerptText((String) m.get("excerpt_text"));
            vo.setDuration(m.get("duration") != null ? ((Number) m.get("duration")).intValue() : 0);
            vo.setFormattedDuration(formatDuration(vo.getDuration()));
            vo.setLikes(m.get("likes") != null ? ((Number) m.get("likes")).intValue() : 0);
            vo.setLiked(finalLikedIds.contains(vo.getId()));
            vo.setCreatedAt((LocalDateTime) m.get("created_at"));
            return vo;
        }).collect(Collectors.toList());

        IPage<GroupDynamicVO> result = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
        result.setRecords(vos);
        return result;
    }

    @Transactional
    public void toggleLike(Long userId, Long dynamicId) {
        GroupDynamic dynamic = groupDynamicMapper.selectById(dynamicId);
        if (dynamic == null) {
            throw new BusinessException("动态不存在");
        }

        GroupMember member = groupMemberMapper.selectOne(new LambdaQueryWrapper<GroupMember>()
                .eq(GroupMember::getGroupId, dynamic.getGroupId())
                .eq(GroupMember::getUserId, userId));
        if (member == null) {
            throw new BusinessException("您不是该小组成员");
        }

        Long count = groupDynamicLikeMapper.selectCount(new LambdaQueryWrapper<GroupDynamicLike>()
                .eq(GroupDynamicLike::getDynamicId, dynamicId)
                .eq(GroupDynamicLike::getUserId, userId));

        if (count > 0) {
            groupDynamicLikeMapper.delete(new LambdaQueryWrapper<GroupDynamicLike>()
                    .eq(GroupDynamicLike::getDynamicId, dynamicId)
                    .eq(GroupDynamicLike::getUserId, userId));
            dynamic.setLikes(Math.max(0, dynamic.getLikes() - 1));
        } else {
            GroupDynamicLike like = new GroupDynamicLike();
            like.setDynamicId(dynamicId);
            like.setUserId(userId);
            groupDynamicLikeMapper.insert(like);
            dynamic.setLikes(dynamic.getLikes() + 1);
        }
        groupDynamicMapper.updateById(dynamic);
    }

    public Map<String, Object> adminDashboard() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalGroups", bookGroupMapper.countTotalGroups());
        result.put("bannedGroups", bookGroupMapper.countBannedGroups());
        return result;
    }

    public IPage<BookGroup> adminGroupList(int page, int size, String keyword, Integer status) {
        Page<BookGroup> pageParam = new Page<>(page, size);
        return bookGroupMapper.selectAdminPage(pageParam, keyword, status);
    }

    @Transactional
    public void banGroup(Long adminId, Long groupId, String reason) {
        BookGroup group = bookGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException("小组不存在");
        }
        group.setStatus(0);
        group.setBanReason(reason);
        group.setBannedAt(LocalDateTime.now());
        group.setBannedBy(adminId);
        bookGroupMapper.updateById(group);
        log.info("封禁小组: adminId={}, groupId={}, reason={}", adminId, groupId, reason);
    }

    @Transactional
    public void unbanGroup(Long adminId, Long groupId) {
        BookGroup group = bookGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException("小组不存在");
        }
        group.setStatus(1);
        group.setBanReason("");
        group.setBannedAt(null);
        group.setBannedBy(null);
        bookGroupMapper.updateById(group);
        log.info("解封小组: adminId={}, groupId={}", adminId, groupId);
    }

    public IPage<Map<String, Object>> adminDynamicList(Long groupId, int page, int size) {
        Page<GroupDynamic> pageParam = new Page<>(page, size);
        return groupDynamicMapper.selectAdminPage(pageParam, groupId);
    }
}
