package com.smartstock.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("stock_info")
public class StockInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String stockCode;

    private String stockName;

    private String market;

    private String board;

    private String industry;

    @Builder.Default
    private Integer isSt = 0;

    @Builder.Default
    private Integer isDelisted = 0;

    private String source;

    private LocalDate listingDate;

    @Builder.Default
    private Integer status = 1;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 根据股票代码判断市场代码
     * 以 6 开头 -> "1"(上海), 其他 -> "0"(深圳)
     */
    public static String getMarketCode(String stockCode) {
        return stockCode.startsWith("6") ? "1" : "0";
    }

    /**
     * 将市场字段转换为东方财富接口所需市场代码
     * SH -> 1, SZ -> 0
     */
    public static String toEastMoneyMarketCode(String market) {
        return "SH".equals(market) ? "1" : "0";
    }

    public static String resolveBoard(String stockCode, String market) {
        if (stockCode == null) {
            return null;
        }
        if (!isTradableAStock(stockCode, market)) {
            return null;
        }
        if (stockCode.startsWith("688")) {
            return "STAR";
        }
        if (stockCode.startsWith("300") || stockCode.startsWith("301")) {
            return "CYB";
        }
        if ("SH".equalsIgnoreCase(market)) {
            return "SH_MAIN";
        }
        if ("SZ".equalsIgnoreCase(market)) {
            return "SZ_MAIN";
        }
        return null;
    }

    public static boolean isTradableAStock(String stockCode, String market) {
        if (stockCode == null || market == null) {
            return false;
        }
        if ("SH".equalsIgnoreCase(market)) {
            return stockCode.startsWith("600")
                    || stockCode.startsWith("601")
                    || stockCode.startsWith("603")
                    || stockCode.startsWith("605")
                    || stockCode.startsWith("688");
        }
        if ("SZ".equalsIgnoreCase(market)) {
            return stockCode.startsWith("000")
                    || stockCode.startsWith("001")
                    || stockCode.startsWith("002")
                    || stockCode.startsWith("003")
                    || stockCode.startsWith("300")
                    || stockCode.startsWith("301");
        }
        return false;
    }

    public static boolean isStStock(String stockName) {
        if (stockName == null) {
            return false;
        }
        String normalized = stockName.trim().toUpperCase();
        return normalized.startsWith("ST")
                || normalized.startsWith("*ST")
                || normalized.contains(" ST")
                || normalized.contains("*ST");
    }

    public static boolean isDelistedStock(String stockName) {
        if (stockName == null) {
            return false;
        }
        String normalized = stockName.trim();
        return normalized.contains("退")
                || normalized.contains("摘牌")
                || normalized.toUpperCase().contains("DELIST");
    }
}
