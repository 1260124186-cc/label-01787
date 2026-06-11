package com.xiaoan.bookstore.controller;

import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.annotation.RateLimit;
import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.BilingualAlignmentVO;
import com.xiaoan.bookstore.dto.BilingualPairCreateDTO;
import com.xiaoan.bookstore.dto.BilingualPairUpdateDTO;
import com.xiaoan.bookstore.dto.BilingualPairVO;
import com.xiaoan.bookstore.entity.BilingualAlignment;
import com.xiaoan.bookstore.entity.BilingualPair;
import com.xiaoan.bookstore.service.BilingualService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mp")
@RequiredArgsConstructor
public class MpBilingualController {

    private final BilingualService bilingualService;

    @PostMapping("/bilingual/pairs")
    @Log("创建双语关联")
    @RateLimit(type = RateLimit.RateLimitType.USER, limit = 20, windowSeconds = 3600)
    public Result<BilingualPair> createPair(@Valid @RequestBody BilingualPairCreateDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(bilingualService.createPair(userId, dto));
    }

    @GetMapping("/bilingual/pairs")
    public Result<?> listPairs(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int size) {
        Long userId = TenantContext.getTenantId();
        return Result.success(bilingualService.listPairs(userId, page, size));
    }

    @GetMapping("/bilingual/pairs/book/{bookId}")
    public Result<List<BilingualPairVO>> getPairsByBook(@PathVariable Long bookId) {
        Long userId = TenantContext.getTenantId();
        return Result.success(bilingualService.getPairsByBook(userId, bookId));
    }

    @GetMapping("/bilingual/pairs/{id}")
    public Result<BilingualPairVO> getPairDetail(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        return Result.success(bilingualService.getPairDetail(userId, id));
    }

    @PutMapping("/bilingual/pairs/{id}")
    @Log("更新双语关联")
    public Result<BilingualPair> updatePair(@PathVariable Long id,
                                            @RequestBody BilingualPairUpdateDTO dto) {
        Long userId = TenantContext.getTenantId();
        return Result.success(bilingualService.updatePair(userId, id, dto));
    }

    @DeleteMapping("/bilingual/pairs/{id}")
    @Log("删除双语关联")
    public Result<Void> deletePair(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        bilingualService.deletePair(userId, id);
        return Result.success();
    }

    @PostMapping("/bilingual/pairs/{id}/ai-align")
    @Log("启动AI段落对齐")
    @RateLimit(type = RateLimit.RateLimitType.USER, limit = 5, windowSeconds = 3600)
    public Result<Void> startAiAlignment(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        bilingualService.startAiAlignment(userId, id);
        return Result.success();
    }

    @GetMapping("/bilingual/pairs/{id}/alignments")
    public Result<List<BilingualAlignmentVO>> listAlignments(@PathVariable Long id,
                                                              @RequestParam(required = false) Integer leftUnitIndex,
                                                              @RequestParam(required = false) Integer rightUnitIndex) {
        Long userId = TenantContext.getTenantId();
        return Result.success(bilingualService.listAlignments(userId, id, leftUnitIndex, rightUnitIndex));
    }

    @GetMapping("/bilingual/pairs/{id}/alignments/find")
    public Result<BilingualAlignmentVO> findAlignedUnit(@PathVariable Long id,
                                                        @RequestParam String side,
                                                        @RequestParam int unitIndex) {
        Long userId = TenantContext.getTenantId();
        return Result.success(bilingualService.findAlignedUnit(userId, id, side, unitIndex));
    }

    @PostMapping("/bilingual/pairs/{id}/alignments")
    @Log("添加手动对齐")
    public Result<BilingualAlignment> addManualAlignment(@PathVariable Long id,
                                                         @RequestBody Map<String, Integer> body) {
        Long userId = TenantContext.getTenantId();
        Integer leftUnitIndex = body.get("leftUnitIndex");
        Integer rightUnitIndex = body.get("rightUnitIndex");
        Integer leftParagraphIndex = body.get("leftParagraphIndex");
        Integer rightParagraphIndex = body.get("rightParagraphIndex");
        if (leftUnitIndex == null || rightUnitIndex == null) {
            throw new com.xiaoan.bookstore.exception.BusinessException("左右单元索引不能为空");
        }
        return Result.success(bilingualService.addManualAlignment(userId, id, leftUnitIndex, rightUnitIndex,
                leftParagraphIndex, rightParagraphIndex));
    }

    @DeleteMapping("/bilingual/pairs/{id}/alignments/{alignmentId}")
    @Log("删除对齐记录")
    public Result<Void> deleteAlignment(@PathVariable Long id,
                                         @PathVariable Long alignmentId) {
        Long userId = TenantContext.getTenantId();
        bilingualService.deleteAlignment(userId, id, alignmentId);
        return Result.success();
    }

    @GetMapping("/bilingual/pairs/{id}/progress")
    public Result<Map<String, Object>> getReadingProgress(@PathVariable Long id) {
        Long userId = TenantContext.getTenantId();
        return Result.success(bilingualService.getReadingProgress(userId, id));
    }
}
