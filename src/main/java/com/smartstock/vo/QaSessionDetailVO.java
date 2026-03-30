package com.smartstock.vo;

import lombok.Data;

import java.util.List;

@Data
public class QaSessionDetailVO {

    private String sessionId;
    private List<QaSessionMessageVO> messages;
}
