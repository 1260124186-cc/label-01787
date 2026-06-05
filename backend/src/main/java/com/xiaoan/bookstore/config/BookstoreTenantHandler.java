package com.xiaoan.bookstore.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.xiaoan.bookstore.common.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BookstoreTenantHandler implements TenantLineHandler {

    private static final Set<String> TENANT_TABLES = new HashSet<>(Arrays.asList(
            "book", "category", "annotation", "reading_record"
    ));

    private static final String TENANT_COLUMN = "user_id";

    @Override
    public Expression getTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && TenantContext.isMpUser()) {
            return new LongValue(tenantId);
        }
        return new NullValue();
    }

    @Override
    public String getTenantIdColumn() {
        return TENANT_COLUMN;
    }

    @Override
    public boolean ignoreTable(String tableName) {
        if (TenantContext.isIgnoreTenant()) {
            return true;
        }
        if (!TenantContext.isMpUser()) {
            return true;
        }
        return !TENANT_TABLES.contains(tableName.toLowerCase());
    }
}
