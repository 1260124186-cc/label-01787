package com.xiaoan.bookstore.dto;

import lombok.Data;

@Data
public class MemberUserVO {

    private Long userId;

    private String nickname;

    private String avatar;

    private String planCode;

    private String planName;

    private String expireAt;

    private Integer pointsBalance;

    private Long extraStorage;

    private String createdAt;
}
