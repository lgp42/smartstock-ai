package com.smartstock.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BacktestRunDTO {

    @NotBlank(message = "股票代码不能为空")
    private String stockCode;

    @NotNull(message = "初始资金不能为空")
    @DecimalMin(value = "1.00", message = "初始资金必须大于 0")
    private BigDecimal initialCapital;

    @Min(value = 2, message = "limit 最小为 2")
    @Max(value = 500, message = "limit 最大为 500")
    private Integer limit = 120;
}
