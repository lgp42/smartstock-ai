package com.smartstock.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SellOrderDTO {

    @NotBlank(message = "股票代码不能为空")
    private String stockCode;

    @NotNull(message = "卖出价格不能为空")
    @DecimalMin(value = "0.01", message = "卖出价格必须大于0.01")
    @Digits(integer = 10, fraction = 2, message = "卖出价格最多保留2位小数")
    private BigDecimal price;

    @NotNull(message = "卖出数量不能为空")
    @Min(value = 100, message = "卖出数量最少100股")
    private Integer quantity;
}
