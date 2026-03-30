package com.smartstock.vo;

import lombok.Data;

@Data
public class QaSessionMessageVO {

    private String id;
    private String role;
    private String content;
    private String createdAt;
}
