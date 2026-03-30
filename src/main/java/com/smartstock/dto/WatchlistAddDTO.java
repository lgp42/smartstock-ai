package com.smartstock.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WatchlistAddDTO {

    @NotBlank(message = "股票代码不能为空")
    private String stockCode;
}
