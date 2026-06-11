package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaoan.bookstore.common.Constants;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlazaService {

    private static final Logger log = LoggerFactory.getLogger(PlazaService.class);

    private final PublicExcerptMapper publicExcerptMapper;
    private final ExcerptLikeMapper excerptLikeMapper;
    private final ExcerptFavoriteMapper excerptFavoriteMapper;
    private final ExcerptReportMapper excerptReportMapper;
    private final AnnotationMapper annotationMapper;
    private final BookMapper bookMapper;
    private final UserMapper userMapper;
    private final ContentComplianceService contentComplianceService;
    private final PointsService pointsService;

    @Transactional
    public PublicExcerpt publishExcerpt(Long userId, ExcerptPublishDTO dto) {
        Annotation annotation = annotationMapper.selectById(dto.getAnnotationId());
        if (annotation == null) {
            throw new BusinessException("批注不存在");
        }
        TenantValidator.validateCrossTenant(annotation.getUserId(), TenantContext.getTenantId());

        if (annotation.getType() != Constants.ANNOTATION_COMMENT) {
            throw new BusinessException("仅评语类型的批注可发布到广场");
        }

        Long count = publicExcerptMapper.selectCount(new LambdaQueryWrapper<PublicExcerpt>()
                .eq(PublicExcerpt::getAnnotationId, annotation.getId())
                .eq(PublicExcerpt::getStatus, Constants.EXCERPT_STATUS_NORMAL));
        if (count > 0) {
            throw new BusinessException("该批注已发布过");
        }

        Book book = bookMapper.selectById(annotation.getBookId());
        if (book == null) {
            throw new BusinessException("书籍不存在");
        }

        PublicExcerpt excerpt = new PublicExcerpt();
        excerpt.setUserId(userId);
        excerpt.setAnnotationId(annotation.getId());
        excerpt.setBookId(book.getId());
        excerpt.setBookTitle(book.getTitle());
        excerpt.setBookAuthor(book.getAuthor() != null ? book.getAuthor() : "");
        excerpt.setExcerptText(annotation.getSelectedText() != null ? annotation.getSelectedText() : "");
        excerpt.setCommentText(dto.getCommentText() != null ? dto.getCommentText() : annotation.getContent());
        excerpt.setLikes(0);
        excerpt.setFavorites(0);
        excerpt.setViews(0);
        excerpt.setStatus(Constants.EXCERPT_STATUS_NORMAL);
        excerpt.setAuditStatus(Constants.EXCERPT_AUDIT_PENDING);
        excerpt.setReportCount(0);
        publicExcerptMapper.insert(excerpt);

        try {
            String auditContent = excerpt.getExcerptText() + " " + excerpt.getCommentText();
            ContentAudit audit = contentComplianceService.auditText(
                    Constants.AUDIT_TARGET_EXCERPT, excerpt.getId(), auditContent);
            
            Integer auditStatus;
            if (audit.getResult() == Constants.AUDIT_RESULT_PASS) {
                auditStatus = Constants.EXCERPT_AUDIT_PASS;
            } else if (audit.getResult() == Constants.AUDIT_RESULT_VIOLATION) {
                auditStatus = Constants.EXCERPT_AUDIT_REJECT;
            } else {
                auditStatus = Constants.EXCERPT_AUDIT_PENDING;
            }
            excerpt.setAuditStatus(auditStatus);
            publicExcerptMapper.updateById(excerpt);
        } catch (Exception e) {
            log.warn("书摘内容审核失败，不影响发布: {}", e.getMessage());
        }

        try {
            pointsService.earnPoints(userId, Constants.POINTS_CATEGORY_SHARE_EXCERPT, 0,
                    "分享书摘到广场", String.valueOf(excerpt.getId()));
        } catch (Exception e) {
            log.warn("分享书摘积分发放失败，不影响发布: {}", e.getMessage());
        }

        log.info("发布书摘到广场: userId={}, excerptId={}, bookTitle={}", userId, excerpt.getId(), excerpt.getBookTitle());
        return excerpt;
    }

    @Transactional
    public void withdrawExcerpt(Long userId, Long excerptId) {
        PublicExcerpt excerpt = publicExcerptMapper.selectById(excerptId);
        if (excerpt == null) {
            throw new BusinessException("书摘不存在");
        }
        TenantValidator.validateCrossTenant(excerpt.getUserId(), TenantContext.getTenantId());

        if (excerpt.getStatus() == Constants.EXCERPT_STATUS_WITHDRAWN) {
            throw new BusinessException("该书摘已撤回");
        }

        excerpt.setStatus(Constants.EXCERPT_STATUS_WITHDRAWN);
        publicExcerptMapper.updateById(excerpt);

        log.info("撤回书摘: userId={}, excerptId={}", userId, excerptId);
    }

    public IPage<PublicExcerptVO> listExcerpts(Long userId, String sortBy, int page, int size) {
        LambdaQueryWrapper<PublicExcerpt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PublicExcerpt::getStatus, Constants.EXCERPT_STATUS_NORMAL);
        wrapper.eq(PublicExcerpt::getAuditStatus, Constants.EXCERPT_AUDIT_PASS);

        if (Constants.SORT_BY_HOT.equals(sortBy)) {
            wrapper.orderByDesc(PublicExcerpt::getLikes);
            wrapper.orderByDesc(PublicExcerpt::getCreatedAt);
        } else {
            wrapper.orderByDesc(PublicExcerpt::getCreatedAt);
        }

        Page<PublicExcerpt> excerptPage = publicExcerptMapper.selectPage(new Page<>(page, size), wrapper);

        List<PublicExcerptVO> voList = convertToVOList(excerptPage.getRecords(), userId);

        Page<PublicExcerptVO> result = new Page<>(page, size, excerptPage.getTotal());
        result.setRecords(voList);
        return result;
    }

    public PublicExcerptVO getExcerptDetail(Long userId, Long excerptId) {
        PublicExcerpt excerpt = publicExcerptMapper.selectById(excerptId);
        if (excerpt == null) {
            throw new BusinessException("书摘不存在");
        }

        if (excerpt.getStatus() != Constants.EXCERPT_STATUS_NORMAL ||
                excerpt.getAuditStatus() != Constants.EXCERPT_AUDIT_PASS) {
            if (!excerpt.getUserId().equals(userId)) {
                throw new BusinessException("书摘不可见");
            }
        }

        excerpt.setViews(excerpt.getViews() + 1);
        publicExcerptMapper.updateById(excerpt);

        return convertToVO(excerpt, userId);
    }

    public IPage<PublicExcerptVO> myExcerpts(Long userId, int page, int size) {
        LambdaQueryWrapper<PublicExcerpt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PublicExcerpt::getUserId, userId);
        wrapper.orderByDesc(PublicExcerpt::getCreatedAt);

        Page<PublicExcerpt> excerptPage = publicExcerptMapper.selectPage(new Page<>(page, size), wrapper);

        List<PublicExcerptVO> voList = convertToVOList(excerptPage.getRecords(), userId);

        Page<PublicExcerptVO> result = new Page<>(page, size, excerptPage.getTotal());
        result.setRecords(voList);
        return result;
    }

    public IPage<PublicExcerptVO> myFavorites(Long userId, int page, int size) {
        LambdaQueryWrapper<ExcerptFavorite> favoriteWrapper = new LambdaQueryWrapper<>();
        favoriteWrapper.eq(ExcerptFavorite::getUserId, userId);
        favoriteWrapper.orderByDesc(ExcerptFavorite::getCreatedAt);

        Page<ExcerptFavorite> favoritePage = excerptFavoriteMapper.selectPage(new Page<>(page, size), favoriteWrapper);

        if (favoritePage.getRecords().isEmpty()) {
            return new Page<>(page, size, 0);
        }

        List<Long> excerptIds = favoritePage.getRecords().stream()
                .map(ExcerptFavorite::getExcerptId)
                .collect(Collectors.toList());

        List<PublicExcerpt> excerpts = publicExcerptMapper.selectBatchIds(excerptIds);
        Map<Long, PublicExcerpt> excerptMap = excerpts.stream()
                .collect(Collectors.toMap(PublicExcerpt::getId, e -> e));

        List<PublicExcerpt> sortedExcerpts = excerptIds.stream()
                .map(excerptMap::get)
                .filter(Objects::nonNull)
                .filter(e -> e.getStatus() == Constants.EXCERPT_STATUS_NORMAL &&
                        e.getAuditStatus() == Constants.EXCERPT_AUDIT_PASS)
                .collect(Collectors.toList());

        List<PublicExcerptVO> voList = convertToVOList(sortedExcerpts, userId);

        Page<PublicExcerptVO> result = new Page<>(page, size, favoritePage.getTotal());
        result.setRecords(voList);
        return result;
    }

    @Transactional
    public boolean toggleLike(Long userId, Long excerptId) {
        PublicExcerpt excerpt = publicExcerptMapper.selectById(excerptId);
        if (excerpt == null) {
            throw new BusinessException("书摘不存在");
        }

        LambdaQueryWrapper<ExcerptLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExcerptLike::getExcerptId, excerptId);
        wrapper.eq(ExcerptLike::getUserId, userId);

        ExcerptLike existingLike = excerptLikeMapper.selectOne(wrapper);
        boolean liked;

        if (existingLike != null) {
            excerptLikeMapper.deleteById(existingLike.getId());
            excerpt.setLikes(Math.max(0, excerpt.getLikes() - 1));
            liked = false;
            log.info("取消点赞: userId={}, excerptId={}", userId, excerptId);
        } else {
            ExcerptLike like = new ExcerptLike();
            like.setExcerptId(excerptId);
            like.setUserId(userId);
            excerptLikeMapper.insert(like);
            excerpt.setLikes(excerpt.getLikes() + 1);
            liked = true;
            log.info("点赞: userId={}, excerptId={}", userId, excerptId);
        }

        publicExcerptMapper.updateById(excerpt);
        return liked;
    }

    @Transactional
    public boolean toggleFavorite(Long userId, Long excerptId) {
        PublicExcerpt excerpt = publicExcerptMapper.selectById(excerptId);
        if (excerpt == null) {
            throw new BusinessException("书摘不存在");
        }

        LambdaQueryWrapper<ExcerptFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExcerptFavorite::getExcerptId, excerptId);
        wrapper.eq(ExcerptFavorite::getUserId, userId);

        ExcerptFavorite existingFavorite = excerptFavoriteMapper.selectOne(wrapper);
        boolean favorited;

        if (existingFavorite != null) {
            excerptFavoriteMapper.deleteById(existingFavorite.getId());
            excerpt.setFavorites(Math.max(0, excerpt.getFavorites() - 1));
            favorited = false;
            log.info("取消收藏: userId={}, excerptId={}", userId, excerptId);
        } else {
            ExcerptFavorite favorite = new ExcerptFavorite();
            favorite.setExcerptId(excerptId);
            favorite.setUserId(userId);
            excerptFavoriteMapper.insert(favorite);
            excerpt.setFavorites(excerpt.getFavorites() + 1);
            favorited = true;
            log.info("收藏: userId={}, excerptId={}", userId, excerptId);
        }

        publicExcerptMapper.updateById(excerpt);
        return favorited;
    }

    @Transactional
    public ExcerptReport reportExcerpt(Long userId, ExcerptReportDTO dto) {
        PublicExcerpt excerpt = publicExcerptMapper.selectById(dto.getExcerptId());
        if (excerpt == null) {
            throw new BusinessException("书摘不存在");
        }

        ExcerptReport report = new ExcerptReport();
        report.setExcerptId(dto.getExcerptId());
        report.setReporterId(userId);
        report.setReason(dto.getReason());
        report.setDetail(dto.getDetail() != null ? dto.getDetail() : "");
        report.setStatus(Constants.EXCERPT_REPORT_PENDING);
        excerptReportMapper.insert(report);

        excerpt.setReportCount(excerpt.getReportCount() + 1);
        publicExcerptMapper.updateById(excerpt);

        log.info("举报书摘: userId={}, excerptId={}, reason={}", userId, dto.getExcerptId(), dto.getReason());
        return report;
    }

    public IPage<PublicExcerpt> adminListExcerpts(Integer status, Integer auditStatus, int page, int size) {
        LambdaQueryWrapper<PublicExcerpt> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(PublicExcerpt::getStatus, status);
        }
        if (auditStatus != null) {
            wrapper.eq(PublicExcerpt::getAuditStatus, auditStatus);
        }
        wrapper.orderByDesc(PublicExcerpt::getCreatedAt);
        return publicExcerptMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Transactional
    public void auditExcerpt(Long excerptId, Integer auditStatus, String reason, Long auditorId) {
        PublicExcerpt excerpt = publicExcerptMapper.selectById(excerptId);
        if (excerpt == null) {
            throw new BusinessException("书摘不存在");
        }

        excerpt.setAuditStatus(auditStatus);
        publicExcerptMapper.updateById(excerpt);

        log.info("审核书摘: excerptId={}, auditStatus={}, auditorId={}", excerptId, auditStatus, auditorId);
    }

    @Transactional
    public void removeExcerpt(Long excerptId, Long handlerId) {
        PublicExcerpt excerpt = publicExcerptMapper.selectById(excerptId);
        if (excerpt == null) {
            throw new BusinessException("书摘不存在");
        }

        excerpt.setStatus(Constants.EXCERPT_STATUS_TAKEN_DOWN);
        publicExcerptMapper.updateById(excerpt);

        log.info("下架书摘: excerptId={}, handlerId={}", excerptId, handlerId);
    }

    public IPage<ExcerptReport> adminListReports(Integer status, int page, int size) {
        LambdaQueryWrapper<ExcerptReport> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(ExcerptReport::getStatus, status);
        }
        wrapper.orderByDesc(ExcerptReport::getCreatedAt);
        return excerptReportMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Transactional
    public void handleReport(Long reportId, Integer newStatus, String handleResult, Long handlerId) {
        ExcerptReport report = excerptReportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException("举报不存在");
        }
        if (report.getStatus() != Constants.EXCERPT_REPORT_PENDING) {
            throw new BusinessException("该举报已处理");
        }

        report.setStatus(newStatus);
        report.setHandleResult(handleResult);
        report.setHandlerId(handlerId);
        report.setHandledAt(LocalDateTime.now());
        excerptReportMapper.updateById(report);

        if (newStatus == Constants.EXCERPT_REPORT_HANDLED) {
            PublicExcerpt excerpt = publicExcerptMapper.selectById(report.getExcerptId());
            if (excerpt != null && excerpt.getStatus() == Constants.EXCERPT_STATUS_NORMAL) {
                excerpt.setStatus(Constants.EXCERPT_STATUS_TAKEN_DOWN);
                publicExcerptMapper.updateById(excerpt);
                log.info("处理举报并下架书摘: reportId={}, excerptId={}", reportId, report.getExcerptId());
            }
        }

        log.info("处理举报: reportId={}, status={}, handlerId={}", reportId, newStatus, handlerId);
    }

    private List<PublicExcerptVO> convertToVOList(List<PublicExcerpt> excerpts, Long currentUserId) {
        if (excerpts.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> userIds = excerpts.stream()
                .map(PublicExcerpt::getUserId)
                .collect(Collectors.toSet());

        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        Set<Long> excerptIds = excerpts.stream()
                .map(PublicExcerpt::getId)
                .collect(Collectors.toSet());

        Set<Long> likedExcerptIds = Collections.emptySet();
        Set<Long> favoritedExcerptIds = Collections.emptySet();

        if (currentUserId != null) {
            List<ExcerptLike> likes = excerptLikeMapper.selectList(new LambdaQueryWrapper<ExcerptLike>()
                    .in(ExcerptLike::getExcerptId, excerptIds)
                    .eq(ExcerptLike::getUserId, currentUserId));
            likedExcerptIds = likes.stream()
                    .map(ExcerptLike::getExcerptId)
                    .collect(Collectors.toSet());

            List<ExcerptFavorite> favorites = excerptFavoriteMapper.selectList(new LambdaQueryWrapper<ExcerptFavorite>()
                    .in(ExcerptFavorite::getExcerptId, excerptIds)
                    .eq(ExcerptFavorite::getUserId, currentUserId));
            favoritedExcerptIds = favorites.stream()
                    .map(ExcerptFavorite::getExcerptId)
                    .collect(Collectors.toSet());
        }

        List<PublicExcerptVO> voList = new ArrayList<>();
        for (PublicExcerpt excerpt : excerpts) {
            PublicExcerptVO vo = new PublicExcerptVO();
            vo.setId(excerpt.getId());
            vo.setUserId(excerpt.getUserId());
            User user = userMap.get(excerpt.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname() != null ? user.getNickname() : "书友");
                vo.setAvatar(user.getAvatar() != null ? user.getAvatar() : "");
            } else {
                vo.setNickname("书友");
                vo.setAvatar("");
            }
            vo.setBookId(excerpt.getBookId());
            vo.setBookTitle(excerpt.getBookTitle());
            vo.setBookAuthor(excerpt.getBookAuthor());
            vo.setExcerptText(excerpt.getExcerptText());
            vo.setCommentText(excerpt.getCommentText());
            vo.setLikes(excerpt.getLikes());
            vo.setFavorites(excerpt.getFavorites());
            vo.setViews(excerpt.getViews());
            vo.setStatus(excerpt.getStatus());
            vo.setAuditStatus(excerpt.getAuditStatus());
            vo.setCreatedAt(excerpt.getCreatedAt());
            vo.setLiked(likedExcerptIds.contains(excerpt.getId()));
            vo.setFavorited(favoritedExcerptIds.contains(excerpt.getId()));
            voList.add(vo);
        }

        return voList;
    }

    private PublicExcerptVO convertToVO(PublicExcerpt excerpt, Long currentUserId) {
        return convertToVOList(Collections.singletonList(excerpt), currentUserId).get(0);
    }
}
