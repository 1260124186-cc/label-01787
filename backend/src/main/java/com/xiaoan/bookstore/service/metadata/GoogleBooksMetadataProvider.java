package com.xiaoan.bookstore.service.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoan.bookstore.dto.BookMetadataVO;
import com.xiaoan.bookstore.dto.MetadataSearchQuery;
import com.xiaoan.bookstore.service.SysConfigService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GoogleBooksMetadataProvider extends AbstractMetadataProvider {

    public static final String SOURCE_CODE = "google_books";
    public static final String SOURCE_NAME = "Google Books";
    private static final String API_BASE = "https://www.googleapis.com/books/v1/volumes";

    public GoogleBooksMetadataProvider(SysConfigService sysConfigService,
                                        StringRedisTemplate redisTemplate,
                                        ObjectMapper objectMapper,
                                        BookMetadataCache cache) {
        super(sysConfigService, redisTemplate, objectMapper, cache);
    }

    @Override
    public String getSourceCode() {
        return SOURCE_CODE;
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    @Override
    protected BookMetadataVO doSearchOne(MetadataSearchQuery query) throws Exception {
        List<BookMetadataVO> list = doSearch(query, 1);
        return list != null && !list.isEmpty() ? list.get(0) : null;
    }

    @Override
    protected List<BookMetadataVO> doSearch(MetadataSearchQuery query, int limit) throws Exception {
        StringBuilder q = new StringBuilder();
        if (query.getIsbn() != null && !query.getIsbn().isBlank()) {
            q.append("isbn:").append(query.getIsbn().trim());
        } else {
            if (query.getTitle() != null && !query.getTitle().isBlank()) {
                q.append("intitle:").append(query.getTitle().trim());
            }
            if (query.getAuthor() != null && !query.getAuthor().isBlank()) {
                if (q.length() > 0) q.append("+");
                q.append("inauthor:").append(query.getAuthor().trim());
            }
        }
        if (q.length() == 0) {
            return Collections.emptyList();
        }

        StringBuilder url = new StringBuilder(API_BASE + "?q=").append(MetadataHttpUtil.encode(q.toString()));
        url.append("&maxResults=").append(Math.min(Math.max(limit, 1), 40));
        url.append("&printType=books");
        String apiKey = getApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            url.append("&key=").append(apiKey);
        }

        JsonNode root = MetadataHttpUtil.getJson(url.toString(), null, getTimeoutMs(), objectMapper);
        if (root == null || !root.has("items")) {
            return Collections.emptyList();
        }

        List<BookMetadataVO> results = new ArrayList<>();
        int count = 0;
        for (JsonNode item : root.get("items")) {
            if (count >= limit) break;
            BookMetadataVO vo = parseItem(item);
            if (vo != null) {
                results.add(vo);
                count++;
            }
        }
        return results;
    }

    private BookMetadataVO parseItem(JsonNode item) {
        if (item == null || !item.has("volumeInfo")) return null;
        JsonNode info = item.get("volumeInfo");
        BookMetadataVO vo = new BookMetadataVO();
        vo.setSource(SOURCE_CODE);

        if (item.has("id")) {
            vo.setSourceId(item.get("id").asText());
        }
        if (info.has("title")) {
            vo.setTitle(info.get("title").asText());
        }
        if (info.has("subtitle")) {
            String subtitle = info.get("subtitle").asText();
            if (vo.getTitle() != null && !vo.getTitle().contains(subtitle)) {
                vo.setTitle(vo.getTitle() + ": " + subtitle);
            }
        }
        if (info.has("authors") && info.get("authors").isArray() && info.get("authors").size() > 0) {
            vo.setAuthor(info.get("authors").get(0).asText());
        }
        if (info.has("industryIdentifiers") && info.get("industryIdentifiers").isArray()) {
            for (JsonNode id : info.get("industryIdentifiers")) {
                String type = id.has("type") ? id.get("type").asText() : "";
                if ("ISBN_13".equals(type) || "ISBN_10".equals(type)) {
                    vo.setIsbn(id.get("identifier").asText());
                    break;
                }
            }
        }
        if (info.has("publisher")) {
            vo.setPublisher(info.get("publisher").asText());
        }
        if (info.has("publishedDate")) {
            vo.setPublishDate(info.get("publishedDate").asText());
        }
        if (info.has("language")) {
            vo.setLanguage(info.get("language").asText());
        }
        if (info.has("pageCount")) {
            vo.setPageCount(info.get("pageCount").asInt());
        }
        if (info.has("averageRating")) {
            vo.setRating(info.get("averageRating").asDouble());
        }
        if (info.has("ratingsCount")) {
            vo.setRatingCount(info.get("ratingsCount").asInt());
        }
        if (info.has("description")) {
            String desc = info.get("description").asText();
            if (desc.length() > 1000) {
                desc = desc.substring(0, 1000) + "...";
            }
            vo.setDescription(desc);
        }
        if (info.has("categories") && info.get("categories").isArray()) {
            List<String> tags = new ArrayList<>();
            for (JsonNode c : info.get("categories")) {
                tags.add(c.asText());
                if (tags.size() >= 10) break;
            }
            vo.setTags(tags);
        }
        if (info.has("imageLinks")) {
            JsonNode links = info.get("imageLinks");
            if (links.has("thumbnail")) {
                String cover = links.get("thumbnail").asText();
                cover = cover.replace("http://", "https://");
                vo.setCoverUrl(cover);
            } else if (links.has("smallThumbnail")) {
                String cover = links.get("smallThumbnail").asText();
                cover = cover.replace("http://", "https://");
                vo.setCoverUrl(cover);
            }
        }
        return vo;
    }
}
