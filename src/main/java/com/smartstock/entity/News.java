package com.smartstock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("news")
public class News {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private String source;

    private String url;

    private String stockCode;

    private String sentiment;

    private BigDecimal sentimentScore;

    private LocalDateTime publishTime;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
