package com.smartstock.service.support;

import com.smartstock.client.StockRealtimeDTO;
import com.smartstock.vo.KlineVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RealtimeSecondSeriesAssemblerTest {

    @Test
    void assembleShouldCreateSecondBarsFromRealtimeSnapshots() {
        MutableClock clock = new MutableClock(Instant.parse("2026-03-19T02:02:01Z"), ZoneId.of("Asia/Shanghai"));
        RealtimeSecondSeriesAssembler assembler = new RealtimeSecondSeriesAssembler(clock);

        List<KlineVO> first = assembler.assemble("0:000001", realtime("10.01", "0.10",
                1_000L, "100000.00", LocalDateTime.of(2026, 3, 19, 10, 2, 1)), 10);
        clock.setInstant(Instant.parse("2026-03-19T02:02:02Z"));
        List<KlineVO> second = assembler.assemble("0:000001", realtime("10.03", "0.30",
                1_080L, "108600.00", LocalDateTime.of(2026, 3, 19, 10, 2, 2)), 10);

        assertEquals(10, first.size());
        assertEquals("2026-03-19 10:01:52", first.get(0).getDate());
        assertEquals("2026-03-19 10:02:01", first.get(9).getDate());
        assertEquals(0L, first.get(9).getVolume());

        assertEquals(10, second.size());
        KlineVO latest = second.get(9);
        assertEquals("2026-03-19 10:02:02", latest.getDate());
        assertEquals(new BigDecimal("10.03"), latest.getOpen());
        assertEquals(new BigDecimal("10.03"), latest.getClose());
        assertEquals(80L, latest.getVolume());
        assertEquals(new BigDecimal("8600.00"), latest.getAmount());
    }

    @Test
    void assembleShouldUpdateExistingBarWithinSameSecond() {
        RealtimeSecondSeriesAssembler assembler = new RealtimeSecondSeriesAssembler(
                Clock.fixed(Instant.parse("2026-03-19T02:02:03Z"), ZoneId.of("Asia/Shanghai")));

        assembler.assemble("0:000001", realtime("10.01", "0.10",
                1_000L, "100000.00", LocalDateTime.of(2026, 3, 19, 10, 2, 1)), 10);
        List<KlineVO> result = assembler.assemble("0:000001", realtime("10.05", "0.50",
                1_050L, "105500.00", LocalDateTime.of(2026, 3, 19, 10, 2, 1)), 10);

        assertEquals(10, result.size());
        KlineVO latest = result.get(9);
        assertEquals(new BigDecimal("10.01"), latest.getOpen());
        assertEquals(new BigDecimal("10.05"), latest.getClose());
        assertEquals(new BigDecimal("10.05"), latest.getHigh());
        assertEquals(new BigDecimal("10.01"), latest.getLow());
        assertEquals(50L, latest.getVolume());
        assertEquals(new BigDecimal("5500.00"), latest.getAmount());
        assertEquals(new BigDecimal("0.50"), latest.getChangeRate());
    }

    @Test
    void assembleShouldAdvanceCurrentSecondEvenWhenQuoteTimeIsStale() {
        MutableClock clock = new MutableClock(Instant.parse("2026-03-19T02:02:02Z"), ZoneId.of("Asia/Shanghai"));
        RealtimeSecondSeriesAssembler assembler = new RealtimeSecondSeriesAssembler(clock);

        assembler.assemble("0:000001", realtime("10.01", "0.10",
                1_000L, "100000.00", LocalDateTime.of(2026, 3, 19, 10, 2, 2)), 10);

        clock.setInstant(Instant.parse("2026-03-19T02:02:03Z"));
        List<KlineVO> result = assembler.assemble("0:000001", realtime("10.01", "0.10",
                1_000L, "100000.00", LocalDateTime.of(2026, 3, 19, 10, 2, 2)), 10);

        assertEquals(10, result.size());
        KlineVO latest = result.get(9);
        assertEquals("2026-03-19 10:02:03", latest.getDate());
        assertEquals(new BigDecimal("10.01"), latest.getOpen());
        assertEquals(new BigDecimal("10.01"), latest.getClose());
        assertEquals(0L, latest.getVolume());
        assertEquals(new BigDecimal("0.00"), latest.getAmount());
    }

    @Test
    void assembleShouldFillMissingSecondsBetweenTwoRequests() {
        MutableClock clock = new MutableClock(Instant.parse("2026-03-19T02:02:01Z"), ZoneId.of("Asia/Shanghai"));
        RealtimeSecondSeriesAssembler assembler = new RealtimeSecondSeriesAssembler(clock);

        assembler.assemble("0:000001", realtime("10.01", "0.10",
                1_000L, "100000.00", LocalDateTime.of(2026, 3, 19, 10, 2, 1)), 10);

        clock.setInstant(Instant.parse("2026-03-19T02:02:05Z"));
        List<KlineVO> result = assembler.assemble("0:000001", realtime("10.05", "0.50",
                1_050L, "105500.00", LocalDateTime.of(2026, 3, 19, 10, 2, 5)), 10);

        assertEquals(10, result.size());
        assertEquals("2026-03-19 10:02:02", result.get(6).getDate());
        assertEquals("2026-03-19 10:02:03", result.get(7).getDate());
        assertEquals("2026-03-19 10:02:04", result.get(8).getDate());
        assertEquals("2026-03-19 10:02:05", result.get(9).getDate());
        assertEquals(0L, result.get(8).getVolume());
        assertEquals(50L, result.get(9).getVolume());
    }

    private StockRealtimeDTO realtime(String price, String changeRate, long volume, String amount, LocalDateTime quoteTime) {
        StockRealtimeDTO dto = new StockRealtimeDTO();
        dto.setCurrentPrice(new BigDecimal(price));
        dto.setChangeRate(new BigDecimal(changeRate));
        dto.setVolume(volume);
        dto.setAmount(new BigDecimal(amount));
        dto.setQuoteTime(quoteTime);
        return dto;
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zoneId;

        private MutableClock(Instant instant, ZoneId zoneId) {
            this.instant = instant;
            this.zoneId = zoneId;
        }

        private void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return zoneId;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant.atOffset(ZoneOffset.UTC).toInstant();
        }
    }
}
