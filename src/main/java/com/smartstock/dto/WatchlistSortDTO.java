package com.smartstock.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class WatchlistSortDTO {

    @Valid
    @NotEmpty(message = "排序列表不能为空")
    private List<Item> items;

    @Data
    public static class Item {

        @NotBlank(message = "股票代码不能为空")
        private String stockCode;

        @NotNull(message = "排序值不能为空")
        private Integer sortOrder;
    }
}
