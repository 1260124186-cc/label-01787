package com.xiaoan.bookstore.service.reader;

import com.xiaoan.bookstore.service.EpubParserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EpubReaderAdapter implements ReaderAdapter {

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
}
