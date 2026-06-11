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
public class OpenLibraryMetadataProvider extends AbstractMetadataProvider {

    public static final String SOURCE_CODE = "openlibrary";
    public static final String SOURCE_NAME = "Open Library";
    private static final String API_BASE = "https://openlibrary.org";
    private static final String COVER_BASE = "https://covers.openlibrary.org";

    public OpenLibraryMetadataProvider(SysConfigService sysConfigService,
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
        String url;
        if (query.getIsbn() != null && !query.getIsbn().isBlank()) {
            url = API_BASE + "/search.json?isbn=" + MetadataHttpUtil.encode(query.getIsbn().trim()) + "&limit=" + Math.max(limit, 1);
        } else {
            StringBuilder sb = new StringBuilder(API_BASE + "/search.json?");
            if (query.getTitle() != null && !query.getTitle().isBlank()) {
                sb.append("title=").append(MetadataHttpUtil.encode(query.getTitle().trim()));
            }
            if (query.getAuthor() != null && !query.getAuthor().isBlank()) {
                if (sb.charAt(sb.length() - 1) != '?') sb.append("&");
                sb.append("author=").append(MetadataHttpUtil.encode(query.getAuthor().trim()));
            }
            sb.append("&limit=").append(Math.max(limit, 1));
            url = sb.toString();
        }

        JsonNode root = MetadataHttpUtil.getJson(url, null, getTimeoutMs(), objectMapper);
        if (root == null || !root.has("docs")) {
            return Collections.emptyList();
        }

        List<BookMetadataVO> results = new ArrayList<>();
        JsonNode docs = root.get("docs");
        int count = 0;
        for (JsonNode doc : docs) {
            if (count >= limit) break;
            BookMetadataVO vo = parseDoc(doc);
            if (vo != null) {
                results.add(vo);
                count++;
            }
        }
        return results;
    }

    private BookMetadataVO parseDoc(JsonNode doc) {
        if (doc == null) return null;
        BookMetadataVO vo = new BookMetadataVO();
        vo.setSource(SOURCE_CODE);

        if (doc.has("title")) {
            vo.setTitle(doc.get("title").asText());
        }
        if (doc.has("author_name") && doc.get("author_name").isArray() && doc.get("author_name").size() > 0) {
            vo.setAuthor(doc.get("author_name").get(0).asText());
        }
        if (doc.has("isbn") && doc.get("isbn").isArray() && doc.get("isbn").size() > 0) {
            vo.setIsbn(doc.get("isbn").get(0).asText());
        }
        if (doc.has("publisher") && doc.get("publisher").isArray() && doc.get("publisher").size() > 0) {
            vo.setPublisher(doc.get("publisher").get(0).asText());
        }
        if (doc.has("publish_date")) {
            vo.setPublishDate(doc.get("publish_date").asText());
        }
        if (doc.has("language") && doc.get("language").isArray() && doc.get("language").size() > 0) {
            vo.setLanguage(doc.get("language").get(0).asText());
        }
        if (doc.has("number_of_pages_median")) {
            vo.setPageCount(doc.get("number_of_pages_median").asInt());
        } else if (doc.has("number_of_pages")) {
            vo.setPageCount(doc.get("number_of_pages").asInt());
        }
        if (doc.has("ratings_average")) {
            vo.setRating(doc.get("ratings_average").asDouble());
        }
        if (doc.has("ratings_count")) {
            vo.setRatingCount(doc.get("ratings_count").asInt());
        }
        if (doc.has("subject") && doc.get("subject").isArray()) {
            List<String> tags = new ArrayList<>();
            for (JsonNode s : doc.get("subject")) {
                tags.add(s.asText());
                if (tags.size() >= 10) break;
            }
            vo.setTags(tags);
        }
        if (doc.has("key")) {
            vo.setSourceId(doc.get("key").asText());
        }
        if (doc.has("cover_i")) {
            vo.setCoverUrl(COVER_BASE + "/b/id/" + doc.get("cover_i").asText() + "-L.jpg");
        } else if (vo.getIsbn() != null) {
            vo.setCoverUrl(COVER_BASE + "/b/isbn/" + vo.getIsbn() + "-L.jpg");
        }
        return vo;
    }
}
