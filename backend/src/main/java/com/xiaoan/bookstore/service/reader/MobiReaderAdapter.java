package com.xiaoan.bookstore.service.reader;

import com.xiaoan.bookstore.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class MobiReaderAdapter implements ReaderAdapter {

    private static final Logger log = LoggerFactory.getLogger(MobiReaderAdapter.class);

    @Override
    public String getFormat() {
        return "mobi";
    }

    @Override
    public String getStreamType() {
        return STREAM_TYPE_HTML;
    }

    @Override
    public List<Map<String, Object>> getToc(String filePath) {
        log.warn("MOBI/AZW3 格式暂不支持目录解析: {}", filePath);
        return new ArrayList<>();
    }

    @Override
    public String getUnitContent(String filePath, int index) {
        throw new BusinessException("MOBI/AZW3 格式暂不支持在线阅读，请转换为 EPUB 或 PDF 格式");
    }

    @Override
    public byte[] getUnitImage(String filePath, int index) {
        return new byte[0];
    }

    @Override
    public int getTotalUnits(String filePath) {
        return 0;
    }
}
