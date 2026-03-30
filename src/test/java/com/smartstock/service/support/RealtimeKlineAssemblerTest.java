package com.smartstock.service.support;

import com.smartstock.client.StockRealtimeDTO;
import com.smartstock.vo.KlineVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RealtimeKlineAssemblerTest {

    @Test
    void mergeShouldAppendCurrentOneMinuteBarDuringTradingSession() {
        RealtimeKlineAssembler assembler = new RealtimeKlineAssembler(Clock.fixed(
                Instant.parse("2026-03-19T02:02:51Z"), ZoneId.of("Asia/Shanghai")));

        List<KlineVO> merged = assembler.merge("1min", List.of(
                kline("2026-03-19 09:59", "10.92", "10.93", "10.94", "10.91", 100_000L, "1000000.00"),
                kline("2026-03-19 10:00", "10.93", "10.94", "10.95", "10.92", 120_000L, "1200000.00"),
                kline("2026-03-19 10:01", "10.94", "10.95", "10.96", "10.93", 130_000L, "1300000.00")
        ), realtime("10.97", "10.90", "10.89", "10.92", 380_000L, "3900000.00"));

        assertEquals(4, merged.size());
        KlineVO latest = merged.get(3);
        assertEquals("2026-03-19 10:02", latest.getDate());
        assertEquals(new BigDecimal("10.95"), latest.getOpen());
        assertEquals(new BigDecimal("10.97"), latest.getClose());
        assertEquals(new BigDecimal("10.97"), latest.getHigh());
        assertEquals(new BigDecimal("10.95"), latest.getLow());
        assertEquals(30_000L, latest.getVolume());
    }

    @Test
    void mergeShouldRefreshCurrentTradingDayBar() {
        RealtimeKlineAssembler assembler = new RealtimeKlineAssembler(Clock.fixed(
                Instant.parse("2026-03-19T02:02:51Z"), ZoneId.of("Asia/Shanghai")));

        List<KlineVO> merged = assembler.merge("day", List.of(
                kline("2026-03-18", "10.91", "10.96", "11.04", "10.90", 791_079L, "867711250.46")
        ), realtime("10.97", "10.90", "10.89", "10.92", 189_015L, "206604422.00"));

        assertEquals(2, merged.size());
        KlineVO latest = merged.get(1);
        assertEquals("2026-03-19", latest.getDate());
        assertEquals(new BigDecimal("10.92"), latest.getOpen());
        assertEquals(new BigDecimal("10.97"), latest.getClose());
        assertEquals(new BigDecimal("10.97"), latest.getHigh());
        assertEquals(new BigDecimal("10.89"), latest.getLow());
        assertEquals(189_015L, latest.getVolume());
    }

    private StockRealtimeDTO realtime(String currentPrice, String high, String low, String open, long volume, String amount) {
        StockRealtimeDTO dto = new StockRealtimeDTO();
        dto.setCurrentPrice(new BigDecimal(currentPrice));
        dto.setHigh(new BigDecimal(high));
        dto.setLow(new BigDecimal(low));
        dto.setOpen(new BigDecimal(open));
        dto.setPreClose(new BigDecimal("10.96"));
        dto.setVolume(volume);
        dto.setAmount(new BigDecimal(amount));
        return dto;
    }

    private KlineVO kline(String date, String open, String close, String high, String low, long volume, String amount) {
        KlineVO vo = new KlineVO();
        vo.setDate(date);
        vo.setOpen(new BigDecimal(open));
        vo.setClose(new BigDecimal(close));
        vo.setHigh(new BigDecimal(high));
        vo.setLow(new BigDecimal(low));
        vo.setVolume(volume);
        vo.setAmount(new BigDecimal(amount));
        return vo;
    }
}
