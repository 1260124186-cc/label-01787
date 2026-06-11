package com.xiaoan.bookstore.service.reader;

import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ReaderAdapterFactory {

    private final Map<String, ReaderAdapter> adapterMap;

    public ReaderAdapterFactory(List<ReaderAdapter> adapters) {
        this.adapterMap = adapters.stream()
                .collect(Collectors.toMap(ReaderAdapter::getFormat, Function.identity()));
    }

    public ReaderAdapter getAdapter(String format) {
        ReaderAdapter adapter = adapterMap.get(format);
        if (adapter == null) {
            throw new BusinessException("不支持的书格式: " + format);
        }
        return adapter;
    }

    public static String resolveFormat(String originalFilename) {
        if (originalFilename == null) {
            return Constants.FORMAT_PDF;
        }
        String lower = originalFilename.toLowerCase();
        if (lower.endsWith(".epub")) {
            return Constants.FORMAT_EPUB;
        }
        if (lower.endsWith(".mobi") || lower.endsWith(".azw3")) {
            return Constants.FORMAT_MOBI;
        }
        return Constants.FORMAT_PDF;
    }
}
