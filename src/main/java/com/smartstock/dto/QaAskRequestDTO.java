package com.smartstock.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QaAskRequestDTO {

    @Size(max = 120, message = "sessionId 长度不能超过 120")
    private String sessionId;

    @NotBlank(message = "stockCode 不能为空")
    private String stockCode;

    @NotBlank(message = "question 不能为空")
    @Size(max = 2000, message = "question 长度不能超过 2000")
    private String question;
}
