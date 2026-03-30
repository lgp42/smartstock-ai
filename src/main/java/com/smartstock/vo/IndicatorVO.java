package com.smartstock.vo;

import lombok.Data;

import java.util.Map;

@Data
public class IndicatorVO {

    private String date;
    private String type;
    private Map<String, Object> data;
}
