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

    private Constants() {}
}
