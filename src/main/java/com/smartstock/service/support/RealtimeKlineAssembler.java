package com.smartstock.service.support;

import com.smartstock.client.StockRealtimeDTO;
import com.smartstock.vo.KlineVO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RealtimeKlineAssembler {

    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalTime MORNING_OPEN = LocalTime.of(9, 30);
    private static final LocalTime MORNING_CLOSE = LocalTime.of(11, 30);
    private static final LocalTime AFTERNOON_OPEN = LocalTime.of(13, 0);
    private static final LocalTime AFTERNOON_CLOSE = LocalTime.of(15, 0);
    private static final Duration LIVE_KLINE_TTL = Duration.ofSeconds(1);
    private static final Duration DEFAULT_KLINE_TTL = Duration.ofMinutes(5);
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    );

    private final Clock clock;

    public RealtimeKlineAssembler(Clock clock) {
        this.clock = clock;
    }

    public boolean shouldUseLiveCache(String period) {
        return shouldUseLiveCache(period, null);
    }

    public boolean shouldUseLiveCache(String period, StockRealtimeDTO realtime) {
        return supportsRealtimeMerge(period) && isTradingSession(resolveQuoteTime(realtime));
    }

    public Duration resolveCacheTtl(String period) {
        return resolveCacheTtl(period, null);
    }

    public Duration resolveCacheTtl(String period, StockRealtimeDTO realtime) {
        return shouldUseLiveCache(period, realtime) ? LIVE_KLINE_TTL : DEFAULT_KLINE_TTL;
    }

    public List<KlineVO> merge(String period, List<KlineVO> baseKlines, StockRealtimeDTO realtime) {
        List<KlineVO> result = copyKlines(baseKlines);
        if (!supportsRealtimeMerge(period) || realtime == null || realtime.getCurrentPrice() == null) {
            return result;
        }

        LocalDateTime quoteTime = resolveQuoteTime(realtime);
        if (!isTradingSession(quoteTime)) {
            return result;
        }

        return switch (period) {
            case "1min", "5min", "15min", "30min", "60min" -> mergeIntraday(period, result, realtime, quoteTime);
            case "day" -> mergeDaily(result, realtime, quoteTime);
            case "week" -> mergeWeekly(result, realtime, quoteTime);
            case "month" -> mergeMonthly(result, realtime, quoteTime);
            default -> result;
        };
    }

    private List<KlineVO> mergeIntraday(String period, List<KlineVO> result, StockRealtimeDTO realtime, LocalDateTime quoteTime) {
        int spanMinutes = switch (period) {
            case "1min" -> 1;
            case "5min" -> 5;
            case "15min" -> 15;
            case "30min" -> 30;
            case "60min" -> 60;
            default -> 1;
        };
        LocalDateTime bucketStart = floorIntradayBucket(quoteTime, spanMinutes);
        String bucketLabel = bucketStart.format(MINUTE_FORMATTER);
        int latestIndex = findBarIndex(result, bucketLabel);
        KlineVO current = latestIndex >= 0 ? result.get(latestIndex) : new KlineVO();

        KlineVO previous = latestIndex >= 1 ? result.get(latestIndex - 1)
                : latestIndex < 0 && !result.isEmpty() ? result.get(result.size() - 1) : null;
        BigDecimal open = firstNonNull(current.getOpen(),
                latestIndex >= 0 ? null : previous == null ? realtime.getOpen() : previous.getClose(),
                realtime.getOpen(),
                realtime.getCurrentPrice());
        BigDecimal close = realtime.getCurrentPrice();
        BigDecimal high = latestIndex >= 0
                ? max(current.getHigh(), current.getClose(), realtime.getCurrentPrice(), open, close)
                : max(open, close);
        BigDecimal low = latestIndex >= 0
                ? min(current.getLow(), current.getClose(), realtime.getCurrentPrice(), open, close)
                : min(open, close);
        Long volume = resolveIntradayVolume(result, latestIndex, bucketStart, realtime.getVolume(), current.getVolume());
        BigDecimal amount = resolveIntradayAmount(result, latestIndex, bucketStart, realtime.getAmount(), current.getAmount());

        current.setDate(bucketLabel);
        current.setOpen(scalePrice(open));
        current.setClose(scalePrice(close));
        current.setHigh(scalePrice(high));
        current.setLow(scalePrice(low));
        current.setVolume(volume);
        current.setAmount(scaleAmount(amount));
        current.setChangeRate(calculateChangeRate(firstNonNull(previous == null ? null : previous.getClose(), realtime.getPreClose()), close, realtime.getChangeRate()));

        if (latestIndex >= 0) {
            result.set(latestIndex, current);
        } else {
            result.add(current);
        }
        result.sort(Comparator.comparing(this::sortableDateTime));
        return result;
    }

    private List<KlineVO> mergeDaily(List<KlineVO> result, StockRealtimeDTO realtime, LocalDateTime quoteTime) {
        String bucketLabel = quoteTime.toLocalDate().format(DAY_FORMATTER);
        int latestIndex = findBarIndex(result, bucketLabel);
        KlineVO current = latestIndex >= 0 ? result.get(latestIndex) : new KlineVO();
        KlineVO previous = latestIndex >= 1 ? result.get(latestIndex - 1)
                : latestIndex < 0 && !result.isEmpty() ? result.get(result.size() - 1) : null;

        current.setDate(bucketLabel);
        current.setOpen(scalePrice(firstNonNull(current.getOpen(), realtime.getOpen(), previous == null ? null : previous.getClose(), realtime.getCurrentPrice())));
        current.setClose(scalePrice(realtime.getCurrentPrice()));
        current.setHigh(scalePrice(max(current.getHigh(), realtime.getHigh(), current.getOpen(), realtime.getCurrentPrice())));
        current.setLow(scalePrice(min(current.getLow(), realtime.getLow(), current.getOpen(), realtime.getCurrentPrice())));
        current.setVolume(realtime.getVolume());
        current.setAmount(scaleAmount(realtime.getAmount()));
        current.setChangeRate(calculateChangeRate(firstNonNull(previous == null ? null : previous.getClose(), realtime.getPreClose()),
                realtime.getCurrentPrice(), realtime.getChangeRate()));

        if (latestIndex >= 0) {
            result.set(latestIndex, current);
        } else {
            result.add(current);
        }
        result.sort(Comparator.comparing(this::sortableDateTime));
        return result;
    }

    private List<KlineVO> mergeWeekly(List<KlineVO> result, StockRealtimeDTO realtime, LocalDateTime quoteTime) {
        LocalDate periodStart = quoteTime.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return mergeHigherPeriod(result, realtime, quoteTime, periodStart.format(DAY_FORMATTER), "week");
    }

    private List<KlineVO> mergeMonthly(List<KlineVO> result, StockRealtimeDTO realtime, LocalDateTime quoteTime) {
        LocalDate periodStart = quoteTime.toLocalDate().withDayOfMonth(1);
        return mergeHigherPeriod(result, realtime, quoteTime, periodStart.format(DAY_FORMATTER), "month");
    }

    private List<KlineVO> mergeHigherPeriod(List<KlineVO> result, StockRealtimeDTO realtime, LocalDateTime quoteTime,
                                            String bucketLabel, String period) {
        int latestIndex = findHigherPeriodIndex(result, quoteTime, period);
        KlineVO current = latestIndex >= 0 ? result.get(latestIndex) : new KlineVO();

        current.setDate(latestIndex >= 0 ? current.getDate() : bucketLabel);
        current.setOpen(scalePrice(firstNonNull(current.getOpen(), realtime.getOpen(), realtime.getCurrentPrice())));
        current.setClose(scalePrice(realtime.getCurrentPrice()));
        current.setHigh(scalePrice(max(current.getHigh(), realtime.getHigh(), current.getOpen(), realtime.getCurrentPrice())));
        current.setLow(scalePrice(min(current.getLow(), realtime.getLow(), current.getOpen(), realtime.getCurrentPrice())));
        current.setVolume(latestIndex >= 0 ? max(current.getVolume(), realtime.getVolume()) : realtime.getVolume());
        current.setAmount(latestIndex >= 0 ? max(current.getAmount(), realtime.getAmount()) : scaleAmount(realtime.getAmount()));
        current.setChangeRate(realtime.getChangeRate());

        if (latestIndex >= 0) {
            result.set(latestIndex, current);
        } else {
            result.add(current);
        }
        result.sort(Comparator.comparing(this::sortableDateTime));
        return result;
    }

    private Long resolveIntradayVolume(List<KlineVO> result, int latestIndex, LocalDateTime bucketStart,
                                       Long realtimeVolume, Long existingVolume) {
        if (realtimeVolume == null) {
            return existingVolume;
        }
        long prior = 0L;
        for (int i = 0; i < result.size(); i++) {
            if (i == latestIndex) {
                continue;
            }
            LocalDateTime barTime = parseDateTime(result.get(i).getDate());
            if (barTime == null || !barTime.toLocalDate().equals(bucketStart.toLocalDate()) || !barTime.isBefore(bucketStart)) {
                continue;
            }
            prior += result.get(i).getVolume() == null ? 0L : result.get(i).getVolume();
        }
        long derived = realtimeVolume - prior;
        if (derived < 0) {
            return existingVolume;
        }
        if (existingVolume == null) {
            return derived;
        }
        return Math.max(existingVolume, derived);
    }

    private BigDecimal resolveIntradayAmount(List<KlineVO> result, int latestIndex, LocalDateTime bucketStart,
                                             BigDecimal realtimeAmount, BigDecimal existingAmount) {
        if (realtimeAmount == null) {
            return existingAmount;
        }
        BigDecimal prior = BigDecimal.ZERO;
        for (int i = 0; i < result.size(); i++) {
            if (i == latestIndex) {
                continue;
            }
            LocalDateTime barTime = parseDateTime(result.get(i).getDate());
            if (barTime == null || !barTime.toLocalDate().equals(bucketStart.toLocalDate()) || !barTime.isBefore(bucketStart)) {
                continue;
            }
            prior = prior.add(result.get(i).getAmount() == null ? BigDecimal.ZERO : result.get(i).getAmount());
        }
        BigDecimal derived = realtimeAmount.subtract(prior);
        if (derived.compareTo(BigDecimal.ZERO) < 0) {
            return existingAmount;
        }
        if (existingAmount == null) {
            return derived;
        }
        return existingAmount.max(derived);
    }

    private int findBarIndex(List<KlineVO> result, String bucketLabel) {
        for (int i = result.size() - 1; i >= 0; i--) {
            if (bucketLabel.equals(result.get(i).getDate())) {
                return i;
            }
        }
        return -1;
    }

    private int findHigherPeriodIndex(List<KlineVO> result, LocalDateTime quoteTime, String period) {
        for (int i = result.size() - 1; i >= 0; i--) {
            LocalDateTime barTime = parseDateTime(result.get(i).getDate());
            if (barTime == null) {
                continue;
            }
            if ("week".equals(period) && sameWeek(barTime.toLocalDate(), quoteTime.toLocalDate())) {
                return i;
            }
            if ("month".equals(period) && barTime.getYear() == quoteTime.getYear() && barTime.getMonth() == quoteTime.getMonth()) {
                return i;
            }
        }
        return -1;
    }

    private boolean sameWeek(LocalDate left, LocalDate right) {
        return left.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .equals(right.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
    }

    private LocalDateTime resolveQuoteTime(StockRealtimeDTO realtime) {
        if (realtime != null && realtime.getQuoteTime() != null) {
            return realtime.getQuoteTime();
        }
        return LocalDateTime.now(clock.withZone(SHANGHAI_ZONE));
    }

    private boolean supportsRealtimeMerge(String period) {
        return List.of("1min", "5min", "15min", "30min", "60min", "day", "week", "month")
                .contains(period == null ? null : period.trim().toLowerCase());
    }

    private boolean isTradingSession(LocalDateTime time) {
        DayOfWeek dayOfWeek = time.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }
        LocalTime localTime = time.toLocalTime();
        boolean morning = !localTime.isBefore(MORNING_OPEN) && localTime.isBefore(MORNING_CLOSE);
        boolean afternoon = !localTime.isBefore(AFTERNOON_OPEN) && localTime.isBefore(AFTERNOON_CLOSE);
        return morning || afternoon;
    }

    private LocalDateTime floorIntradayBucket(LocalDateTime quoteTime, int spanMinutes) {
        LocalDate date = quoteTime.toLocalDate();
        LocalDateTime sessionStart = quoteTime.toLocalTime().isBefore(MORNING_CLOSE)
                ? LocalDateTime.of(date, MORNING_OPEN)
                : LocalDateTime.of(date, AFTERNOON_OPEN);
        long elapsed = Duration.between(sessionStart, quoteTime).toMinutes();
        long offset = Math.max(0, elapsed / spanMinutes) * spanMinutes;
        return sessionStart.plusMinutes(offset);
    }

    private LocalDateTime sortableDateTime(KlineVO item) {
        LocalDateTime parsed = parseDateTime(item.getDate());
        return parsed == null ? LocalDateTime.MIN : parsed;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value, DAY_FORMATTER).atStartOfDay();
        } catch (DateTimeParseException ignored) {
        }
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private List<KlineVO> copyKlines(List<KlineVO> baseKlines) {
        List<KlineVO> result = new ArrayList<>();
        if (baseKlines == null) {
            return result;
        }
        for (KlineVO item : baseKlines) {
            KlineVO copy = new KlineVO();
            copy.setDate(item.getDate());
            copy.setOpen(item.getOpen());
            copy.setClose(item.getClose());
            copy.setHigh(item.getHigh());
            copy.setLow(item.getLow());
            copy.setVolume(item.getVolume());
            copy.setAmount(item.getAmount());
            copy.setChangeRate(item.getChangeRate());
            result.add(copy);
        }
        return result;
    }

    private BigDecimal calculateChangeRate(BigDecimal previousClose, BigDecimal currentPrice, BigDecimal fallback) {
        if (previousClose == null || currentPrice == null || previousClose.compareTo(BigDecimal.ZERO) == 0) {
            return fallback;
        }
        return currentPrice.subtract(previousClose)
                .divide(previousClose, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scalePrice(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleAmount(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal firstNonNull(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private BigDecimal max(BigDecimal... values) {
        BigDecimal result = null;
        for (BigDecimal value : values) {
            if (value == null) {
                continue;
            }
            result = result == null || value.compareTo(result) > 0 ? value : result;
        }
        return result;
    }

    private BigDecimal min(BigDecimal... values) {
        BigDecimal result = null;
        for (BigDecimal value : values) {
            if (value == null) {
                continue;
            }
            result = result == null || value.compareTo(result) < 0 ? value : result;
        }
        return result;
    }

    private Long max(Long left, Long right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return Math.max(left, right);
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
}
