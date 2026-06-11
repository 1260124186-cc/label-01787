package com.xiaoan.bookstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.dto.SearchResultDTO;
import com.xiaoan.bookstore.entity.Annotation;
import com.xiaoan.bookstore.entity.Book;
import com.xiaoan.bookstore.entity.BookPageText;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.AnnotationMapper;
import com.xiaoan.bookstore.mapper.BookMapper;
import com.xiaoan.bookstore.mapper.BookPageTextMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FullTextSearchService {

    private static final Logger log = LoggerFactory.getLogger(FullTextSearchService.class);

    private final BookPageTextMapper bookPageTextMapper;
    private final BookMapper bookMapper;
    private final AnnotationMapper annotationMapper;

    public List<SearchResultDTO> search(Long userId, String keyword, String scope, Long bookId, int page, int size) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BusinessException("搜索关键词不能为空");
        }

        String searchKeyword = keyword.trim();
        int offset = (page - 1) * size;

        List<SearchResultDTO> results = new ArrayList<>();

        if (Constants.SEARCH_SCOPE_NOTES.equals(scope)) {
            results = searchAnnotations(userId, searchKeyword, bookId, offset, size);
        } else if (Constants.SEARCH_SCOPE_BOOK.equals(scope) && bookId != null) {
            results = searchInSingleBook(userId, bookId, searchKeyword, offset, size);
        } else {
            results = searchAllBooks(userId, searchKeyword, offset, size);
        }

        return results;
    }

    private List<SearchResultDTO> searchAllBooks(Long userId, String keyword, int offset, int size) {
        List<BookPageText> pageTexts = bookPageTextMapper.searchByFullText(userId, keyword, offset, size);

        Map<Long, List<BookPageText>> groupedByBook = pageTexts.stream()
                .collect(Collectors.groupingBy(BookPageText::getBookId));

        List<SearchResultDTO> results = new ArrayList<>();
        for (Map.Entry<Long, List<BookPageText>> entry : groupedByBook.entrySet()) {
            Long bookId = entry.getKey();
            Book book = bookMapper.selectById(bookId);
            if (book == null) continue;

            SearchResultDTO dto = new SearchResultDTO();
            dto.setBookId(bookId);
            dto.setBookTitle(book.getTitle());
            dto.setBookAuthor(book.getAuthor());
            dto.setBookFormat(book.getBookFormat());

            int totalCount = bookPageTextMapper.countByFullText(userId, keyword);
            dto.setTotalMatches(totalCount);

            List<SearchResultDTO.SearchMatch> matches = entry.getValue().stream()
                    .map(pt -> createMatchFromPageText(pt, keyword))
                    .filter(Objects::nonNull)
                    .limit(5)
                    .toList();
            dto.setMatches(matches);

            results.add(dto);
        }

        return results;
    }

    private List<SearchResultDTO> searchInSingleBook(Long userId, Long bookId, String keyword, int offset, int size) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException("书籍不存在");
        }

        List<BookPageText> pageTexts = bookPageTextMapper.searchByBookAndFullText(userId, bookId, keyword, offset, size);

        SearchResultDTO dto = new SearchResultDTO();
        dto.setBookId(bookId);
        dto.setBookTitle(book.getTitle());
        dto.setBookAuthor(book.getAuthor());
        dto.setBookFormat(book.getBookFormat());

        int totalCount = bookPageTextMapper.countByBookAndFullText(userId, bookId, keyword);
        dto.setTotalMatches(totalCount);

        List<SearchResultDTO.SearchMatch> matches = pageTexts.stream()
                .map(pt -> createMatchFromPageText(pt, keyword))
                .filter(Objects::nonNull)
                .toList();
        dto.setMatches(matches);

        return Collections.singletonList(dto);
    }

    private List<SearchResultDTO> searchAnnotations(Long userId, String keyword, Long bookId, int offset, int size) {
        LambdaQueryWrapper<Annotation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Annotation::getUserId, userId);
        if (bookId != null) {
            wrapper.eq(Annotation::getBookId, bookId);
        }
        wrapper.and(w -> w.like(Annotation::getContent, keyword)
                .or().like(Annotation::getSelectedText, keyword));
        wrapper.orderByDesc(Annotation::getCreatedAt);
        wrapper.last("LIMIT " + offset + ", " + size);

        List<Annotation> annotations = annotationMapper.selectList(wrapper);

        Map<Long, List<Annotation>> groupedByBook = annotations.stream()
                .collect(Collectors.groupingBy(Annotation::getBookId));

        List<SearchResultDTO> results = new ArrayList<>();
        for (Map.Entry<Long, List<Annotation>> entry : groupedByBook.entrySet()) {
            Long bId = entry.getKey();
            Book book = bookMapper.selectById(bId);
            if (book == null) continue;

            SearchResultDTO dto = new SearchResultDTO();
            dto.setBookId(bId);
            dto.setBookTitle(book.getTitle());
            dto.setBookAuthor(book.getAuthor());
            dto.setBookFormat(book.getBookFormat());

            LambdaQueryWrapper<Annotation> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.eq(Annotation::getUserId, userId);
            countWrapper.eq(Annotation::getBookId, bId);
            countWrapper.and(w -> w.like(Annotation::getContent, keyword)
                    .or().like(Annotation::getSelectedText, keyword));
            dto.setTotalMatches(annotationMapper.selectCount(countWrapper).intValue());

            List<SearchResultDTO.SearchMatch> matches = entry.getValue().stream()
                    .map(a -> createMatchFromAnnotation(a, keyword))
                    .filter(Objects::nonNull)
                    .toList();
            dto.setMatches(matches);

            results.add(dto);
        }

        return results;
    }

    private SearchResultDTO.SearchMatch createMatchFromPageText(BookPageText pageText, String keyword) {
        String text = pageText.getPageText();
        if (text == null || text.isEmpty()) return null;

        int index = findMatchIndex(text, keyword);
        if (index < 0) return null;

        int contextLen = Constants.SEARCH_HIGHLIGHT_CONTEXT;
        int start = Math.max(0, index - contextLen);
        int end = Math.min(text.length(), index + keyword.length() + contextLen);

        String snippet = text.substring(start, end);
        if (start > 0) snippet = "..." + snippet;
        if (end < text.length()) snippet = snippet + "...";

        String highlightSnippet = buildHighlightSnippet(text, keyword, start, end);

        SearchResultDTO.SearchMatch match = new SearchResultDTO.SearchMatch();
        match.setId(pageText.getId());
        match.setPageNum(pageText.getPageNum());
        match.setSnippet(snippet);
        match.setHighlightSnippet(highlightSnippet);
        match.setMatchStart(index);
        match.setMatchEnd(index + keyword.length());
        match.setSourceType("book");

        return match;
    }

    private SearchResultDTO.SearchMatch createMatchFromAnnotation(Annotation annotation, String keyword) {
        String text = annotation.getContent() + " " + (annotation.getSelectedText() != null ? annotation.getSelectedText() : "");
        if (text == null || text.isEmpty()) return null;

        int index = findMatchIndex(text, keyword);
        if (index < 0) return null;

        int contextLen = Constants.SEARCH_HIGHLIGHT_CONTEXT;
        int start = Math.max(0, index - contextLen);
        int end = Math.min(text.length(), index + keyword.length() + contextLen);

        String snippet = text.substring(start, end);
        if (start > 0) snippet = "..." + snippet;
        if (end < text.length()) snippet = snippet + "...";

        String highlightSnippet = buildHighlightSnippet(text, keyword, start, end);

        SearchResultDTO.SearchMatch match = new SearchResultDTO.SearchMatch();
        match.setId(annotation.getId());
        match.setPageNum(annotation.getPageNum());
        match.setSnippet(snippet);
        match.setHighlightSnippet(highlightSnippet);
        match.setMatchStart(index);
        match.setMatchEnd(index + keyword.length());
        match.setSourceType("annotation");
        match.setAnnotationId(annotation.getId());
        match.setAnnotationType(annotation.getType() == 1 ? "comment" : "note");

        return match;
    }

    private int findMatchIndex(String text, String keyword) {
        if (text == null || keyword == null) return -1;
        return text.toLowerCase().indexOf(keyword.toLowerCase());
    }

    private String buildHighlightSnippet(String text, String keyword, int start, int end) {
        String snippet = text.substring(start, end);
        String lowerSnippet = snippet.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();

        StringBuilder result = new StringBuilder();
        int idx = 0;
        while (idx < snippet.length()) {
            int matchIdx = lowerSnippet.indexOf(lowerKeyword, idx);
            if (matchIdx < 0) {
                result.append(escapeHtml(snippet.substring(idx)));
                break;
            }
            result.append(escapeHtml(snippet.substring(idx, matchIdx)));
            result.append("<em class=\"highlight\">");
            result.append(escapeHtml(snippet.substring(matchIdx, matchIdx + keyword.length())));
            result.append("</em>");
            idx = matchIdx + keyword.length();
        }

        if (start > 0) result.insert(0, "...");
        if (end < text.length()) result.append("...");

        return result.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    public List<SearchResultDTO.SearchMatch> getPageMatches(Long userId, Long bookId, String keyword) {
        List<BookPageText> pageTexts = bookPageTextMapper.searchByBookAndFullText(userId, bookId, keyword, 0, 1000);
        return pageTexts.stream()
                .map(pt -> {
                    SearchResultDTO.SearchMatch match = new SearchResultDTO.SearchMatch();
                    match.setPageNum(pt.getPageNum());
                    match.setMatchStart(findMatchIndex(pt.getPageText(), keyword));
                    match.setMatchEnd(match.getMatchStart() + keyword.length());
                    return match;
                })
                .filter(m -> m.getMatchStart() >= 0)
                .toList();
    }
}
