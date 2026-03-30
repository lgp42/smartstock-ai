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
@TableName("orders")
public class TradeOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String stockCode;

    private String orderType;

    private BigDecimal price;

    private Integer quantity;

    private BigDecimal amount;

    private BigDecimal fee;

    @Builder.Default
    private String status = "pending";

    private BigDecimal filledPrice;

    private Integer filledQuantity;

    private LocalDateTime filledTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
