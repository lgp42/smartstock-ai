package com.smartstock.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class StockAnalysisRequestDTO {

    @NotBlank(message = "stockCode 不能为空")
    private String stockCode;

    @Pattern(
            regexp = "^(simple|detailed|full|brief)$",
            message = "reportType 仅支持 simple/detailed/full/brief"
    )
    private String reportType = "detailed";

    private boolean forceRefresh;
}
