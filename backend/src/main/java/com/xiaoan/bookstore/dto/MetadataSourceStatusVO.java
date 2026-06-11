package com.xiaoan.bookstore.dto;

import lombok.Data;

@Data
public class MetadataSourceStatusVO {
    private String sourceCode;
    private String sourceName;
    private boolean enabled;
    private int priority;
    private long dailyCallsUsed;
    private long dailyCallsLimit;
    private long totalCalls;
    private long successCount;
    private long failCount;
    private String lastCalledAt;
    private boolean hasApiKey;
}
