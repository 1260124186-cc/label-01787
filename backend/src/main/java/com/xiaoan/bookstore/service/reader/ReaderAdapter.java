package com.xiaoan.bookstore.service.reader;

import java.util.List;
import java.util.Map;

public interface ReaderAdapter {

    String STREAM_TYPE_IMAGE = "image";
    String STREAM_TYPE_HTML = "html";

    String getFormat();

    String getStreamType();

    List<Map<String, Object>> getToc(String filePath);

    String getUnitContent(String filePath, int index);

    byte[] getUnitImage(String filePath, int index);

    int getTotalUnits(String filePath);

    @Deprecated
    default String getPageContent(String filePath, int index) {
        return getUnitContent(filePath, index);
    }

    @Deprecated
    default byte[] getPageImage(String filePath, int index) {
        return getUnitImage(filePath, index);
    }

    Map<String, String> extractMetadata(String filePath);
}
