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
@TableName("trade_records")
public class TradeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long orderId;

    private String stockCode;

    private String tradeType;

    private BigDecimal price;

    private Integer quantity;

    private BigDecimal amount;

    private BigDecimal fee;

    private LocalDateTime tradeTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
