package com.xiaoan.bookstore.common;

public class Constants {
    /** 用户类型：管理员 */
    public static final int USER_TYPE_ADMIN = 1;
    /** 用户类型：小程序用户 */
    public static final int USER_TYPE_MP = 2;

    /** 批注类型：评语 */
    public static final int ANNOTATION_COMMENT = 1;
    /** 批注类型：笔记 */
    public static final int ANNOTATION_NOTE = 2;

    /** 状态：禁用 */
    public static final int STATUS_DISABLED = 0;
    /** 状态：启用 */
    public static final int STATUS_ENABLED = 1;
    /** 状态：已下架 */
    public static final int STATUS_TAKEN_DOWN = 2;

    /** 版权申诉状态：待处理 */
    public static final int COMPLAINT_PENDING = 0;
    /** 版权申诉状态：处理中 */
    public static final int COMPLAINT_PROCESSING = 1;
    /** 版权申诉状态：已下架 */
    public static final int COMPLAINT_TAKEN_DOWN = 2;
    /** 版权申诉状态：已驳回 */
    public static final int COMPLAINT_REJECTED = 3;

    /** 内容审核目标类型：书名 */
    public static final int AUDIT_TARGET_BOOK_TITLE = 1;
    /** 内容审核目标类型：批注 */
    public static final int AUDIT_TARGET_ANNOTATION = 2;
    /** 内容审核目标类型：书摘 */
    public static final int AUDIT_TARGET_EXCERPT = 3;

    /** 内容审核结果：通过 */
    public static final int AUDIT_RESULT_PASS = 0;
    /** 内容审核结果：疑似违规 */
    public static final int AUDIT_RESULT_SUSPECTED = 1;
    /** 内容审核结果：确认违规 */
    public static final int AUDIT_RESULT_VIOLATION = 2;

    /** JWT请求头 */
    public static final String TOKEN_HEADER = "Authorization";
    /** JWT前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** 上下文中的用户ID */
    public static final String CONTEXT_USER_ID = "userId";
    public static final String CONTEXT_USER_TYPE = "userType";
    public static final String CONTEXT_ROLE_ID = "roleId";
    public static final String CONTEXT_ROLE_CODE = "roleCode";

    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_OPERATOR = "OPERATOR";
    public static final String ROLE_AUDITOR = "AUDITOR";

    /** 备份任务类型：导出 */
    public static final int BACKUP_TYPE_EXPORT = 1;
    /** 备份任务类型：导入 */
    public static final int BACKUP_TYPE_IMPORT = 2;

    /** 备份任务状态：待处理 */
    public static final int BACKUP_STATUS_PENDING = 0;
    /** 备份任务状态：处理中 */
    public static final int BACKUP_STATUS_PROCESSING = 1;
    /** 备份任务状态：已完成 */
    public static final int BACKUP_STATUS_COMPLETED = 2;
    /** 备份任务状态：失败 */
    public static final int BACKUP_STATUS_FAILED = 3;

    /** 备份文件过期天数 */
    public static final int BACKUP_EXPIRE_DAYS = 7;

    /** 备份格式版本 */
    public static final String BACKUP_SCHEMA_VERSION = "1.0";

    public static final String PLAN_FREE = "free";
    public static final String PLAN_VIP = "vip";

    public static final int ORDER_TYPE_MEMBERSHIP = 1;
    public static final int ORDER_TYPE_STORAGE = 2;

    public static final int ORDER_STATUS_PENDING = 0;
    public static final int ORDER_STATUS_PAID = 1;
    public static final int ORDER_STATUS_CANCELLED = 2;
    public static final int ORDER_STATUS_REFUNDED = 3;

    public static final int POINTS_TYPE_EARN = 1;
    public static final int POINTS_TYPE_CONSUME = 2;

    public static final String POINTS_CATEGORY_DAILY_CHECKIN = "daily_checkin";
    public static final String POINTS_CATEGORY_UPLOAD_BOOK = "upload_book";
    public static final String POINTS_CATEGORY_SHARE_EXCERPT = "share_excerpt";
    public static final String POINTS_CATEGORY_EXCHANGE_VIP = "exchange_vip";
    public static final String POINTS_CATEGORY_EXCHANGE_STORAGE = "exchange_storage";
    public static final String POINTS_CATEGORY_ADMIN_ADJUST = "admin_adjust";

