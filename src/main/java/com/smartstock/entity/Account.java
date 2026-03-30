package com.smartstock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("accounts")
public class Account {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private BigDecimal totalAssets;

    private BigDecimal availableCash;

    private BigDecimal frozenCash;

    private BigDecimal positionValue;

    private BigDecimal totalProfit;

    private BigDecimal profitRate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
