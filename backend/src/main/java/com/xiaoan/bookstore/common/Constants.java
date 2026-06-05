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
