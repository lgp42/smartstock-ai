package com.smartstock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartstock.client.EastMoneyClient;
import com.smartstock.client.StockRealtimeDTO;
import com.smartstock.common.BusinessException;
import com.smartstock.common.ErrorCode;
import com.smartstock.convert.TradeStructMapper;
import com.smartstock.dto.BuyOrderDTO;
import com.smartstock.dto.SellOrderDTO;
import com.smartstock.entity.*;
import com.smartstock.mapper.*;
import com.smartstock.service.TradeService;
import com.smartstock.util.TextEncodingUtils;
import com.smartstock.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeServiceImpl implements TradeService {

    private static final BigDecimal INITIAL_ASSETS = new BigDecimal("1000000");

    private final AccountMapper accountMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final PositionMapper positionMapper;
    private final TradeRecordMapper tradeRecordMapper;
    private final StockInfoMapper stockInfoMapper;
    private final EastMoneyClient eastMoneyClient;
    private final TradeStructMapper tradeStructMapper;

    private static final BigDecimal MIN_FEE = new BigDecimal("5");
    private static final BigDecimal FEE_RATE = new BigDecimal("0.0003");
    private static final BigDecimal TAX_RATE = new BigDecimal("0.001");
    private static final BigDecimal PRICE_LIMIT_RATE = new BigDecimal("0.10");
    private static final int RATE_SCALE = 4;

    @Override
    public AccountVO getAccount(Long userId) {
        Account account = getAccountByUserId(userId);
        refreshAccountSnapshot(account);
        accountMapper.updateById(account);
        return toAccountVO(userId, account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO buy(Long userId, BuyOrderDTO dto) {
        String stockCode = dto.getStockCode();
        BigDecimal price = dto.getPrice();
        int quantity = dto.getQuantity();

        // 检查 quantity 是否是 100 的整数倍
        if (quantity % 100 != 0) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY, "买入数量必须是100的整数倍");
        }

        // 获取账户
        Account account = getAccountByUserId(userId);

        // 获取股票信息
        StockInfo stockInfo = getStockInfo(stockCode);
        String marketCode = StockInfo.toEastMoneyMarketCode(stockInfo.getMarket());

        // 获取当前股价，检查价格范围
        StockRealtimeDTO realtimeDTO = eastMoneyClient.getRealtimeQuote(stockCode, marketCode);
        BigDecimal currentPrice = null;
        if (realtimeDTO != null && realtimeDTO.getCurrentPrice() != null) {
            currentPrice = realtimeDTO.getCurrentPrice();
            BigDecimal upperLimit = currentPrice.multiply(BigDecimal.ONE.add(PRICE_LIMIT_RATE))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal lowerLimit = currentPrice.multiply(BigDecimal.ONE.subtract(PRICE_LIMIT_RATE))
                    .setScale(2, RoundingMode.HALF_UP);
            if (price.compareTo(upperLimit) > 0 || price.compareTo(lowerLimit) < 0) {
                throw new BusinessException(ErrorCode.PRICE_LIMIT,
                        String.format("买入价格超出范围，当前价: %.2f，允许范围: %.2f - %.2f",
                                currentPrice, lowerLimit, upperLimit));
            }
        }

        // 计算金额和费用
        BigDecimal amount = price.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal fee = amount.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        if (fee.compareTo(MIN_FEE) < 0) {
            fee = MIN_FEE;
        }
        BigDecimal totalCost = amount.add(fee);

        // 检查资金是否足够
        if (account.getAvailableCash().compareTo(totalCost) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_FUNDS,
                    String.format("资金不足，需要: %.2f，可用: %.2f", totalCost, account.getAvailableCash()));
        }

        LocalDateTime now = LocalDateTime.now();
        if (currentPrice != null && price.compareTo(currentPrice) < 0) {
            TradeOrder order = TradeOrder.builder()
                    .userId(userId)
                    .stockCode(stockCode)
                    .orderType("buy")
                    .price(price)
                    .quantity(quantity)
                    .amount(amount)
                    .fee(fee)
                    .status("pending")
                    .filledQuantity(0)
                    .build();
            tradeOrderMapper.insert(order);

            account.setAvailableCash(account.getAvailableCash().subtract(totalCost).setScale(2, RoundingMode.HALF_UP));
            account.setFrozenCash(nonNullMoney(account.getFrozenCash()).add(totalCost).setScale(2, RoundingMode.HALF_UP));
            refreshAccountSnapshot(account);
            accountMapper.updateById(account);

            log.info("Buy order pending: userId={}, stockCode={}, quantity={}, price={}, currentPrice={}",
                    userId, stockCode, quantity, price, currentPrice);
            return toOrderVO(order, stockInfo.getStockName());
        }

        // 创建订单
        TradeOrder order = TradeOrder.builder()
                .userId(userId)
                .stockCode(stockCode)
                .orderType("buy")
                .price(price)
                .quantity(quantity)
                .amount(amount)
                .fee(fee)
                .status("filled")
                .filledPrice(price)
                .filledQuantity(quantity)
                .filledTime(now)
                .build();
        tradeOrderMapper.insert(order);

        // 先更新账户可用资金，账户汇总数据在持仓落库后统一刷新
        account.setAvailableCash(account.getAvailableCash().subtract(totalCost).setScale(2, RoundingMode.HALF_UP));

        // 更新或创建持仓
        Position position = positionMapper.selectOne(
                new LambdaQueryWrapper<Position>()
                        .eq(Position::getUserId, userId)
                        .eq(Position::getStockCode, stockCode));
        if (position == null) {
            position = Position.builder()
                    .userId(userId)
                    .stockCode(stockCode)
                    .quantity(quantity)
                    .availableQuantity(quantity)
                    .costPrice(price)
                    .currentPrice(price)
                    .marketValue(amount)
                    .profit(BigDecimal.ZERO)
                    .profitRate(BigDecimal.ZERO)
                    .build();
            positionMapper.insert(position);
        } else {
            // 计算新成本价：(旧持仓数量*旧成本价 + 新数量*买入价) / 总数量
            int newQuantity = position.getQuantity() + quantity;
            BigDecimal newCostPrice = position.getCostPrice()
                    .multiply(BigDecimal.valueOf(position.getQuantity()))
                    .add(price.multiply(BigDecimal.valueOf(quantity)))
                    .divide(BigDecimal.valueOf(newQuantity), 4, RoundingMode.HALF_UP);

            position.setQuantity(newQuantity);
            position.setAvailableQuantity(position.getAvailableQuantity() + quantity);
            position.setCostPrice(newCostPrice);
            position.setCurrentPrice(price);
            BigDecimal marketValue = price.multiply(BigDecimal.valueOf(newQuantity)).setScale(2, RoundingMode.HALF_UP);
            position.setMarketValue(marketValue);
            BigDecimal profit = marketValue.subtract(
                    newCostPrice.multiply(BigDecimal.valueOf(newQuantity)).setScale(2, RoundingMode.HALF_UP));
            position.setProfit(profit.setScale(2, RoundingMode.HALF_UP));
            BigDecimal costTotal = newCostPrice.multiply(BigDecimal.valueOf(newQuantity));
            if (costTotal.compareTo(BigDecimal.ZERO) != 0) {
                position.setProfitRate(toRatePercent(profit, costTotal));
            }
            positionMapper.updateById(position);
        }

        // 创建交易记录
        TradeRecord record = TradeRecord.builder()
                .userId(userId)
                .orderId(order.getId())
                .stockCode(stockCode)
                .tradeType("buy")
                .price(price)
                .quantity(quantity)
                .amount(amount)
                .fee(fee)
                .tradeTime(now)
                .build();
        tradeRecordMapper.insert(record);

        refreshAccountSnapshot(account);
        accountMapper.updateById(account);

        log.info("Buy order filled: userId={}, stockCode={}, quantity={}, price={}, amount={}",
                userId, stockCode, quantity, price, totalCost);

        return toOrderVO(order, stockInfo.getStockName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO sell(Long userId, SellOrderDTO dto) {
        String stockCode = dto.getStockCode();
        BigDecimal price = dto.getPrice();
        int quantity = dto.getQuantity();

        // 检查 quantity 是否是 100 的整数倍
        if (quantity % 100 != 0) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY, "卖出数量必须是100的整数倍");
        }

        // 查持仓，检查可用数量
        Position position = positionMapper.selectOne(
                new LambdaQueryWrapper<Position>()
                        .eq(Position::getUserId, userId)
                        .eq(Position::getStockCode, stockCode));
        if (position == null || position.getAvailableQuantity() < quantity) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POSITION,
                    "持仓不足，可用数量: " + (position != null ? position.getAvailableQuantity() : 0));
        }

        // 获取股票信息
        StockInfo stockInfo = getStockInfo(stockCode);
        String marketCode = StockInfo.toEastMoneyMarketCode(stockInfo.getMarket());

        // 获取当前股价，检查价格范围
        StockRealtimeDTO realtimeDTO = eastMoneyClient.getRealtimeQuote(stockCode, marketCode);
        BigDecimal currentPrice = null;
        if (realtimeDTO != null && realtimeDTO.getCurrentPrice() != null) {
            currentPrice = realtimeDTO.getCurrentPrice();
            BigDecimal upperLimit = currentPrice.multiply(BigDecimal.ONE.add(PRICE_LIMIT_RATE))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal lowerLimit = currentPrice.multiply(BigDecimal.ONE.subtract(PRICE_LIMIT_RATE))
                    .setScale(2, RoundingMode.HALF_UP);
            if (price.compareTo(upperLimit) > 0 || price.compareTo(lowerLimit) < 0) {
                throw new BusinessException(ErrorCode.PRICE_LIMIT,
                        String.format("卖出价格超出范围，当前价: %.2f，允许范围: %.2f - %.2f",
                                currentPrice, lowerLimit, upperLimit));
            }
        }

        // 计算金额、费用和税
        BigDecimal amount = price.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal fee = amount.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        if (fee.compareTo(MIN_FEE) < 0) {
            fee = MIN_FEE;
        }
        BigDecimal tax = amount.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netIncome = amount.subtract(fee).subtract(tax);

        LocalDateTime now = LocalDateTime.now();
        if (currentPrice != null && price.compareTo(currentPrice) > 0) {
            TradeOrder order = TradeOrder.builder()
                    .userId(userId)
                    .stockCode(stockCode)
                    .orderType("sell")
                    .price(price)
                    .quantity(quantity)
                    .amount(amount)
                    .fee(fee.add(tax))
                    .status("pending")
                    .filledQuantity(0)
                    .build();
            tradeOrderMapper.insert(order);

            position.setAvailableQuantity(position.getAvailableQuantity() - quantity);
            positionMapper.updateById(position);

            Account account = getAccountByUserId(userId);
            refreshAccountSnapshot(account);
            accountMapper.updateById(account);

            log.info("Sell order pending: userId={}, stockCode={}, quantity={}, price={}, currentPrice={}",
                    userId, stockCode, quantity, price, currentPrice);
            return toOrderVO(order, stockInfo.getStockName());
        }

        // 创建订单
        TradeOrder order = TradeOrder.builder()
                .userId(userId)
                .stockCode(stockCode)
                .orderType("sell")
                .price(price)
                .quantity(quantity)
                .amount(amount)
                .fee(fee.add(tax))
                .status("filled")
                .filledPrice(price)
                .filledQuantity(quantity)
                .filledTime(now)
                .build();
        tradeOrderMapper.insert(order);

        // 先更新账户可用资金，账户汇总数据在持仓落库后统一刷新
        Account account = getAccountByUserId(userId);
        account.setAvailableCash(account.getAvailableCash().add(netIncome).setScale(2, RoundingMode.HALF_UP));

        // 更新持仓
        int newQuantity = position.getQuantity() - quantity;
        if (newQuantity <= 0) {
            positionMapper.deleteById(position.getId());
        } else {
            position.setQuantity(newQuantity);
            position.setAvailableQuantity(position.getAvailableQuantity() - quantity);
            position.setCurrentPrice(price);
            BigDecimal marketValue = price.multiply(BigDecimal.valueOf(newQuantity)).setScale(2, RoundingMode.HALF_UP);
            position.setMarketValue(marketValue);
            BigDecimal costTotal = position.getCostPrice().multiply(BigDecimal.valueOf(newQuantity));
            BigDecimal profit = marketValue.subtract(costTotal.setScale(2, RoundingMode.HALF_UP));
            position.setProfit(profit.setScale(2, RoundingMode.HALF_UP));
            if (costTotal.compareTo(BigDecimal.ZERO) != 0) {
                position.setProfitRate(toRatePercent(profit, costTotal));
            }
            positionMapper.updateById(position);
        }

        // 创建交易记录
        TradeRecord record = TradeRecord.builder()
                .userId(userId)
                .orderId(order.getId())
                .stockCode(stockCode)
                .tradeType("sell")
                .price(price)
                .quantity(quantity)
                .amount(amount)
                .fee(fee.add(tax))
                .tradeTime(now)
                .build();
        tradeRecordMapper.insert(record);

        refreshAccountSnapshot(account);
        accountMapper.updateById(account);

        log.info("Sell order filled: userId={}, stockCode={}, quantity={}, price={}, netIncome={}",
                userId, stockCode, quantity, price, netIncome);

        return toOrderVO(order, stockInfo.getStockName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO cancelOrder(Long userId, Long orderId) {
        TradeOrder order = tradeOrderMapper.selectOne(
                new LambdaQueryWrapper<TradeOrder>()
                        .eq(TradeOrder::getId, orderId)
                        .eq(TradeOrder::getUserId, userId));
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        if (!"pending".equalsIgnoreCase(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只有待成交订单可以撤销");
        }

        if ("buy".equalsIgnoreCase(order.getOrderType())) {
            Account account = getAccountByUserId(userId);
            BigDecimal releaseAmount = order.getAmount().add(nonNullMoney(order.getFee()));
            account.setAvailableCash(account.getAvailableCash().add(releaseAmount).setScale(2, RoundingMode.HALF_UP));
            BigDecimal frozenCash = nonNullMoney(account.getFrozenCash()).subtract(releaseAmount);
            if (frozenCash.compareTo(BigDecimal.ZERO) < 0) {
                frozenCash = BigDecimal.ZERO;
            }
            account.setFrozenCash(frozenCash.setScale(2, RoundingMode.HALF_UP));
            refreshAccountSnapshot(account);
            accountMapper.updateById(account);
        } else if ("sell".equalsIgnoreCase(order.getOrderType())) {
            Position position = positionMapper.selectOne(
                    new LambdaQueryWrapper<Position>()
                            .eq(Position::getUserId, userId)
                            .eq(Position::getStockCode, order.getStockCode()));
            if (position != null) {
                int availableQuantity = position.getAvailableQuantity() + order.getQuantity();
                position.setAvailableQuantity(Math.min(availableQuantity, position.getQuantity()));
                positionMapper.updateById(position);
            }
            Account account = getAccountByUserId(userId);
            refreshAccountSnapshot(account);
            accountMapper.updateById(account);
        }
        order.setStatus("cancelled");
        tradeOrderMapper.updateById(order);
        StockInfo stockInfo = findStockInfo(order.getStockCode());
        return toOrderVO(order, stockInfo != null ? stockInfo.getStockName() : order.getStockCode());
    }

    @Override
    public PageVO<OrderVO> getOrders(Long userId, String stockCode, String orderType, String status,
                                     int page, int pageSize) {
        LambdaQueryWrapper<TradeOrder> wrapper = new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getUserId, userId);
        if (StringUtils.hasText(stockCode)) {
            wrapper.eq(TradeOrder::getStockCode, stockCode);
        }
        if (StringUtils.hasText(orderType)) {
            wrapper.eq(TradeOrder::getOrderType, orderType.toLowerCase());
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(TradeOrder::getStatus, status.toLowerCase());
        }
        wrapper.orderByDesc(TradeOrder::getCreatedAt)
                .orderByDesc(TradeOrder::getId);

        Page<TradeOrder> pageResult = tradeOrderMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<OrderVO> records = new ArrayList<>();
        for (TradeOrder order : pageResult.getRecords()) {
            StockInfo stockInfo = findStockInfo(order.getStockCode());
            records.add(toOrderVO(order, stockInfo != null ? stockInfo.getStockName() : order.getStockCode()));
        }
        return new PageVO<>(pageResult.getTotal(), page, pageSize, records);
    }

    @Override
    public List<PositionVO> getPositions(Long userId) {
        List<Position> positions = positionMapper.selectList(
                new LambdaQueryWrapper<Position>().eq(Position::getUserId, userId));

        List<PositionVO> result = new ArrayList<>();
        BigDecimal totalPositionValue = BigDecimal.ZERO;

        for (Position position : positions) {
            String stockCode = position.getStockCode();
            StockInfo stockInfo = findStockInfo(stockCode);

            PositionVO vo = tradeStructMapper.toPositionVO(position, stockInfo != null ? stockInfo.getStockName() : stockCode);

            // 获取当前股价
            try {
                String marketCode = stockInfo != null
                        ? StockInfo.toEastMoneyMarketCode(stockInfo.getMarket())
                        : StockInfo.getMarketCode(stockCode);
                StockRealtimeDTO realtimeDTO = eastMoneyClient.getRealtimeQuote(stockCode, marketCode);
                if (realtimeDTO != null && realtimeDTO.getCurrentPrice() != null) {
                    BigDecimal currentPrice = realtimeDTO.getCurrentPrice();
                    BigDecimal marketValue = currentPrice.multiply(BigDecimal.valueOf(position.getQuantity()))
                            .setScale(2, RoundingMode.HALF_UP);
                    BigDecimal costTotal = position.getCostPrice().multiply(BigDecimal.valueOf(position.getQuantity()))
                            .setScale(2, RoundingMode.HALF_UP);
                    BigDecimal profit = marketValue.subtract(costTotal);
                    BigDecimal profitRate = costTotal.compareTo(BigDecimal.ZERO) != 0
                            ? toRatePercent(profit, costTotal)
                            : zeroRate();

                    vo.setCurrentPrice(currentPrice);
                    vo.setMarketValue(marketValue);
                    vo.setProfit(profit.setScale(2, RoundingMode.HALF_UP));
                    vo.setProfitRate(profitRate);

                    // 同步更新数据库中的持仓信息
                    position.setCurrentPrice(currentPrice);
                    position.setMarketValue(marketValue);
                    position.setProfit(profit.setScale(2, RoundingMode.HALF_UP));
                    position.setProfitRate(profitRate);
                    positionMapper.updateById(position);

                    totalPositionValue = totalPositionValue.add(marketValue);
                } else {
                    vo.setCurrentPrice(position.getCurrentPrice());
                    vo.setMarketValue(position.getMarketValue());
                    vo.setProfit(position.getProfit());
                    vo.setProfitRate(position.getProfitRate());
                    if (position.getMarketValue() != null) {
                        totalPositionValue = totalPositionValue.add(position.getMarketValue());
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to get realtime quote for position: {}", stockCode, e);
                vo.setCurrentPrice(position.getCurrentPrice());
                vo.setMarketValue(position.getMarketValue());
                vo.setProfit(position.getProfit());
                vo.setProfitRate(position.getProfitRate());
                if (position.getMarketValue() != null) {
                    totalPositionValue = totalPositionValue.add(position.getMarketValue());
                }
            }

            result.add(vo);
        }

        // 更新账户的 positionValue 和 totalAssets
        try {
            Account account = getAccountByUserId(userId);
            refreshAccountSnapshot(account);
            accountMapper.updateById(account);
        } catch (Exception e) {
            log.warn("Failed to update account assets: userId={}", userId, e);
        }

        return result;
    }

    @Override
    public PageVO<TradeRecordVO> getTradeRecords(Long userId, String stockCode, String tradeType,
                                                  String startDate, String endDate, int page, int pageSize) {
        validateTradeRecordQuery(tradeType, startDate, endDate, page, pageSize);

        LambdaQueryWrapper<TradeRecord> wrapper = new LambdaQueryWrapper<TradeRecord>()
                .eq(TradeRecord::getUserId, userId);

        if (StringUtils.hasText(stockCode)) {
            wrapper.eq(TradeRecord::getStockCode, stockCode);
        }
        if (StringUtils.hasText(tradeType)) {
            wrapper.eq(TradeRecord::getTradeType, tradeType.toLowerCase());
        }
        if (StringUtils.hasText(startDate)) {
            LocalDateTime start = parseTradeDateTime(startDate, "startDate", false);
            wrapper.ge(TradeRecord::getTradeTime, start);
        }
        if (StringUtils.hasText(endDate)) {
            LocalDateTime end = parseTradeDateTime(endDate, "endDate", true);
            wrapper.le(TradeRecord::getTradeTime, end);
        }
        wrapper.orderByDesc(TradeRecord::getTradeTime);

        Page<TradeRecord> pageResult = tradeRecordMapper.selectPage(
                new Page<>(page, pageSize), wrapper);

        List<TradeRecordVO> records = new ArrayList<>();
        for (TradeRecord record : pageResult.getRecords()) {
            StockInfo stockInfo = findStockInfo(record.getStockCode());

            records.add(tradeStructMapper.toTradeRecordVO(record,
                    stockInfo != null ? stockInfo.getStockName() : record.getStockCode()));
        }

        return new PageVO<>(pageResult.getTotal(), page, pageSize, records);
    }

    private void validateTradeRecordQuery(String tradeType, String startDate, String endDate, int page, int pageSize) {
        if (StringUtils.hasText(tradeType)
                && !"buy".equalsIgnoreCase(tradeType)
                && !"sell".equalsIgnoreCase(tradeType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "tradeType 仅支持 buy 或 sell");
        }
        if (page < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "page 最小为 1");
        }
        if (pageSize < 1 || pageSize > 100) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "pageSize 需在 1 到 100 之间");
        }

        LocalDateTime start = StringUtils.hasText(startDate) ? parseTradeDateTime(startDate, "startDate", false) : null;
        LocalDateTime end = StringUtils.hasText(endDate) ? parseTradeDateTime(endDate, "endDate", true) : null;
        if (start != null && end != null && start.isAfter(end)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "startDate 不能晚于 endDate");
        }
    }

    private LocalDateTime parseTradeDateTime(String rawDate, String fieldName, boolean endOfDayWhenDateOnly) {
        String value = rawDate == null ? null : rawDate.trim();
        if (!StringUtils.hasText(value)) {
            throw invalidTradeDateTime(fieldName);
        }
        try {
            LocalDate date = LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return endOfDayWhenDateOnly ? date.atTime(LocalTime.MAX) : date.atStartOfDay();
        } catch (DateTimeParseException ignored) {
        }

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        throw invalidTradeDateTime(fieldName);
    }

    private BusinessException invalidTradeDateTime(String fieldName) {
        return new BusinessException(ErrorCode.BAD_REQUEST,
                fieldName + " 格式必须为 yyyy-MM-dd / yyyy-MM-dd HH:mm / yyyy-MM-dd HH:mm:ss");
    }

    private Account getAccountByUserId(Long userId) {
        Account account = accountMapper.selectOne(
                new LambdaQueryWrapper<Account>().eq(Account::getUserId, userId));
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "账户不存在，请先注册");
        }
        return account;
    }

    private StockInfo getStockInfo(String stockCode) {
        StockInfo stockInfo = findStockInfo(stockCode);
        if (stockInfo == null) {
            throw new BusinessException(ErrorCode.STOCK_NOT_FOUND, "股票不存在: " + stockCode);
        }
        return stockInfo;
    }

    private StockInfo findStockInfo(String stockCode) {
        List<StockInfo> matches = stockInfoMapper.selectList(
                new LambdaQueryWrapper<StockInfo>().eq(StockInfo::getStockCode, stockCode));
        StockInfo preferred = selectPreferredStoredStockInfo(stockCode, matches);
        if (preferred != null && !needsRemoteRefresh(preferred)) {
            return preferred;
        }

        StockInfo refreshed = refreshStockInfoFromRemote(stockCode);
        if (refreshed != null) {
            return refreshed;
        }
        return preferred;
    }

    private StockInfo selectPreferredStoredStockInfo(String stockCode, List<StockInfo> matches) {
        if (matches == null || matches.isEmpty()) {
            return null;
        }
        String preferredMarket = preferredMarket(stockCode);
        for (StockInfo match : matches) {
            if (preferredMarket.equalsIgnoreCase(match.getMarket())
                    && StockInfo.isTradableAStock(match.getStockCode(), match.getMarket())) {
                return match;
            }
        }
        for (StockInfo match : matches) {
            if (StockInfo.isTradableAStock(match.getStockCode(), match.getMarket())) {
                return match;
            }
        }
        for (StockInfo match : matches) {
            if (preferredMarket.equalsIgnoreCase(match.getMarket())) {
                return match;
            }
        }
        return null;
    }

    private StockInfo upsertStockInfo(java.util.Map<String, String> remote, String source) {
        String stockCode = remote.get("stockCode");
        String market = remote.get("market");
        if (!StringUtils.hasText(stockCode) || !StringUtils.hasText(market)) {
            return null;
        }
        StockInfo existing = stockInfoMapper.selectOne(new LambdaQueryWrapper<StockInfo>()
                .eq(StockInfo::getStockCode, stockCode));
        if (existing != null) {
            existing.setMarket(market);
            if (StringUtils.hasText(remote.get("stockName"))) {
                existing.setStockName(remote.get("stockName"));
            }
            if (StringUtils.hasText(remote.get("industry"))) {
                existing.setIndustry(remote.get("industry"));
            }
            existing.setBoard(StockInfo.resolveBoard(stockCode, market));
            existing.setIsSt(StockInfo.isStStock(remote.get("stockName")) ? 1 : 0);
            existing.setIsDelisted(StockInfo.isDelistedStock(remote.get("stockName")) ? 1 : 0);
            existing.setSource(source);
            existing.setStatus(existing.getIsDelisted() != null && existing.getIsDelisted() == 1 ? 0 : 1);
            stockInfoMapper.updateById(existing);
            return existing;
        }

        StockInfo created = StockInfo.builder()
                .stockCode(stockCode)
                .stockName(remote.get("stockName"))
                .market(market)
                .board(StockInfo.resolveBoard(stockCode, market))
                .industry(remote.get("industry"))
                .isSt(StockInfo.isStStock(remote.get("stockName")) ? 1 : 0)
                .isDelisted(StockInfo.isDelistedStock(remote.get("stockName")) ? 1 : 0)
                .source(source)
                .status(1)
                .build();
        stockInfoMapper.insert(created);
        return created;
    }

    private String preferredMarket(String stockCode) {
        return stockCode != null && stockCode.startsWith("6") ? "SH" : "SZ";
    }

    private boolean needsRemoteRefresh(StockInfo stockInfo) {
        return stockInfo == null
                || TextEncodingUtils.hasCorruptedDisplayText(stockInfo.getStockName())
                || !StringUtils.hasText(stockInfo.getMarket());
    }

    private StockInfo refreshStockInfoFromRemote(String stockCode) {
        for (java.util.Map<String, String> remote : eastMoneyClient.searchStocks(stockCode)) {
            if (!stockCode.equals(remote.get("stockCode"))
                    || !StockInfo.isTradableAStock(stockCode, remote.get("market"))) {
                continue;
            }
            String market = remote.get("market");
            if (StringUtils.hasText(market)) {
                StockRealtimeDTO realtimeDTO = eastMoneyClient.getRealtimeQuote(stockCode, StockInfo.toEastMoneyMarketCode(market));
                if (realtimeDTO != null && StringUtils.hasText(realtimeDTO.getStockName())) {
                    remote.put("stockName", realtimeDTO.getStockName());
                }
            }
            return upsertStockInfo(remote, "TRADE");
        }
        return null;
    }

    private AccountVO toAccountVO(Long userId, Account account) {
        AccountVO vo = tradeStructMapper.toAccountVO(account);
        vo.setUserId(userId);
        return vo;
    }

    private void refreshAccountSnapshot(Account account) {
        List<Position> positions = positionMapper.selectList(
                new LambdaQueryWrapper<Position>().eq(Position::getUserId, account.getUserId()));

        BigDecimal positionValue = BigDecimal.ZERO;
        for (Position position : positions) {
            if (position.getMarketValue() != null) {
                positionValue = positionValue.add(position.getMarketValue());
            }
        }

        BigDecimal frozenCash = account.getFrozenCash() != null ? account.getFrozenCash() : BigDecimal.ZERO;
        BigDecimal totalAssets = account.getAvailableCash()
                .add(frozenCash)
                .add(positionValue)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalProfit = totalAssets.subtract(INITIAL_ASSETS).setScale(2, RoundingMode.HALF_UP);

        account.setPositionValue(positionValue.setScale(2, RoundingMode.HALF_UP));
        account.setTotalAssets(totalAssets);
        account.setTotalProfit(totalProfit);
        account.setProfitRate(toRatePercent(totalProfit, INITIAL_ASSETS));
    }

    private BigDecimal nonNullMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value;
    }

    private BigDecimal toRatePercent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return zeroRate();
        }
        return numerator.divide(denominator, RATE_SCALE + 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(RATE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal zeroRate() {
        return BigDecimal.ZERO.setScale(RATE_SCALE, RoundingMode.HALF_UP);
    }

    private OrderVO toOrderVO(TradeOrder order, String stockName) {
        return tradeStructMapper.toOrderVO(order, stockName);
    }
}
