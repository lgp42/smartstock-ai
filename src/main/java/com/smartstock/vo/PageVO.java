package com.smartstock.vo;

import lombok.Data;

import java.util.List;

@Data
public class PageVO<T> {

    private Long total;
    private Integer page;
    private Integer pageSize;
    private List<T> records;

    public PageVO(Long total, Integer page, Integer pageSize, List<T> records) {
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.records = records;
    }
}
