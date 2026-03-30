package com.smartstock.service.support;

import com.smartstock.client.StockRealtimeDTO;
import com.smartstock.vo.KlineVO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RealtimeSecondSeriesAssembler {

    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter SECOND_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_BARS_PER_SERIES = 600;
    private static final int DEFAULT_BOOTSTRAP_BARS = 120;
    private static final LocalTime MORNING_OPEN = LocalTime.of(9, 30);
    private static final LocalTime MORNING_CLOSE = LocalTime.of(11, 30);
    private static final LocalTime AFTERNOON_OPEN = LocalTime.of(13, 0);
    private static final LocalTime AFTERNOON_CLOSE = LocalTime.of(15, 0);

    private final Clock clock;
    private final Map<String, Deque<SecondBarState>> seriesStore = new ConcurrentHashMap<>();

    public RealtimeSecondSeriesAssembler(Clock clock) {
        this.clock = clock;
    }

    public List<KlineVO> assemble(String seriesKey, StockRealtimeDTO realtime, int limit) {
        if (seriesKey == null || seriesKey.isBlank()) {
            return List.of();
        }
        Deque<SecondBarState> series = seriesStore.computeIfAbsent(seriesKey, ignored -> new ArrayDeque<>());
        synchronized (series) {
            if (realtime == null || realtime.getCurrentPrice() == null) {
                return snapshot(series, limit);
            }

            LocalDateTime bucket = resolveBucketTime(realtime);
            if (shouldResetSeries(series.peekLast(), bucket)) {
                series.clear();
            }
            if (series.isEmpty()) {
                bootstrapSeries(series, realtime, bucket, seedBarCount(limit));
            } else {
                fillMissingSeconds(series, bucket, seedBarCount(limit));
            }

            SecondBarState current = series.peekLast();
            if (current == null || !current.bucket().equals(bucket)) {
                current = createBar(series.peekLast(), realtime, bucket);
                series.addLast(current);
            } else {
                updateBar(current, realtime);
            }

            while (series.size() > MAX_BARS_PER_SERIES) {
                series.removeFirst();
            }
            return snapshot(series, limit);
        }
    }

    private boolean shouldResetSeries(SecondBarState last, LocalDateTime bucket) {
        if (last == null) {
            return false;
        }
        LocalDate lastDate = last.bucket().toLocalDate();
        if (!lastDate.equals(bucket.toLocalDate())) {
            return true;
        }
        return bucket.isBefore(last.bucket());
    }

    private void bootstrapSeries(Deque<SecondBarState> series, StockRealtimeDTO realtime, LocalDateTime bucket, int seedBarCount) {
        BigDecimal price = scalePrice(realtime.getCurrentPrice());
        BigDecimal changeRate = resolveChangeRate(realtime);
        long cumulativeVolume = realtime.getVolume() == null ? 0L : realtime.getVolume();
        BigDecimal cumulativeAmount = scaleAmount(realtime.getAmount());
        LocalDateTime start = bucket.minusSeconds(seedBarCount - 1L);
        for (int i = 0; i < seedBarCount; i++) {
            series.addLast(new SecondBarState(start.plusSeconds(i), price, price, price, price,
                    0L, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), cumulativeVolume, cumulativeAmount, changeRate));
        }
    }

    private void fillMissingSeconds(Deque<SecondBarState> series, LocalDateTime bucket, int seedBarCount) {
        SecondBarState last = series.peekLast();
        if (last == null || !bucket.isAfter(last.bucket())) {
            return;
        }
        long gap = java.time.Duration.between(last.bucket(), bucket).getSeconds();
        if (gap <= 1) {
            return;
        }
        if (gap > seedBarCount) {
            SecondBarState latest = series.peekLast();
            series.clear();
            if (latest != null) {
                bootstrapSeries(series, toRealtime(latest), bucket.minusSeconds(1), seedBarCount);
            }
            return;
        }

        for (long step = 1; step < gap; step++) {
            SecondBarState previous = series.peekLast();
            if (previous == null) {
                return;
            }
            BigDecimal price = previous.close();
            series.addLast(new SecondBarState(previous.bucket().plusSeconds(1), price, price, price, price,
                    0L, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), previous.lastCumulativeVolume(),
                    previous.lastCumulativeAmount(), previous.changeRate()));
        }
    }

    private SecondBarState createBar(SecondBarState previous, StockRealtimeDTO realtime, LocalDateTime bucket) {
        BigDecimal price = scalePrice(realtime.getCurrentPrice());
        long cumulativeVolume = realtime.getVolume() == null ? 0L : realtime.getVolume();
        BigDecimal cumulativeAmount = scaleAmount(realtime.getAmount());
        long baseVolume = previous == null ? cumulativeVolume : previous.lastCumulativeVolume();
        BigDecimal baseAmount = previous == null ? cumulativeAmount : previous.lastCumulativeAmount();
        long volume = previous == null ? 0L : Math.max(cumulativeVolume - baseVolume, 0L);
        BigDecimal amount = previous == null ? BigDecimal.ZERO : nonNegative(cumulativeAmount.subtract(baseAmount));
        return new SecondBarState(bucket, price, price, price, price, volume, amount, cumulativeVolume, cumulativeAmount,
                resolveChangeRate(realtime));
    }

    private StockRealtimeDTO toRealtime(SecondBarState state) {
        StockRealtimeDTO dto = new StockRealtimeDTO();
        dto.setCurrentPrice(state.close());
        dto.setChangeRate(state.changeRate());
        dto.setVolume(state.lastCumulativeVolume());
        dto.setAmount(state.lastCumulativeAmount());
        dto.setQuoteTime(state.bucket());
        return dto;
    }

    private void updateBar(SecondBarState current, StockRealtimeDTO realtime) {
        BigDecimal price = scalePrice(realtime.getCurrentPrice());
        current.high(max(current.high(), price));
        current.low(min(current.low(), price));
        current.close(price);
        long cumulativeVolume = realtime.getVolume() == null ? current.lastCumulativeVolume() : realtime.getVolume();
        BigDecimal cumulativeAmount = realtime.getAmount() == null ? current.lastCumulativeAmount() : scaleAmount(realtime.getAmount());
        current.lastCumulativeVolume(cumulativeVolume);
        current.lastCumulativeAmount(cumulativeAmount);
        current.volume(Math.max(cumulativeVolume - current.baseVolume(), 0L));
        current.amount(nonNegative(cumulativeAmount.subtract(current.baseAmount())));
        current.changeRate(resolveChangeRate(realtime));
    }

    private List<KlineVO> snapshot(Deque<SecondBarState> series, int limit) {
        List<KlineVO> result = new ArrayList<>(series.size());
        for (SecondBarState bar : series) {
            KlineVO vo = new KlineVO();
            vo.setDate(bar.bucket().format(SECOND_FORMATTER));
            vo.setOpen(bar.open());
            vo.setClose(bar.close());
            vo.setHigh(bar.high());
            vo.setLow(bar.low());
            vo.setVolume(bar.volume());
            vo.setAmount(bar.amount());
            vo.setChangeRate(bar.changeRate());
            result.add(vo);
        }
        if (limit > 0 && result.size() > limit) {
            return new ArrayList<>(result.subList(result.size() - limit, result.size()));
        }
        return result;
    }

    private LocalDateTime resolveBucketTime(StockRealtimeDTO realtime) {
        LocalDateTime quoteTime = realtime.getQuoteTime() != null ? realtime.getQuoteTime() : LocalDateTime.now(clock.withZone(SHANGHAI_ZONE));
        LocalDateTime now = LocalDateTime.now(clock.withZone(SHANGHAI_ZONE));
        LocalDateTime normalizedQuoteTime = quoteTime.withNano(0);
        LocalDateTime normalizedNow = now.withNano(0);
        if (isTradingSession(normalizedNow)
                && normalizedNow.toLocalDate().equals(normalizedQuoteTime.toLocalDate())
                && normalizedNow.isAfter(normalizedQuoteTime)) {
            return normalizedNow;
        }
        return normalizedQuoteTime;
    }

    private int seedBarCount(int limit) {
        if (limit <= 0) {
            return DEFAULT_BOOTSTRAP_BARS;
        }
        return Math.max(10, Math.min(limit, DEFAULT_BOOTSTRAP_BARS));
    }

    private boolean isTradingSession(LocalDateTime time) {
        LocalTime localTime = time.toLocalTime();
        boolean morning = !localTime.isBefore(MORNING_OPEN) && !localTime.isAfter(MORNING_CLOSE);
        boolean afternoon = !localTime.isBefore(AFTERNOON_OPEN) && !localTime.isAfter(AFTERNOON_CLOSE);
        return morning || afternoon;
    }

    private BigDecimal resolveChangeRate(StockRealtimeDTO realtime) {
        if (realtime.getChangeRate() != null) {
            return realtime.getChangeRate().setScale(2, RoundingMode.HALF_UP);
        }
        if (realtime.getPreClose() == null || realtime.getPreClose().compareTo(BigDecimal.ZERO) == 0 || realtime.getCurrentPrice() == null) {
            return null;
        }
        return realtime.getCurrentPrice()
                .subtract(realtime.getPreClose())
                .multiply(BigDecimal.valueOf(100))
                .divide(realtime.getPreClose(), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal scalePrice(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nonNegative(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal max(BigDecimal left, BigDecimal right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.max(right);
    }

    private BigDecimal min(BigDecimal left, BigDecimal right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.min(right);
    }

    private static final class SecondBarState {
        private final LocalDateTime bucket;
        private final BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private final long baseVolume;
        private final BigDecimal baseAmount;
        private long volume;
        private BigDecimal amount;
        private long lastCumulativeVolume;
        private BigDecimal lastCumulativeAmount;
        private BigDecimal changeRate;

        private SecondBarState(LocalDateTime bucket, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close,
                               long volume, BigDecimal amount, long lastCumulativeVolume, BigDecimal lastCumulativeAmount,
                               BigDecimal changeRate) {
            this.bucket = bucket;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.baseVolume = lastCumulativeVolume - volume;
            this.baseAmount = lastCumulativeAmount.subtract(amount);
            this.volume = volume;
            this.amount = amount;
            this.lastCumulativeVolume = lastCumulativeVolume;
            this.lastCumulativeAmount = lastCumulativeAmount;
            this.changeRate = changeRate;
        }

        private LocalDateTime bucket() {
            return bucket;
        }

        private BigDecimal open() {
            return open;
        }

        private BigDecimal high() {
            return high;
        }

        private void high(BigDecimal high) {
            this.high = high;
        }

        private BigDecimal low() {
            return low;
        }

        private void low(BigDecimal low) {
            this.low = low;
        }

        private BigDecimal close() {
            return close;
        }

        private void close(BigDecimal close) {
            this.close = close;
        }

        private long baseVolume() {
            return baseVolume;
        }

        private BigDecimal baseAmount() {
            return baseAmount;
        }

        private long volume() {
            return volume;
        }

        private void volume(long volume) {
            this.volume = volume;
        }

        private BigDecimal amount() {
            return amount;
        }

        private void amount(BigDecimal amount) {
            this.amount = amount;
        }

        private long lastCumulativeVolume() {
            return lastCumulativeVolume;
        }

        private void lastCumulativeVolume(long lastCumulativeVolume) {
            this.lastCumulativeVolume = lastCumulativeVolume;
        }

        private BigDecimal lastCumulativeAmount() {
            return lastCumulativeAmount;
        }

        private void lastCumulativeAmount(BigDecimal lastCumulativeAmount) {
            this.lastCumulativeAmount = lastCumulativeAmount;
        }

        private BigDecimal changeRate() {
            return changeRate;
        }

        private void changeRate(BigDecimal changeRate) {
            this.changeRate = changeRate;
        }
    }
}