    public static final int EXCHANGE_TYPE_VIP_DAYS = 1;
    public static final int EXCHANGE_TYPE_STORAGE = 2;

    public static final long BYTES_PER_MB = 1024L * 1024L;
    public static final long BYTES_PER_GB = 1024L * 1024L * 1024L;

    public static final int ORDER_EXPIRE_MINUTES = 30;

    public static final String FORMAT_PDF = "pdf";
    public static final String FORMAT_EPUB = "epub";
    public static final String FORMAT_MOBI = "mobi";

    public static final long MAX_SIZE_PDF = 157286400L;
    public static final long MAX_SIZE_EPUB = 104857600L;
    public static final long MAX_SIZE_MOBI = 104857600L;

    /** 公开书摘状态：已撤回 */
    public static final int EXCERPT_STATUS_WITHDRAWN = 0;
    /** 公开书摘状态：正常 */
    public static final int EXCERPT_STATUS_NORMAL = 1;
    /** 公开书摘状态：已下架 */
    public static final int EXCERPT_STATUS_TAKEN_DOWN = 2;

    /** 书摘审核状态：待审核 */
    public static final int EXCERPT_AUDIT_PENDING = 0;
    /** 书摘审核状态：审核通过 */
    public static final int EXCERPT_AUDIT_PASS = 1;
    /** 书摘审核状态：审核不通过 */
    public static final int EXCERPT_AUDIT_REJECT = 2;

    /** 书摘举报状态：待处理 */
    public static final int EXCERPT_REPORT_PENDING = 0;
    /** 书摘举报状态：已处理 */
    public static final int EXCERPT_REPORT_HANDLED = 1;
    /** 书摘举报状态：已驳回 */
    public static final int EXCERPT_REPORT_REJECTED = 2;

    /** 排序方式：最新 */
    public static final String SORT_BY_LATEST = "latest";
    /** 排序方式：最热 */
    public static final String SORT_BY_HOT = "hot";

    /** 索引任务状态：待处理 */
    public static final int INDEX_STATUS_PENDING = 0;
    /** 索引任务状态：处理中 */
    public static final int INDEX_STATUS_PROCESSING = 1;
    /** 索引任务状态：已完成 */
    public static final int INDEX_STATUS_COMPLETED = 2;
    /** 索引任务状态：失败 */
    public static final int INDEX_STATUS_FAILED = 3;

    /** 搜索范围：全书 */
    public static final String SEARCH_SCOPE_ALL = "all";
    /** 搜索范围：单本书 */
    public static final String SEARCH_SCOPE_BOOK = "book";
    /** 搜索范围：仅笔记与批注 */
    public static final String SEARCH_SCOPE_NOTES = "notes";

    /** 高亮上下文字数 */
    public static final int SEARCH_HIGHLIGHT_CONTEXT = 30;

    /** 批注颜色：黄色 */
    public static final String ANNOTATION_COLOR_YELLOW = "yellow";
    /** 批注颜色：绿色 */
    public static final String ANNOTATION_COLOR_GREEN = "green";
    /** 批注颜色：粉色 */
    public static final String ANNOTATION_COLOR_PINK = "pink";

    /** 批注未置顶 */
    public static final int ANNOTATION_NOT_PINNED = 0;
    /** 批注已置顶 */
    public static final int ANNOTATION_PINNED = 1;

    /** 书籍排序：上传时间（最新） */
    public static final String BOOK_SORT_UPLOAD_TIME = "upload_time";
    /** 书籍排序：最近阅读 */
    public static final String BOOK_SORT_LAST_READ = "last_read";
    /** 书籍排序：书名（字母序） */
    public static final String BOOK_SORT_TITLE = "title";

    /** 书籍回收站过期天数 */
    public static final int BOOK_TRASH_EXPIRE_DAYS = 7;

    private Constants() {}
}
