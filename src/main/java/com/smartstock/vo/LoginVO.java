package com.smartstock.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoginVO {

    private Long userId;
    private String email;
    private String nickname;
    private String avatar;
    private LocalDateTime createdAt;
    private String token;
    private Long expiresIn;
}
