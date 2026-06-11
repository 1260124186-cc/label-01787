package com.xiaoan.bookstore.controller;

import com.xiaoan.bookstore.common.Result;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.dto.IndexStatusVO;
import com.xiaoan.bookstore.dto.SearchResultDTO;
import com.xiaoan.bookstore.service.BookIndexService;
import com.xiaoan.bookstore.service.FullTextSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mp")
@RequiredArgsConstructor
public class MpSearchController {

    private final FullTextSearchService fullTextSearchService;
    private final BookIndexService bookIndexService;

    @GetMapping("/search")
    public Result<List<SearchResultDTO>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "all") String scope,
            @RequestParam(required = false) Long bookId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = TenantContext.getTenantId();
        List<SearchResultDTO> results = fullTextSearchService.search(userId, keyword, scope, bookId, page, size);
        return Result.success(results);
    }

    @GetMapping("/search/page-matches")
    public Result<List<SearchResultDTO.SearchMatch>> getPageMatches(
            @RequestParam Long bookId,
            @RequestParam String keyword) {
        Long userId = TenantContext.getTenantId();
        List<SearchResultDTO.SearchMatch> matches = fullTextSearchService.getPageMatches(userId, bookId, keyword);
        return Result.success(matches);
    }

    @GetMapping("/search/index-status/{bookId}")
    public Result<IndexStatusVO> getBookIndexStatus(@PathVariable Long bookId) {
        IndexStatusVO status = bookIndexService.getBookIndexStatus(bookId);
        return Result.success(status);
    }

    @PostMapping("/search/rebuild-index/{bookId}")
    public Result<Void> rebuildIndex(@PathVariable Long bookId) {
        bookIndexService.rebuildIndex(bookId);
        return Result.success();
    }
}
