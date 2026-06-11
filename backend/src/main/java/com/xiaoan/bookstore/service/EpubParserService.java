package com.xiaoan.bookstore.service;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.Resources;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.domain.SpineReference;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.epub.EpubReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EpubParserService {

    private static final Logger log = LoggerFactory.getLogger(EpubParserService.class);
    private static final Pattern IMG_SRC_PATTERN = Pattern.compile(
            "<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern LINK_HREF_PATTERN = Pattern.compile(
            "<link[^>]+href\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern BODY_PATTERN = Pattern.compile(
            "<body[^>]*>([\\s\\S]*?)</body>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEAD_PATTERN = Pattern.compile(
            "<head[^>]*>([\\s\\S]*?)</head>", Pattern.CASE_INSENSITIVE);
    private static final Pattern STYLE_PATTERN = Pattern.compile(
            "<style[^>]*>([\\s\\S]*?)</style>", Pattern.CASE_INSENSITIVE);

    public int getChapterCount(Path filePath) {
        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            Book epubBook = new EpubReader().readEpub(fis);
            return epubBook.getSpine().size();
        } catch (Exception e) {
            log.error("获取EPUB章节数失败", e);
            return 0;
        }
    }

    public List<Map<String, Object>> getToc(Path filePath) {
        List<Map<String, Object>> toc = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            Book epubBook = new EpubReader().readEpub(fis);
            List<TOCReference> tocReferences = epubBook.getTableOfContents().getTocReferences();
            if (tocReferences != null && !tocReferences.isEmpty()) {
                buildTocRecursive(tocReferences, toc, epubBook);
            } else {
                toc = buildTocFromSpine(epubBook);
            }
        } catch (Exception e) {
            log.error("获取EPUB目录失败", e);
        }
        return toc;
    }

    private void buildTocRecursive(List<TOCReference> refs, List<Map<String, Object>> result, Book epubBook) {
        for (TOCReference ref : refs) {
            Map<String, Object> entry = new HashMap<>();
            String title = ref.getTitle();
            if (title == null || title.trim().isEmpty()) {
                title = "未命名章节";
            }
            entry.put("title", title);
            entry.put("chapterIndex", resolveChapterIndex(epubBook, ref));
            if (ref.getChildren() != null && !ref.getChildren().isEmpty()) {
                List<Map<String, Object>> children = new ArrayList<>();
                buildTocRecursive(ref.getChildren(), children, epubBook);
                if (!children.isEmpty()) {
                    entry.put("children", children);
                }
            }
            result.add(entry);
        }
    }

    private List<Map<String, Object>> buildTocFromSpine(Book epubBook) {
        List<Map<String, Object>> toc = new ArrayList<>();
        Spine spine = epubBook.getSpine();
        for (int i = 0; i < spine.size(); i++) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("title", "第 " + (i + 1) + " 章");
            entry.put("chapterIndex", i);
            toc.add(entry);
        }
        return toc;
    }

    public String getChapterHtml(Path filePath, int chapterIndex) {
        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            Book epubBook = new EpubReader().readEpub(fis);
            Spine spine = epubBook.getSpine();
            if (chapterIndex < 0 || chapterIndex >= spine.size()) {
                return "";
            }
            SpineReference ref = spine.getSpineReferences().get(chapterIndex);
            if (ref.getResource() == null || ref.getResource().getData() == null) {
                return "";
            }
            String html = new String(ref.getResource().getData(), "UTF-8");
            String cssStyles = extractStyles(html, epubBook, ref.getResource());
            String bodyContent = extractBody(html);
            bodyContent = inlineImages(bodyContent, epubBook, ref.getResource());
            if (cssStyles != null && !cssStyles.isEmpty()) {
                bodyContent = "<style>" + cssStyles + "</style>" + bodyContent;
            }
            return bodyContent;
        } catch (Exception e) {
            log.error("获取EPUB章节内容失败: chapterIndex={}", chapterIndex, e);
            return "";
        }
    }

    private String extractStyles(String html, Book epubBook, Resource currentResource) {
        StringBuilder styles = new StringBuilder();
        Resources resources = epubBook.getResources();
        String baseHref = currentResource != null ? getParentHref(currentResource.getHref()) : "";

        Matcher headMatcher = HEAD_PATTERN.matcher(html);
        if (headMatcher.find()) {
            String head = headMatcher.group(1);
            Matcher styleMatcher = STYLE_PATTERN.matcher(head);
            while (styleMatcher.find()) {
                styles.append(styleMatcher.group(1)).append("\n");
            }
            Matcher linkMatcher = LINK_HREF_PATTERN.matcher(head);
            while (linkMatcher.find()) {
                String href = linkMatcher.group(1);
                try {
                    String cssContent = loadCssResource(resources, baseHref, href);
                    if (cssContent != null) {
                        styles.append(cssContent).append("\n");
                    }
                } catch (Exception e) {
                    log.debug("加载CSS资源失败: {}", href);
                }
            }
        }
        return styles.toString();
    }

    private String loadCssResource(Resources resources, String baseHref, String href) {
        if (href == null || href.isEmpty() || href.startsWith("http://") || href.startsWith("https://")) {
            return null;
        }
        int hashIdx = href.indexOf('#');
        if (hashIdx > 0) {
            href = href.substring(0, hashIdx);
        }
        Resource cssResource = findResource(resources, baseHref, href);
        if (cssResource != null) {
            try {
                byte[] data = cssResource.getData();
                if (data != null) {
                    return new String(data, "UTF-8");
                }
            } catch (Exception e) {
                log.debug("解析CSS资源失败: {}", href);
            }
        }
        return null;
    }

    private String inlineImages(String html, Book epubBook, Resource currentResource) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        Resources resources = epubBook.getResources();
        String baseHref = currentResource != null ? getParentHref(currentResource.getHref()) : "";

        StringBuffer sb = new StringBuffer();
        Matcher matcher = IMG_SRC_PATTERN.matcher(html);
        while (matcher.find()) {
            String imgTag = matcher.group(0);
            String src = matcher.group(1);
            String replacement = imgTag;
            try {
                String dataUri = loadImageAsDataUri(resources, baseHref, src);
                if (dataUri != null) {
                    replacement = imgTag.replace(src, dataUri);
                }
            } catch (Exception e) {
                log.debug("内联图片失败: {}", src);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String loadImageAsDataUri(Resources resources, String baseHref, String src) {
        if (src == null || src.isEmpty()
                || src.startsWith("http://") || src.startsWith("https://")
                || src.startsWith("data:")) {
            return null;
        }
        int queryIdx = src.indexOf('?');
        int hashIdx = src.indexOf('#');
        int endIdx = src.length();
        if (queryIdx > 0) endIdx = Math.min(endIdx, queryIdx);
        if (hashIdx > 0) endIdx = Math.min(endIdx, hashIdx);
        String cleanSrc = src.substring(0, endIdx);

        Resource imgResource = findResource(resources, baseHref, cleanSrc);
        if (imgResource != null) {
            try {
                byte[] data = imgResource.getData();
                if (data != null) {
                    String mediaType = imgResource.getMediaType() != null
                            ? imgResource.getMediaType().getName()
                            : guessMediaType(cleanSrc);
                    if (mediaType != null) {
                        String base64 = Base64.getEncoder().encodeToString(data);
                        return "data:" + mediaType + ";base64," + base64;
                    }
                }
            } catch (Exception e) {
                log.debug("加载图片资源失败: {}", cleanSrc);
            }
        }
        return null;
    }

    private Resource findResource(Resources resources, String baseHref, String href) {
        if (resources == null || href == null) {
            return null;
        }
        Resource resource = resources.getByHref(href);
        if (resource != null) {
            return resource;
        }
        if (baseHref != null && !baseHref.isEmpty()) {
            String combined = baseHref + href;
            resource = resources.getByHref(combined);
            if (resource != null) {
                return resource;
            }
        }
        String simpleName = href;
        int slashIdx = href.lastIndexOf('/');
        if (slashIdx >= 0) {
            simpleName = href.substring(slashIdx + 1);
        }
        for (Resource r : resources.getAll()) {
            if (r != null && r.getHref() != null && r.getHref().endsWith(simpleName)) {
                return r;
            }
        }
        return null;
    }

    private String getParentHref(String href) {
        if (href == null) return "";
        int idx = href.lastIndexOf('/');
        return idx >= 0 ? href.substring(0, idx + 1) : "";
    }

    private String guessMediaType(String fileName) {
        if (fileName == null) return null;
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".bmp")) return "image/bmp";
        return "image/png";
    }

    private int resolveChapterIndex(Book epubBook, TOCReference tocRef) {
        if (tocRef == null || tocRef.getResource() == null) {
            return 0;
        }
        String href = tocRef.getResource().getHref();
        if (href == null) {
            return 0;
        }
        int hashIdx = href.indexOf('#');
        String cleanHref = hashIdx > 0 ? href.substring(0, hashIdx) : href;

        Spine spine = epubBook.getSpine();
        for (int i = 0; i < spine.size(); i++) {
            SpineReference spineRef = spine.getSpineReferences().get(i);
            if (spineRef.getResource() != null && spineRef.getResource().getHref() != null) {
                String spineHref = spineRef.getResource().getHref();
                if (spineHref.equals(cleanHref) || spineHref.equals(href)) {
                    return i;
                }
            }
        }
        return 0;
    }

    private String extractBody(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        Matcher matcher = BODY_PATTERN.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        int bodyStart = html.toLowerCase().indexOf("<body");
        if (bodyStart >= 0) {
            int contentStart = html.indexOf('>', bodyStart) + 1;
            int bodyEnd = html.toLowerCase().indexOf("</body>");
            if (contentStart > 0 && bodyEnd > contentStart) {
                return html.substring(contentStart, bodyEnd).trim();
            }
        }
        int htmlStart = html.toLowerCase().indexOf("<html");
        if (htmlStart < 0) {
            return html.trim();
        }
        return "";
    }
}
