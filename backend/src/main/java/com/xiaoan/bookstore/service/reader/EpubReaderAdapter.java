package com.xiaoan.bookstore.service.reader;

import com.xiaoan.bookstore.service.EpubParserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EpubReaderAdapter implements ReaderAdapter {

    private static final Logger log = LoggerFactory.getLogger(EpubReaderAdapter.class);

    private final EpubParserService epubParserService;

    @Override
    public String getFormat() {
        return "epub";
    }

    @Override
    public String getStreamType() {
        return STREAM_TYPE_HTML;
    }

    @Override
    public List<Map<String, Object>> getToc(String filePath) {
        return epubParserService.getToc(Paths.get(filePath));
    }

    @Override
    public String getUnitContent(String filePath, int chapterIndex) {
        return epubParserService.getChapterHtml(Paths.get(filePath), chapterIndex);
    }

    @Override
    public byte[] getUnitImage(String filePath, int index) {
        return new byte[0];
    }

    @Override
    public int getTotalUnits(String filePath) {
        return epubParserService.getChapterCount(Paths.get(filePath));
    }

    @Override
    public Map<String, String> extractMetadata(String filePath) {
        Map<String, String> meta = new HashMap<>();
        try {
            Path path = Paths.get(filePath);
            nl.siegmann.epublib.domain.Book epubBook = new nl.siegmann.epublib.epub.EpubReader().readEpub(new FileInputStream(path.toFile()));
            if (epubBook != null) {
                var metadata = epubBook.getMetadata();
                if (metadata != null) {
                    var titles = metadata.getTitles();
                    if (titles != null && !titles.isEmpty()) {
                        meta.put("title", titles.get(0));
                    }
                    var authors = metadata.getAuthors();
                    if (authors != null && !authors.isEmpty()) {
                        StringBuilder authorSb = new StringBuilder();
                        for (var author : authors) {
                            if (authorSb.length() > 0) authorSb.append(", ");
                            authorSb.append(author.getFirstname());
                            if (author.getLastname() != null && !author.getLastname().isBlank()) {
                                authorSb.append(" ").append(author.getLastname());
                            }
                        }
                        meta.put("author", authorSb.toString().trim());
                    }
                    var subjects = metadata.getSubjects();
                    if (subjects != null && !subjects.isEmpty()) {
                        StringBuilder subjectSb = new StringBuilder();
                        for (var subject : subjects) {
                            if (subjectSb.length() > 0) subjectSb.append(", ");
                            subjectSb.append(subject);
                        }
                        meta.put("subject", subjectSb.toString().trim());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("提取EPUB元数据失败: {}", e.getMessage());
        }
        return meta;
    }
}
