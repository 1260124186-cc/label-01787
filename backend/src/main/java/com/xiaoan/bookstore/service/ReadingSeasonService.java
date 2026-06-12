package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.dto.*;
import com.xiaoan.bookstore.entity.*;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReadingSeasonService {

    private static final Logger log = LoggerFactory.getLogger(ReadingSeasonService.class);

    private static final Map<Integer, String> STATUS_NAMES = new LinkedHashMap<>();
    static {
        STATUS_NAMES.put(0, "草稿");
        STATUS_NAMES.put(1, "报名中");
        STATUS_NAMES.put(2, "进行中");
        STATUS_NAMES.put(3, "已结束");
        STATUS_NAMES.put(4, "已取消");
    }

    private static final Map<Integer, String> SEASON_TYPE_NAMES = new LinkedHashMap<>();
    static {
        SEASON_TYPE_NAMES.put(1, "挑战赛");
        SEASON_TYPE_NAMES.put(2, "阅读马拉松");
        SEASON_TYPE_NAMES.put(3, "读书月");
    }

    private static final Map<Integer, String> PARTICIPANT_STATUS_NAMES = new LinkedHashMap<>();
    static {
        PARTICIPANT_STATUS_NAMES.put(0, "已退出");
        PARTICIPANT_STATUS_NAMES.put(1, "进行中");
        PARTICIPANT_STATUS_NAMES.put(2, "已完成");
        PARTICIPANT_STATUS_NAMES.put(3, "未完成");
        PARTICIPANT_STATUS_NAMES.put(4, "作弊取消资格");
    }

    private static final Map<Integer, String> STREAK_BADGE_TYPES = new LinkedHashMap<>();
    static {
        STREAK_BADGE_TYPES.put(3, "season_streak_3");
        STREAK_BADGE_TYPES.put(7, "season_streak_7");
        STREAK_BADGE_TYPES.put(14, "season_streak_14");
        STREAK_BADGE_TYPES.put(21, "season_streak_21");
        STREAK_BADGE_TYPES.put(30, "season_streak_30");
    }

    private static final Map<Integer, String> STREAK_BADGE_NAMES = new LinkedHashMap<>();
    static {
        STREAK_BADGE_NAMES.put(3, "三日笃行");
        STREAK_BADGE_NAMES.put(7, "七日不辍");
        STREAK_BADGE_NAMES.put(14, "两周坚守");
        STREAK_BADGE_NAMES.put(21, "廿一日挑战达成");
        STREAK_BADGE_NAMES.put(30, "月度阅读之星");
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ReadingSeasonMapper seasonMapper;
    private final SeasonParticipantMapper participantMapper;
    private final SeasonDailyRecordMapper dailyRecordMapper;
    private final SeasonBadgeMapper seasonBadgeMapper;
    private final SeasonCheatDetectionMapper cheatDetectionMapper;
    private final ReadingRecordMapper readingRecordMapper;
    private final PointsService pointsService;
    private final NotificationService notificationService;
    private final UserMapper userMapper;

    @Transactional
    public SeasonVO createSeason(SeasonCreateDTO dto) {
        validateSeasonDates(dto.getStartDate(), dto.getEndDate(), dto.getSignupStart(), dto.getSignupEnd());

        ReadingSeason season = new ReadingSeason();
        season.setTitle(dto.getTitle());
        season.setSubtitle(dto.getSubtitle() != null ? dto.getSubtitle() : "");
        season.setCoverImage(dto.getCoverImage() != null ? dto.getCoverImage() : "");
        season.setDescription(dto.getDescription() != null ? dto.getDescription() : "");
        season.setSeasonType(dto.getSeasonType() != null ? dto.getSeasonType() : 1);
        season.setStatus(0);
        season.setStartDate(dto.getStartDate());
        season.setEndDate(dto.getEndDate());
        season.setSignupStart(dto.getSignupStart());
        season.setSignupEnd(dto.getSignupEnd());
        season.setDurationDays(dto.getDurationDays());
        season.setDailyMinDuration(dto.getDailyMinDuration() != null ? dto.getDailyMinDuration() : 600);
        season.setDailyMaxDuration(dto.getDailyMaxDuration() != null ? dto.getDailyMaxDuration() : 28800);
        season.setMaxParticipants(dto.getMaxParticipants() != null ? dto.getMaxParticipants() : 0);
        season.setPointsReward(dto.getPointsReward() != null ? dto.getPointsReward() : 0);
        season.setBadgeIcon(dto.getBadgeIcon() != null ? dto.getBadgeIcon() : "");
        season.setBadgeName(dto.getBadgeName() != null ? dto.getBadgeName() : "");
        season.setRules(dto.getRules() != null ? dto.getRules() : "");
        season.setPrizeConfig(dto.getPrizeConfig() != null ? dto.getPrizeConfig() : "");
        season.setCheatThresholdSpeed(dto.getCheatThresholdSpeed() != null ? dto.getCheatThresholdSpeed() : 7200);
        season.setCheatThresholdStreak(dto.getCheatThresholdStreak() != null ? dto.getCheatThresholdStreak() : 168);
        season.setCheatAutoFlag(dto.getCheatAutoFlag() != null ? dto.getCheatAutoFlag() : 1);

        long daysBetween = ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1;
        if (dto.getDurationDays() != (int) daysBetween) {
            season.setDurationDays((int) daysBetween);
        }

        seasonMapper.insert(season);
        log.info("创建阅读赛季: seasonId={}, title={}", season.getId(), season.getTitle());
        return toSeasonVO(season, null);
    }

    @Transactional
    public SeasonVO updateSeason(Long seasonId, SeasonUpdateDTO dto) {
        ReadingSeason season = seasonMapper.selectById(seasonId);
        if (season == null) {
            throw new BusinessException("赛季不存在");
        }
        if (season.getStatus() != 0 && season.getStatus() != 1) {
            throw new BusinessException("赛季已开始，无法编辑");
        }

        if (dto.getTitle() != null) season.setTitle(dto.getTitle());
        if (dto.getSubtitle() != null) season.setSubtitle(dto.getSubtitle());
        if (dto.getCoverImage() != null) season.setCoverImage(dto.getCoverImage());
        if (dto.getDescription() != null) season.setDescription(dto.getDescription());
        if (dto.getSeasonType() != null) season.setSeasonType(dto.getSeasonType());
        if (dto.getStartDate() != null) season.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) season.setEndDate(dto.getEndDate());
        if (dto.getSignupStart() != null) season.setSignupStart(dto.getSignupStart());
        if (dto.getSignupEnd() != null) season.setSignupEnd(dto.getSignupEnd());
        if (dto.getDurationDays() != null) season.setDurationDays(dto.getDurationDays());
        if (dto.getDailyMinDuration() != null) season.setDailyMinDuration(dto.getDailyMinDuration());
        if (dto.getDailyMaxDuration() != null) season.setDailyMaxDuration(dto.getDailyMaxDuration());
        if (dto.getMaxParticipants() != null) season.setMaxParticipants(dto.getMaxParticipants());
        if (dto.getPointsReward() != null) season.setPointsReward(dto.getPointsReward());
        if (dto.getBadgeIcon() != null) season.setBadgeIcon(dto.getBadgeIcon());
        if (dto.getBadgeName() != null) season.setBadgeName(dto.getBadgeName());
        if (dto.getRules() != null) season.setRules(dto.getRules());
        if (dto.getPrizeConfig() != null) season.setPrizeConfig(dto.getPrizeConfig());
        if (dto.getCheatThresholdSpeed() != null) season.setCheatThresholdSpeed(dto.getCheatThresholdSpeed());
        if (dto.getCheatThresholdStreak() != null) season.setCheatThresholdStreak(dto.getCheatThresholdStreak());
        if (dto.getCheatAutoFlag() != null) season.setCheatAutoFlag(dto.getCheatAutoFlag());

        seasonMapper.updateById(season);
        log.info("更新阅读赛季: seasonId={}", seasonId);
        return toSeasonVO(season, null);
    }

    @Transactional
    public void publishSeason(Long seasonId) {
        ReadingSeason season = seasonMapper.selectById(seasonId);
        if (season == null) {
            throw new BusinessException("赛季不存在");
        }
        if (season.getStatus() != 0) {
            throw new BusinessException("只有草稿状态的赛季可以发布");
        }

        LocalDate today = LocalDate.now();
        if (season.getSignupStart() != null && !today.isBefore(season.getSignupStart())) {
            season.setStatus(1);
        } else if (season.getSignupStart() == null) {
            season.setStatus(1);
        } else {
            season.setStatus(1);
        }
        season.setPublishedAt(LocalDateTime.now());
        seasonMapper.updateById(season);
        log.info("发布阅读赛季: seasonId={}", seasonId);
    }

    @Transactional
    public SeasonParticipant signup(Long userId, Long seasonId) {
        ReadingSeason season = seasonMapper.selectById(seasonId);
        if (season == null) {
            throw new BusinessException("赛季不存在");
        }
        if (season.getStatus() != 1 && season.getStatus() != 2) {
            throw new BusinessException("赛季当前状态不允许报名");
        }

        LocalDate today = LocalDate.now();
        if (season.getSignupStart() != null && today.isBefore(season.getSignupStart())) {
            throw new BusinessException("报名尚未开始");
        }
        if (season.getSignupEnd() != null && today.isAfter(season.getSignupEnd())) {
            throw new BusinessException("报名已截止");
        }

        SeasonParticipant existing = participantMapper.findBySeasonAndUser(seasonId, userId);
        if (existing != null) {
            if (existing.getStatus() == 1) {
                throw new BusinessException("已报名该赛季");
            }
            if (existing.getStatus() == 4) {
                throw new BusinessException("因作弊已被取消资格，无法再次报名");
            }
            existing.setStatus(1);
            existing.setSignupAt(LocalDateTime.now());
            participantMapper.updateById(existing);
            return existing;
        }

        if (season.getMaxParticipants() > 0) {
            Integer currentCount = seasonMapper.countActiveParticipants(seasonId);
            if (currentCount != null && currentCount >= season.getMaxParticipants()) {
                throw new BusinessException("报名人数已达上限");
            }
        }

        SeasonParticipant participant = new SeasonParticipant();
        participant.setSeasonId(seasonId);
        participant.setUserId(userId);
        participant.setStatus(1);
        participant.setSignupAt(LocalDateTime.now());
        participant.setQualifiedDays(0);
        participant.setTotalDuration(0L);
        participant.setTotalBooks(0);
        participant.setStreakDays(0);
        participant.setMaxStreakDays(0);
        participant.setPointsAwarded(0);
        participant.setBadgeAwarded(0);
        participant.setPrizeClaimed(0);
        participantMapper.insert(participant);

        log.info("用户报名赛季: userId={}, seasonId={}", userId, seasonId);
        return participant;
    }

    @Transactional
    public void quitSeason(Long userId, Long seasonId) {
        SeasonParticipant participant = participantMapper.findBySeasonAndUser(seasonId, userId);
        if (participant == null) {
            throw new BusinessException("未报名该赛季");
        }
        if (participant.getStatus() != 1) {
            throw new BusinessException("当前状态无法退出");
        }
        participant.setStatus(0);
        participantMapper.updateById(participant);
        log.info("用户退出赛季: userId={}, seasonId={}", userId, seasonId);
    }

    @Transactional
    public void syncDailyRecord(Long userId, Long seasonId, LocalDate recordDate) {
        ReadingSeason season = seasonMapper.selectById(seasonId);
        if (season == null) {
            throw new BusinessException("赛季不存在");
        }
        SeasonParticipant participant = participantMapper.findBySeasonAndUser(seasonId, userId);
        if (participant == null || participant.getStatus() != 1) {
            throw new BusinessException("未报名或状态异常");
        }

        if (recordDate.isBefore(season.getStartDate()) || recordDate.isAfter(season.getEndDate())) {
            throw new BusinessException("记录日期不在赛季范围内");
        }

        Integer dailyDuration = dailyRecordMapper.sumDailyReadingDuration(userId, recordDate);
        if (dailyDuration == null) dailyDuration = 0;

        int effectiveDuration = Math.min(dailyDuration, season.getDailyMaxDuration());
        boolean isQualified = effectiveDuration >= season.getDailyMinDuration();

        Integer bookCount = dailyRecordMapper.countDistinctBooks(userId, season.getStartDate(), season.getEndDate());
        if (bookCount == null) bookCount = 0;

        SeasonDailyRecord existing = dailyRecordMapper.findBySeasonUserDate(seasonId, userId, recordDate);
        if (existing != null) {
            existing.setDuration(effectiveDuration);
            existing.setBookCount(bookCount);
            existing.setIsQualified(isQualified ? 1 : 0);
            dailyRecordMapper.updateById(existing);
        } else {
            SeasonDailyRecord record = new SeasonDailyRecord();
            record.setSeasonId(seasonId);
            record.setUserId(userId);
            record.setRecordDate(recordDate);
            record.setDuration(effectiveDuration);
            record.setBookCount(bookCount);
            record.setIsQualified(isQualified ? 1 : 0);
            record.setIsFlagged(0);
            record.setFlagReason("");
            dailyRecordMapper.insert(record);
        }

        if (season.getCheatAutoFlag() == 1) {
            runCheatDetection(season, userId, recordDate, dailyDuration);
        }

        recalculateParticipantProgress(season, participant);
        checkAndAwardStreakBadges(userId, seasonId, participant.getStreakDays());
        checkSeasonCompletion(season, participant);
    }

    private void runCheatDetection(ReadingSeason season, Long userId, LocalDate date, int dailyDuration) {
        int hoursInDay = 24;
        double avgSpeedPerHour = (double) dailyDuration / hoursInDay;

        if (avgSpeedPerHour > season.getCheatThresholdSpeed()) {
            flagCheatRecord(season.getId(), userId, date, 1,
                    "阅读速度异常：日均每小时" + Math.round(avgSpeedPerHour) + "秒，超过阈值" + season.getCheatThresholdSpeed() + "秒",
                    2);
            SeasonDailyRecord record = dailyRecordMapper.findBySeasonUserDate(season.getId(), userId, date);
            if (record != null) {
                record.setIsFlagged(1);
                record.setFlagReason("阅读速度异常");
                dailyRecordMapper.updateById(record);
            }
        }

        if (dailyDuration > season.getCheatThresholdStreak() * 3600) {
            flagCheatRecord(season.getId(), userId, date, 2,
                    "连续阅读时长异常：单日阅读" + (dailyDuration / 3600) + "小时，超过阈值" + season.getCheatThresholdStreak() + "小时",
                    3);
        }
    }

    private void flagCheatRecord(Long seasonId, Long userId, LocalDate date, int detectionType, String detail, int severity) {
        List<SeasonCheatDetection> existing = cheatDetectionMapper.findBySeasonUserDate(seasonId, userId, date);
        boolean alreadyFlagged = existing.stream().anyMatch(e -> e.getDetectionType() == detectionType);
        if (alreadyFlagged) return;

        SeasonCheatDetection detection = new SeasonCheatDetection();
        detection.setSeasonId(seasonId);
        detection.setUserId(userId);
        detection.setDetectionDate(date);
        detection.setDetectionType(detectionType);
        detection.setDetectionDetail(detail);
        detection.setSeverity(severity);
        detection.setStatus(0);
        cheatDetectionMapper.insert(detection);
        log.info("标记作弊嫌疑: userId={}, seasonId={}, type={}, date={}", userId, seasonId, detectionType, date);
    }

    private void recalculateParticipantProgress(ReadingSeason season, SeasonParticipant participant) {
        List<LocalDate> qualifiedDates = dailyRecordMapper.listQualifiedDates(season.getId(), participant.getUserId());
        int qualifiedDays = qualifiedDates.size();
        Long totalDuration = dailyRecordMapper.sumDurationBySeasonUser(season.getId(), participant.getUserId());
        if (totalDuration == null) totalDuration = 0L;

        int streakDays = calculateStreak(qualifiedDates);
        int maxStreakDays = participant.getMaxStreakDays();
        if (streakDays > maxStreakDays) {
            maxStreakDays = streakDays;
        }

        Integer totalBooks = dailyRecordMapper.countDistinctBooks(
                participant.getUserId(), season.getStartDate(), season.getEndDate());
        if (totalBooks == null) totalBooks = 0;

        participant.setQualifiedDays(qualifiedDays);
        participant.setTotalDuration(totalDuration);
        participant.setStreakDays(streakDays);
        participant.setMaxStreakDays(maxStreakDays);
        participant.setTotalBooks(totalBooks);

        participantMapper.updateProgress(season.getId(), participant.getUserId(),
                streakDays, maxStreakDays, qualifiedDays, totalDuration, totalBooks, participant.getStatus());
    }

    private int calculateStreak(List<LocalDate> qualifiedDates) {
        if (qualifiedDates.isEmpty()) return 0;
        Collections.sort(qualifiedDates);
        LocalDate today = LocalDate.now();
        int streak = 0;
        LocalDate checkDate = today;

        for (int i = qualifiedDates.size() - 1; i >= 0; i--) {
            if (qualifiedDates.get(i).equals(checkDate)) {
                streak++;
                checkDate = checkDate.minusDays(1);
            } else if (qualifiedDates.get(i).isBefore(checkDate)) {
                break;
            }
        }
        return streak;
    }

    private void checkAndAwardStreakBadges(Long userId, Long seasonId, int streakDays) {
        for (Map.Entry<Integer, String> entry : STREAK_BADGE_TYPES.entrySet()) {
            int threshold = entry.getKey();
            String badgeType = entry.getValue();
            if (streakDays >= threshold) {
                Integer existing = seasonBadgeMapper.countByUserSeasonType(userId, seasonId, badgeType);
                if (existing == null || existing == 0) {
                    SeasonBadge badge = new SeasonBadge();
                    badge.setUserId(userId);
                    badge.setSeasonId(seasonId);
                    badge.setBadgeType(badgeType);
                    badge.setBadgeName(STREAK_BADGE_NAMES.get(threshold));
                    badge.setBadgeIcon(badgeType);
                    seasonBadgeMapper.insert(badge);
                    log.info("颁发赛季徽章: userId={}, seasonId={}, badge={}", userId, seasonId, badgeType);
                }
            }
        }
    }

    @Transactional
    public void checkSeasonCompletion(ReadingSeason season, SeasonParticipant participant) {
        if (participant.getQualifiedDays() >= season.getDurationDays()) {
            participant.setStatus(2);
            participant.setCompletedAt(LocalDateTime.now());
            participantMapper.updateById(participant);

            if (season.getPointsReward() > 0 && participant.getPointsAwarded() == 0) {
                pointsService.earnPoints(participant.getUserId(), "season_complete",
                        season.getPointsReward(), "完成赛季: " + season.getTitle(),
                        String.valueOf(season.getId()));
                participant.setPointsAwarded(season.getPointsReward());
                participantMapper.updateById(participant);
            }

            if (participant.getBadgeAwarded() == 0 && !season.getBadgeIcon().isEmpty()) {
                SeasonBadge badge = new SeasonBadge();
                badge.setUserId(participant.getUserId());
                badge.setSeasonId(season.getId());
                badge.setBadgeType("season_complete");
                badge.setBadgeName(season.getBadgeName().isEmpty() ? "赛季挑战达成" : season.getBadgeName());
                badge.setBadgeIcon(season.getBadgeIcon());
                seasonBadgeMapper.insert(badge);
                participant.setBadgeAwarded(1);
                participantMapper.updateById(participant);
            }

            Map<String, String> variables = new HashMap<>();
            variables.put("seasonTitle", season.getTitle());
            variables.put("points", String.valueOf(season.getPointsReward()));
            notificationService.sendByTemplate("season_complete", participant.getUserId(), variables,
                    String.valueOf(season.getId()));

            log.info("赛季挑战完成: userId={}, seasonId={}", participant.getUserId(), season.getId());
        }
    }

    public List<SeasonVO> listSeasons(Integer status, Long userId) {
        LambdaQueryWrapper<ReadingSeason> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(ReadingSeason::getStatus, status);
        } else {
            wrapper.ne(ReadingSeason::getStatus, 0);
        }
        wrapper.orderByDesc(ReadingSeason::getStartDate);
        List<ReadingSeason> seasons = seasonMapper.selectList(wrapper);
        return seasons.stream().map(s -> {
            SeasonParticipant participant = userId != null ? participantMapper.findBySeasonAndUser(s.getId(), userId) : null;
            return toSeasonVO(s, participant);
        }).collect(Collectors.toList());
    }

    public SeasonVO getSeasonDetail(Long seasonId, Long userId) {
        ReadingSeason season = seasonMapper.selectById(seasonId);
        if (season == null) {
            throw new BusinessException("赛季不存在");
        }
        SeasonParticipant participant = userId != null ? participantMapper.findBySeasonAndUser(seasonId, userId) : null;
        return toSeasonVO(season, participant);
    }

    public SeasonProgressVO getProgress(Long userId, Long seasonId) {
        ReadingSeason season = seasonMapper.selectById(seasonId);
        if (season == null) {
            throw new BusinessException("赛季不存在");
        }
        SeasonParticipant participant = participantMapper.findBySeasonAndUser(seasonId, userId);
        if (participant == null) {
            throw new BusinessException("未报名该赛季");
        }

        SeasonProgressVO vo = new SeasonProgressVO();
        vo.setSeasonId(seasonId);
        vo.setTitle(season.getTitle());
        vo.setStatus(season.getStatus());
        vo.setDurationDays(season.getDurationDays());
        vo.setDailyMinDuration(season.getDailyMinDuration());

        LocalDate today = LocalDate.now();
        long daysElapsed = ChronoUnit.DAYS.between(season.getStartDate(), today) + 1;
        if (today.isBefore(season.getStartDate())) daysElapsed = 0;
        if (today.isAfter(season.getEndDate())) daysElapsed = season.getDurationDays();
        vo.setDaysElapsed((int) daysElapsed);
        vo.setDaysRemaining(Math.max(0, season.getDurationDays() - (int) daysElapsed));

        vo.setQualifiedDays(participant.getQualifiedDays());
        vo.setTotalDuration(participant.getTotalDuration());
        vo.setStreakDays(participant.getStreakDays());
        vo.setMaxStreakDays(participant.getMaxStreakDays());

        if (season.getDurationDays() > 0) {
            vo.setProgress(Math.round((double) participant.getQualifiedDays() / season.getDurationDays() * 1000.0) / 10.0);
        } else {
            vo.setProgress(0.0);
        }

        Integer rank = seasonMapper.getUserRank(seasonId, participant.getQualifiedDays(), participant.getTotalDuration());
        vo.setRank(rank);

        List<LocalDate> qualifiedDates = dailyRecordMapper.listQualifiedDates(seasonId, userId);
        vo.setQualifiedDates(qualifiedDates.stream().map(LocalDate::toString).collect(Collectors.toList()));

        List<Map<String, Object>> dailyRecords = dailyRecordMapper.listDailyRecords(seasonId, userId);
        List<SeasonDailyRecordVO> dailyRecordVOs = dailyRecords.stream().map(r -> {
            SeasonDailyRecordVO dr = new SeasonDailyRecordVO();
            dr.setDate(String.valueOf(r.get("date")));
            dr.setDuration(r.get("duration") != null ? ((Number) r.get("duration")).intValue() : 0);
            dr.setBookCount(r.get("bookCount") != null ? ((Number) r.get("bookCount")).intValue() : 0);
            dr.setIsQualified(r.get("isQualified") != null && ((Number) r.get("isQualified")).intValue() == 1);
            dr.setIsFlagged(r.get("isFlagged") != null && ((Number) r.get("isFlagged")).intValue() == 1);
            dr.setFlagReason(r.get("flagReason") != null ? String.valueOf(r.get("flagReason")) : "");
            dr.setFormattedDuration(formatDuration(dr.getDuration()));
            return dr;
        }).collect(Collectors.toList());
        vo.setDailyRecords(dailyRecordVOs);

        return vo;
    }

    public Page<SeasonLeaderboardVO> getLeaderboard(Long seasonId, int page, int size) {
        ReadingSeason season = seasonMapper.selectById(seasonId);
        if (season == null) {
            throw new BusinessException("赛季不存在");
        }

        int offset = (page - 1) * size;
        List<Map<String, Object>> records = seasonMapper.leaderboard(seasonId, offset, size);
        Integer totalParticipants = seasonMapper.countActiveParticipants(seasonId);

        List<SeasonLeaderboardVO> voList = new ArrayList<>();
        int rank = offset + 1;
        for (Map<String, Object> record : records) {
            SeasonLeaderboardVO vo = new SeasonLeaderboardVO();
            vo.setRank(rank++);
            vo.setUserId(((Number) record.get("user_id")).longValue());
            vo.setNickname(record.get("nickname") != null ? String.valueOf(record.get("nickname")) : "");
            vo.setAvatarUrl(record.get("avatar_url") != null ? String.valueOf(record.get("avatar_url")) : "");
            vo.setQualifiedDays(record.get("qualified_days") != null ? ((Number) record.get("qualified_days")).intValue() : 0);
            vo.setTotalDuration(record.get("total_duration") != null ? ((Number) record.get("total_duration")).longValue() : 0L);
            vo.setStreakDays(record.get("streak_days") != null ? ((Number) record.get("streak_days")).intValue() : 0);
            vo.setFormattedDuration(formatDuration(vo.getTotalDuration()));
            voList.add(vo);
        }

        Page<SeasonLeaderboardVO> result = new Page<>(page, size);
        result.setRecords(voList);
        result.setTotal(totalParticipants != null ? totalParticipants.longValue() : 0L);
        return result;
    }

    public List<SeasonBadgeVO> getUserBadges(Long userId) {
        List<SeasonBadge> badges = seasonBadgeMapper.listByUserId(userId);
        return badges.stream().map(this::toBadgeVO).collect(Collectors.toList());
    }

    public List<SeasonBadgeVO> getSeasonBadges(Long userId, Long seasonId) {
        List<SeasonBadge> badges = seasonBadgeMapper.listByUserAndSeason(userId, seasonId);
        return badges.stream().map(this::toBadgeVO).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getMySeasons(Long userId, Integer status) {
        return participantMapper.findActiveByUser(userId);
    }

    public Page<Map<String, Object>> getMySeasonsPaged(Long userId, Integer status, int page, int size) {
        int offset = (page - 1) * size;
        List<Map<String, Object>> records = participantMapper.findByUserPaged(userId, offset, size);
        Integer total = participantMapper.countByUser(userId);

        Page<Map<String, Object>> result = new Page<>(page, size);
        result.setRecords(records);
        result.setTotal(total != null ? total.longValue() : 0L);
        return result;
    }

    @Transactional
    public void deleteSeason(Long seasonId) {
        ReadingSeason season = seasonMapper.selectById(seasonId);
        if (season == null) {
            throw new BusinessException("赛季不存在");
        }
        if (season.getStatus() == 2) {
            throw new BusinessException("进行中的赛季无法删除");
        }
        season.setStatus(4);
        seasonMapper.updateById(season);
        log.info("删除(取消)阅读赛季: seasonId={}", seasonId);
    }

    @Transactional
    public void awardPrizes(Long seasonId) {
        ReadingSeason season = seasonMapper.selectById(seasonId);
        if (season == null) {
            throw new BusinessException("赛季不存在");
        }
        if (season.getStatus() != 3) {
            throw new BusinessException("只有已结束的赛季可以发放奖品");
        }

        LambdaQueryWrapper<SeasonParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeasonParticipant::getSeasonId, seasonId)
                .eq(SeasonParticipant::getStatus, 2)
                .eq(SeasonParticipant::getPointsAwarded, 0);
        List<SeasonParticipant> participants = participantMapper.selectList(wrapper);

        for (SeasonParticipant p : participants) {
            if (season.getPointsReward() > 0) {
                pointsService.earnPoints(p.getUserId(), "season_complete",
                        season.getPointsReward(), "完成赛季: " + season.getTitle(),
                        String.valueOf(seasonId));
                p.setPointsAwarded(season.getPointsReward());
            }
            if (p.getBadgeAwarded() == 0 && !season.getBadgeIcon().isEmpty()) {
                SeasonBadge badge = new SeasonBadge();
                badge.setUserId(p.getUserId());
                badge.setSeasonId(seasonId);
                badge.setBadgeType("season_complete");
                badge.setBadgeName(season.getBadgeName().isEmpty() ? "赛季挑战达成" : season.getBadgeName());
                badge.setBadgeIcon(season.getBadgeIcon());
                seasonBadgeMapper.insert(badge);
                p.setBadgeAwarded(1);
            }
            participantMapper.updateById(p);
        }

        log.info("批量发放赛季奖品: seasonId={}, participantCount={}", seasonId, participants.size());
    }

    @Transactional
    public void handleCheatDetection(Long detectionId, Long handlerId, int status, String handleResult) {
        SeasonCheatDetection detection = cheatDetectionMapper.selectById(detectionId);
        if (detection == null) {
            throw new BusinessException("作弊检测记录不存在");
        }
        if (detection.getStatus() != 0) {
            throw new BusinessException("该记录已处理");
        }
        detection.setStatus(status);
        detection.setHandledBy(handlerId);
        detection.setHandleResult(handleResult != null ? handleResult : "");
        detection.setHandledAt(LocalDateTime.now());
        cheatDetectionMapper.updateById(detection);

        if (status == 1) {
            SeasonParticipant participant = participantMapper.findBySeasonAndUser(detection.getSeasonId(), detection.getUserId());
            if (participant != null && participant.getStatus() == 1) {
                participant.setStatus(4);
                participantMapper.updateById(participant);
                log.info("确认作弊，取消用户资格: userId={}, seasonId={}", detection.getUserId(), detection.getSeasonId());
            }
        }

        log.info("处理作弊检测: detectionId={}, status={}", detectionId, status);
    }

    public Page<Map<String, Object>> listCheatDetections(Integer status, int page, int size) {
        int offset = (page - 1) * size;
        int queryStatus = status != null ? status : 0;
        List<Map<String, Object>> records = cheatDetectionMapper.findByStatusPaged(queryStatus, offset, size);
        Long total = cheatDetectionMapper.countByStatus(queryStatus);

        Page<Map<String, Object>> result = new Page<>(page, size);
        result.setRecords(records);
        result.setTotal(total != null ? total : 0L);
        return result;
    }

    public SeasonStatsVO getAdminStats() {
        SeasonStatsVO vo = new SeasonStatsVO();
        vo.setTotalSeasons(seasonMapper.countTotalSeasons());
        vo.setActiveSeasons(seasonMapper.countActiveSeasons());
        vo.setTotalParticipants(participantMapper.countTotalParticipants());
        vo.setSeasonTypeStats(seasonMapper.seasonTypeStats());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.minusDays(30).toLocalDate().atStartOfDay();
        vo.setDailyCreationStats(seasonMapper.dailyCreationStats(monthStart, now));

        return vo;
    }

    public Map<String, Object> getSeasonAdminDetail(Long seasonId) {
        ReadingSeason season = seasonMapper.selectById(seasonId);
        if (season == null) {
            throw new BusinessException("赛季不存在");
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("season", season);
        detail.put("activeParticipants", seasonMapper.countActiveParticipants(seasonId));
        detail.put("completedParticipants", participantMapper.countCompletedBySeason(seasonId));
        detail.put("disqualifiedParticipants", participantMapper.countCheatDisqualified(seasonId));
        detail.put("flaggedRecords", dailyRecordMapper.countFlaggedRecords(seasonId));
        detail.put("detectionTypeStats", cheatDetectionMapper.detectionTypeStats(seasonId));
        detail.put("badgeStats", seasonBadgeMapper.badgeStatsBySeason(seasonId));
        detail.put("dailyQualifiedStats", dailyRecordMapper.dailyQualifiedStats(seasonId));

        return detail;
    }

    @Transactional
    public void syncAllParticipantDailyRecords(Long seasonId) {
        ReadingSeason season = seasonMapper.selectById(seasonId);
        if (season == null) {
            throw new BusinessException("赛季不存在");
        }
        if (season.getStatus() != 2) {
            throw new BusinessException("只有进行中的赛季可以同步");
        }

        LocalDate today = LocalDate.now();
        if (today.isBefore(season.getStartDate()) || today.isAfter(season.getEndDate())) {
            return;
        }

        LambdaQueryWrapper<SeasonParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeasonParticipant::getSeasonId, seasonId)
                .eq(SeasonParticipant::getStatus, 1);
        List<SeasonParticipant> participants = participantMapper.selectList(wrapper);

        for (SeasonParticipant p : participants) {
            try {
                syncDailyRecord(p.getUserId(), seasonId, today);
            } catch (Exception e) {
                log.warn("同步用户赛季每日记录失败: userId={}, seasonId={}, error={}", p.getUserId(), seasonId, e.getMessage());
            }
        }

        log.info("批量同步赛季每日记录: seasonId={}, participantCount={}", seasonId, participants.size());
    }

    @Transactional
    public void endSeason(Long seasonId) {
        ReadingSeason season = seasonMapper.selectById(seasonId);
        if (season == null) {
            throw new BusinessException("赛季不存在");
        }
        if (season.getStatus() != 2) {
            throw new BusinessException("只有进行中的赛季可以手动结束");
        }

        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<SeasonParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeasonParticipant::getSeasonId, seasonId)
                .eq(SeasonParticipant::getStatus, 1);
        List<SeasonParticipant> activeParticipants = participantMapper.selectList(wrapper);

        for (SeasonParticipant p : activeParticipants) {
            try {
                syncDailyRecord(p.getUserId(), seasonId, today);
            } catch (Exception e) {
                log.warn("结束赛季同步失败: userId={}, error={}", p.getUserId(), e.getMessage());
            }

            SeasonParticipant refreshed = participantMapper.findBySeasonAndUser(seasonId, p.getUserId());
            if (refreshed != null && refreshed.getStatus() == 1) {
                refreshed.setStatus(3);
                participantMapper.updateById(refreshed);
            }
        }

        season.setStatus(3);
        seasonMapper.updateById(season);
        log.info("手动结束赛季: seasonId={}", seasonId);
    }

    private void validateSeasonDates(LocalDate startDate, LocalDate endDate, LocalDate signupStart, LocalDate signupEnd) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
        if (signupStart != null && signupEnd != null && signupStart.isAfter(signupEnd)) {
            throw new BusinessException("报名开始日期不能晚于报名截止日期");
        }
        if (signupStart != null && signupStart.isAfter(startDate)) {
            throw new BusinessException("报名开始日期不能晚于赛季开始日期");
        }
    }

    private SeasonVO toSeasonVO(ReadingSeason season, SeasonParticipant participant) {
        SeasonVO vo = new SeasonVO();
        vo.setId(season.getId());
        vo.setTitle(season.getTitle());
        vo.setSubtitle(season.getSubtitle());
        vo.setCoverImage(season.getCoverImage());
        vo.setDescription(season.getDescription());
        vo.setSeasonType(season.getSeasonType());
        vo.setStatus(season.getStatus());
        vo.setStatusName(STATUS_NAMES.getOrDefault(season.getStatus(), "未知"));
        vo.setStartDate(season.getStartDate() != null ? season.getStartDate().toString() : "");
        vo.setEndDate(season.getEndDate() != null ? season.getEndDate().toString() : "");
        vo.setSignupStart(season.getSignupStart() != null ? season.getSignupStart().toString() : "");
        vo.setSignupEnd(season.getSignupEnd() != null ? season.getSignupEnd().toString() : "");
        vo.setDurationDays(season.getDurationDays());
        vo.setDailyMinDuration(season.getDailyMinDuration());
        vo.setDailyMaxDuration(season.getDailyMaxDuration());
        vo.setMaxParticipants(season.getMaxParticipants());
        vo.setCurrentParticipants(seasonMapper.countActiveParticipants(season.getId()));
        vo.setPointsReward(season.getPointsReward());
        vo.setBadgeIcon(season.getBadgeIcon());
        vo.setBadgeName(season.getBadgeName());
        vo.setRules(season.getRules());
        vo.setPrizeConfig(season.getPrizeConfig());
        vo.setIsJoined(participant != null && participant.getStatus() == 1);

        if (participant != null) {
            SeasonParticipantVO pvo = new SeasonParticipantVO();
            pvo.setId(participant.getId());
            pvo.setSeasonId(participant.getSeasonId());
            pvo.setUserId(participant.getUserId());
            pvo.setStatus(participant.getStatus());
            pvo.setStatusName(PARTICIPANT_STATUS_NAMES.getOrDefault(participant.getStatus(), "未知"));
            pvo.setSignupAt(participant.getSignupAt() != null ? participant.getSignupAt().format(DATETIME_FMT) : "");
            pvo.setQualifiedDays(participant.getQualifiedDays());
            pvo.setTotalDuration(participant.getTotalDuration());
            pvo.setTotalBooks(participant.getTotalBooks());
            pvo.setStreakDays(participant.getStreakDays());
            pvo.setMaxStreakDays(participant.getMaxStreakDays());
            pvo.setCompletedAt(participant.getCompletedAt() != null ? participant.getCompletedAt().format(DATETIME_FMT) : "");
            pvo.setPointsAwarded(participant.getPointsAwarded());
            pvo.setBadgeAwarded(participant.getBadgeAwarded());
            pvo.setPrizeClaimed(participant.getPrizeClaimed());

            if (season.getDurationDays() > 0) {
                pvo.setProgress(Math.round((double) participant.getQualifiedDays() / season.getDurationDays() * 1000.0) / 10.0);
            } else {
                pvo.setProgress(0.0);
            }

            Integer rank = seasonMapper.getUserRank(season.getId(), participant.getQualifiedDays(), participant.getTotalDuration());
            pvo.setRank(rank);

            vo.setParticipantInfo(pvo);
        }

        return vo;
    }

    private SeasonBadgeVO toBadgeVO(SeasonBadge badge) {
        SeasonBadgeVO vo = new SeasonBadgeVO();
        vo.setId(badge.getId());
        vo.setBadgeType(badge.getBadgeType());
        vo.setBadgeName(badge.getBadgeName());
        vo.setBadgeIcon(badge.getBadgeIcon());
        vo.setEarnedAt(badge.getEarnedAt() != null ? badge.getEarnedAt().format(DATETIME_FMT) : "");
        return vo;
    }

    private String formatDuration(long seconds) {
        if (seconds <= 0) return "0分钟";
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours > 0) {
            return hours + "小时" + minutes + "分钟";
        }
        return minutes + "分钟";
    }
}
