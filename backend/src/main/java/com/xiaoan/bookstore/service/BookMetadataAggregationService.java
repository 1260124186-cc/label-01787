package com.xiaoan.bookstore.service;

import com.xiaoan.bookstore.dto.BookMetadataVO;
import com.xiaoan.bookstore.dto.MetadataSearchQuery;
import com.xiaoan.bookstore.dto.MetadataSourceStatusVO;
import com.xiaoan.bookstore.entity.Book;
import com.xiaoan.bookstore.service.metadata.AbstractMetadataProvider;
import com.xiaoan.bookstore.service.metadata.BookMetadataProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookMetadataAggregationService {

    private static final Logger log = LoggerFactory.getLogger(BookMetadataAggregationService.class);

    private final List<BookMetadataProvider> providers;

    public BookMetadataVO searchBestMatch(String title, String author, String isbn) {
        MetadataSearchQuery query = MetadataSearchQuery.of(title, author, isbn);
        return searchBestMatch(query);
    }

    public BookMetadataVO searchBestMatch(MetadataSearchQuery query) {
        if (query == null || query.isEmpty()) {
            return null;
        }

        List<BookMetadataProvider> sortedProviders = providers.stream()
                .filter(BookMetadataProvider::isEnabled)
                .sorted(Comparator.comparingInt(BookMetadataProvider::getPriority))
                .collect(Collectors.toList());

        BookMetadataVO best = null;
        int bestScore = -1;

        for (BookMetadataProvider provider : sortedProviders) {
            try {
                BookMetadataVO result = provider.searchOne(query);
                if (result != null) {
                    int score = scoreMetadata(result, query);
                    log.debug("元数据源 {} 返回结果，匹配分数: {}", provider.getSourceCode(), score);
                    if (score > bestScore) {
                        bestScore = score;
                        best = result;
                    }
                    if (score >= 80) {
                        break;
                    }
                }
            } catch (Exception e) {
                log.warn("元数据源 {} 查询失败: {}", provider.getSourceCode(), e.getMessage());
            }
        }

        if (best != null) {
            enrichWithSecondarySources(best, query, sortedProviders);
        }

        return best;
    }

    public List<BookMetadataVO> searchAll(MetadataSearchQuery query, int limitPerSource) {
        if (query == null || query.isEmpty()) {
            return Collections.emptyList();
        }

        List<BookMetadataProvider> sortedProviders = providers.stream()
                .filter(BookMetadataProvider::isEnabled)
                .sorted(Comparator.comparingInt(BookMetadataProvider::getPriority))
                .collect(Collectors.toList());

        Set<String> seenKeys = new HashSet<>();
        List<BookMetadataVO> allResults = new ArrayList<>();

        for (BookMetadataProvider provider : sortedProviders) {
            try {
                List<BookMetadataVO> results = provider.search(query, limitPerSource);
                for (BookMetadataVO r : results) {
                    String key = buildDedupKey(r);
                    if (!seenKeys.contains(key)) {
                        seenKeys.add(key);
                        allResults.add(r);
                    }
                }
            } catch (Exception e) {
                log.warn("元数据源 {} 批量查询失败: {}", provider.getSourceCode(), e.getMessage());
            }
        }

        allResults.sort((a, b) -> Integer.compare(scoreMetadata(b, query), scoreMetadata(a, query)));
        return allResults;
    }

    public void applyMetadataToBook(Book book, BookMetadataVO meta) {
        if (book == null || meta == null) return;

        if (book.getTitle() == null || book.getTitle().isBlank()) {
            book.setTitle(meta.getTitle());
        }
        if (book.getAuthor() == null || book.getAuthor().isBlank()) {
            book.setAuthor(meta.getAuthor());
        }
        if (book.getIsbn() == null || book.getIsbn().isBlank()) {
            book.setIsbn(meta.getIsbn());
        }
        if ((book.getCoverThumbnail() == null || book.getCoverThumbnail().isBlank())
                && (book.getCoverUrl() == null || book.getCoverUrl().isBlank())) {
            book.setCoverUrl(meta.getCoverUrl());
        }
        if (book.getDescription() == null || book.getDescription().isBlank()) {
            book.setDescription(meta.getDescription());
        }
        if (book.getRating() == null) {
            book.setRating(meta.getRating());
        }
        if (book.getRatingCount() == null) {
            book.setRatingCount(meta.getRatingCount());
        }
        if (book.getTags() == null || book.getTags().isBlank()) {
            if (meta.getTags() != null && !meta.getTags().isEmpty()) {
                book.setTags(String.join(",", meta.getTags()));
            }
        }
        if (book.getPublisher() == null || book.getPublisher().isBlank()) {
            book.setPublisher(meta.getPublisher());
        }
        if (book.getPublishDate() == null || book.getPublishDate().isBlank()) {
            book.setPublishDate(meta.getPublishDate());
        }
        if (book.getLanguage() == null || book.getLanguage().isBlank()) {
            book.setLanguage(meta.getLanguage());
        }
        if (book.getPageCount() == null || book.getPageCount() == 0) {
            if (meta.getPageCount() != null && meta.getPageCount() > 0) {
                book.setPageCount(meta.getPageCount());
            }
        }
        book.setMetadataSource(meta.getSource());
        book.setMetadataFetchedAt(LocalDateTime.now());
    }

    public List<MetadataSourceStatusVO> getAllSourceStatus() {
        List<MetadataSourceStatusVO> list = new ArrayList<>();
        for (BookMetadataProvider provider : providers) {
            if (provider instanceof AbstractMetadataProvider) {
                list.add(((AbstractMetadataProvider) provider).getStatus());
            } else {
                MetadataSourceStatusVO vo = new MetadataSourceStatusVO();
                vo.setSourceCode(provider.getSourceCode());
                vo.setSourceName(provider.getSourceName());
                vo.setEnabled(provider.isEnabled());
                vo.setPriority(provider.getPriority());
                list.add(vo);
            }
        }
        list.sort(Comparator.comparingInt(MetadataSourceStatusVO::getPriority));
        return list;
    }

    private void enrichWithSecondarySources(BookMetadataVO primary, MetadataSearchQuery query,
                                            List<BookMetadataProvider> providers) {
        for (BookMetadataProvider provider : providers) {
            if (provider.getSourceCode().equals(primary.getSource())) {
                continue;
            }
            try {
                BookMetadataVO secondary = provider.searchOne(query);
                if (secondary != null) {
                    if (primary.getDescription() == null || primary.getDescription().isBlank()) {
                        primary.setDescription(secondary.getDescription());
                    }
                    if (primary.getRating() == null && secondary.getRating() != null) {
                        primary.setRating(secondary.getRating());
                        primary.setRatingCount(secondary.getRatingCount());
                    }
                    if ((primary.getTags() == null || primary.getTags().isEmpty())
                            && secondary.getTags() != null && !secondary.getTags().isEmpty()) {
                        primary.setTags(secondary.getTags());
                    }
                    if (primary.getCoverUrl() == null || primary.getCoverUrl().isBlank()) {
                        primary.setCoverUrl(secondary.getCoverUrl());
                    }
                    if ((primary.getPublisher() == null || primary.getPublisher().isBlank())
                            && secondary.getPublisher() != null) {
                        primary.setPublisher(secondary.getPublisher());
                    }
                    if ((primary.getPublishDate() == null || primary.getPublishDate().isBlank())
                            && secondary.getPublishDate() != null) {
                        primary.setPublishDate(secondary.getPublishDate());
                    }
                    if ((primary.getLanguage() == null || primary.getLanguage().isBlank())
                            && secondary.getLanguage() != null) {
                        primary.setLanguage(secondary.getLanguage());
                    }
                }
            } catch (Exception e) {
                log.debug("补充元数据失败: source={}", provider.getSourceCode());
            }
        }
    }

    private int scoreMetadata(BookMetadataVO meta, MetadataSearchQuery query) {
        int score = 0;

        if (query.getIsbn() != null && !query.getIsbn().isBlank()
                && meta.getIsbn() != null && !meta.getIsbn().isBlank()) {
            String qIsbn = query.getIsbn().replaceAll("[^0-9]", "");
            String mIsbn = meta.getIsbn().replaceAll("[^0-9]", "");
            if (qIsbn.equals(mIsbn)) {
                score += 100;
            } else if (qIsbn.length() >= 10 && mIsbn.length() >= 10
                    && qIsbn.substring(Math.max(0, qIsbn.length() - 10))
                    .equals(mIsbn.substring(Math.max(0, mIsbn.length() - 10)))) {
                score += 90;
            }
        }

        if (query.getTitle() != null && !query.getTitle().isBlank()
                && meta.getTitle() != null && !meta.getTitle().isBlank()) {
            String qt = query.getTitle().toLowerCase().trim();
            String mt = meta.getTitle().toLowerCase().trim();
            if (qt.equals(mt)) {
                score += 40;
            } else if (mt.contains(qt) || qt.contains(mt)) {
                score += 25;
            } else {
                double sim = similarity(qt, mt);
                score += (int) (sim * 20);
            }
        }

        if (query.getAuthor() != null && !query.getAuthor().isBlank()
                && meta.getAuthor() != null && !meta.getAuthor().isBlank()) {
            String qa = query.getAuthor().toLowerCase().trim();
            String ma = meta.getAuthor().toLowerCase().trim();
            if (qa.equals(ma)) {
                score += 30;
            } else if (ma.contains(qa) || qa.contains(ma)) {
                score += 15;
            } else {
                String[] qaParts = qa.split("[\\s,]+");
                String[] maParts = ma.split("[\\s,]+");
                for (String p1 : qaParts) {
                    for (String p2 : maParts) {
                        if (p1.length() >= 2 && p2.length() >= 2 && p1.equals(p2)) {
                            score += 8;
                        }
                    }
                }
            }
        }

        if (meta.getCoverUrl() != null && !meta.getCoverUrl().isBlank()) score += 5;
        if (meta.getDescription() != null && !meta.getDescription().isBlank()) score += 5;
        if (meta.getRating() != null) score += 3;
        if (meta.getTags() != null && !meta.getTags().isEmpty()) score += 2;

        return Math.min(score, 100);
    }

    private double similarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0;
        if (s1.equals(s2)) return 1.0;
        if (s1.length() < 2 || s2.length() < 2) return 0;

        Set<String> set1 = new HashSet<>();
        Set<String> set2 = new HashSet<>();
        for (int i = 0; i < s1.length() - 1; i++) {
            set1.add(s1.substring(i, i + 2));
        }
        for (int i = 0; i < s2.length() - 1; i++) {
            set2.add(s2.substring(i, i + 2));
        }
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        if (union.isEmpty()) return 0;
        Set<String> inter = new HashSet<>(set1);
        inter.retainAll(set2);
        return (double) inter.size() / union.size();
    }

    private String buildDedupKey(BookMetadataVO vo) {
        StringBuilder sb = new StringBuilder();
        if (vo.getIsbn() != null && !vo.getIsbn().isBlank()) {
            sb.append("isbn:").append(vo.getIsbn().replaceAll("[^0-9]", ""));
        } else {
            if (vo.getTitle() != null) sb.append(vo.getTitle().toLowerCase().trim());
            sb.append("|");
            if (vo.getAuthor() != null) sb.append(vo.getAuthor().toLowerCase().trim());
        }
        return sb.toString();
    }

    public String formatDateTime(LocalDateTime dt) {
        if (dt == null) return null;
        return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
