package com.smartstock.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class WatchlistBatchDTO {

    @NotEmpty(message = "股票代码列表不能为空")
    private List<String> stockCodes;
}
