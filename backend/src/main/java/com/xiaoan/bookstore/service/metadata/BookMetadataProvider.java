package com.xiaoan.bookstore.service.metadata;

import com.xiaoan.bookstore.dto.BookMetadataVO;
import com.xiaoan.bookstore.dto.MetadataSearchQuery;

import java.util.List;

public interface BookMetadataProvider {

    String getSourceCode();

    String getSourceName();

    boolean isEnabled();

    int getPriority();

    BookMetadataVO searchOne(MetadataSearchQuery query);

    List<BookMetadataVO> search(MetadataSearchQuery query, int limit);
}
