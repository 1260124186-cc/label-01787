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

    private Constants() {}
}
