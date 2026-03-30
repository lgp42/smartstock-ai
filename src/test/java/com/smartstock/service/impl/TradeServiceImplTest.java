package com.smartstock.service.impl;

import com.smartstock.client.EastMoneyClient;
import com.smartstock.client.StockRealtimeDTO;
import com.smartstock.common.BusinessException;
import com.smartstock.convert.TradeStructMapper;
import com.smartstock.dto.BuyOrderDTO;
import com.smartstock.dto.SellOrderDTO;
import com.smartstock.entity.Account;
import com.smartstock.entity.Position;
import com.smartstock.entity.StockInfo;
import com.smartstock.entity.TradeOrder;
import com.smartstock.mapper.AccountMapper;
import com.smartstock.mapper.PositionMapper;
import com.smartstock.mapper.StockInfoMapper;
import com.smartstock.mapper.TradeOrderMapper;
import com.smartstock.mapper.TradeRecordMapper;
import com.smartstock.vo.AccountVO;
import com.smartstock.vo.OrderVO;
import com.smartstock.vo.PageVO;
import com.smartstock.vo.TradeRecordVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeServiceImplTest {

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private TradeOrderMapper tradeOrderMapper;

    @Mock
    private PositionMapper positionMapper;

    @Mock
    private TradeRecordMapper tradeRecordMapper;

    @Mock
    private StockInfoMapper stockInfoMapper;

    @Mock
    private EastMoneyClient eastMoneyClient;

    @Mock
    private TradeStructMapper tradeStructMapper;

    private TradeServiceImpl tradeService;

    @BeforeEach
    void setUp() {
        tradeService = new TradeServiceImpl(
                accountMapper,
                tradeOrderMapper,
                positionMapper,
                tradeRecordMapper,
                stockInfoMapper,
                eastMoneyClient,
                tradeStructMapper
        );
        org.mockito.Mockito.lenient().when(tradeStructMapper.toOrderVO(any(), any())).thenAnswer(invocation -> {
            TradeOrder order = invocation.getArgument(0);
            String stockName = invocation.getArgument(1);
            OrderVO vo = new OrderVO();
            vo.setOrderId(order.getId());
            vo.setStockCode(order.getStockCode());
            vo.setStockName(stockName);
            vo.setOrderType(order.getOrderType());
            vo.setPrice(order.getPrice());
            vo.setQuantity(order.getQuantity());
            vo.setAmount(order.getAmount());
            vo.setFee(order.getFee());
            vo.setStatus(order.getStatus());
            vo.setFilledPrice(order.getFilledPrice());
            vo.setFilledQuantity(order.getFilledQuantity());
            vo.setFilledTime(order.getFilledTime());
            return vo;
        });
        org.mockito.Mockito.lenient().when(tradeStructMapper.toAccountVO(any())).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            AccountVO vo = new AccountVO();
            vo.setUserId(account.getUserId());
            vo.setTotalAssets(account.getTotalAssets());
            vo.setAvailableCash(account.getAvailableCash());
            vo.setFrozenCash(account.getFrozenCash());
            vo.setPositionValue(account.getPositionValue());
            vo.setTotalProfit(account.getTotalProfit());
            vo.setProfitRate(account.getProfitRate());
            return vo;
        });
        org.mockito.Mockito.lenient().when(tradeStructMapper.toPositionVO(any(), any())).thenAnswer(invocation -> {
            Position position = invocation.getArgument(0);
            String stockName = invocation.getArgument(1);
            com.smartstock.vo.PositionVO vo = new com.smartstock.vo.PositionVO();
            vo.setStockCode(position.getStockCode());
            vo.setStockName(stockName);
            vo.setQuantity(position.getQuantity());
            vo.setAvailableQuantity(position.getAvailableQuantity());
            vo.setCostPrice(position.getCostPrice());
            vo.setCurrentPrice(position.getCurrentPrice());
            vo.setMarketValue(position.getMarketValue());
            vo.setProfit(position.getProfit());
            vo.setProfitRate(position.getProfitRate());
            return vo;
        });
        org.mockito.Mockito.lenient().when(tradeStructMapper.toTradeRecordVO(any(), any())).thenAnswer(invocation -> {
            com.smartstock.entity.TradeRecord record = invocation.getArgument(0);
            String stockName = invocation.getArgument(1);
            TradeRecordVO vo = new TradeRecordVO();
            vo.setRecordId(record.getId());
            vo.setStockCode(record.getStockCode());
            vo.setStockName(stockName);
            vo.setTradeType(record.getTradeType());
            vo.setPrice(record.getPrice());
            vo.setQuantity(record.getQuantity());
            vo.setAmount(record.getAmount());
            vo.setFee(record.getFee());
            vo.setTradeTime(record.getTradeTime());
            return vo;
        });
    }

    @Test
    void buyShouldRefreshAccountSnapshotImmediately() {
        Long userId = 1L;
        BuyOrderDTO dto = new BuyOrderDTO();
        dto.setStockCode("600519");
        dto.setPrice(new BigDecimal("100.00"));
        dto.setQuantity(100);

        Account account = baseAccount(userId, "1000000.00");
        StockInfo stockInfo = StockInfo.builder().stockCode("600519").stockName("贵州茅台").market("SH").build();
        StockRealtimeDTO realtimeDTO = new StockRealtimeDTO();
        realtimeDTO.setCurrentPrice(new BigDecimal("100.00"));

        AtomicReference<Position> storedPosition = new AtomicReference<>();

        when(accountMapper.selectOne(any())).thenReturn(account);
        when(stockInfoMapper.selectList(any())).thenReturn(List.of(stockInfo));
        when(eastMoneyClient.getRealtimeQuote("600519", "1")).thenReturn(realtimeDTO);
        when(positionMapper.selectOne(any())).thenReturn(null);
        when(positionMapper.selectList(any())).thenAnswer(invocation ->
                storedPosition.get() == null ? List.of() : List.of(storedPosition.get()));

        doAnswer(invocation -> {
            TradeOrder order = invocation.getArgument(0);
            order.setId(101L);
            return 1;
        }).when(tradeOrderMapper).insert(any(TradeOrder.class));
        doAnswer(invocation -> {
            Position position = invocation.getArgument(0);
            position.setId(201L);
            storedPosition.set(position);
            return 1;
        }).when(positionMapper).insert(any(Position.class));

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        when(accountMapper.updateById(accountCaptor.capture())).thenReturn(1);

        OrderVO orderVO = tradeService.buy(userId, dto);

        assertNotNull(orderVO);
        assertMoneyEquals("989995.00", accountCaptor.getValue().getAvailableCash());
        assertMoneyEquals("10000.00", accountCaptor.getValue().getPositionValue());
        assertMoneyEquals("999995.00", accountCaptor.getValue().getTotalAssets());
        assertMoneyEquals("-5.00", accountCaptor.getValue().getTotalProfit());
        assertMoneyEquals("-0.0005", accountCaptor.getValue().getProfitRate());
    }

    @Test
    void sellShouldRefreshAccountSnapshotImmediately() {
        Long userId = 2L;
        SellOrderDTO dto = new SellOrderDTO();
        dto.setStockCode("600519");
        dto.setPrice(new BigDecimal("100.00"));
        dto.setQuantity(100);

        Account account = baseAccount(userId, "989995.00");
        account.setPositionValue(new BigDecimal("10000.00"));
        account.setTotalAssets(new BigDecimal("999995.00"));
        account.setTotalProfit(new BigDecimal("-5.00"));

        StockInfo stockInfo = StockInfo.builder().stockCode("600519").stockName("贵州茅台").market("SH").build();
        StockRealtimeDTO realtimeDTO = new StockRealtimeDTO();
        realtimeDTO.setCurrentPrice(new BigDecimal("100.00"));

        Position position = Position.builder()
                .id(301L)
                .userId(userId)
                .stockCode("600519")
                .quantity(100)
                .availableQuantity(100)
                .costPrice(new BigDecimal("100.00"))
                .currentPrice(new BigDecimal("100.00"))
                .marketValue(new BigDecimal("10000.00"))
                .profit(BigDecimal.ZERO)
                .profitRate(BigDecimal.ZERO)
                .build();
        AtomicReference<Position> storedPosition = new AtomicReference<>(position);

        when(positionMapper.selectOne(any())).thenReturn(position);
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(stockInfoMapper.selectList(any())).thenReturn(List.of(stockInfo));
        when(eastMoneyClient.getRealtimeQuote("600519", "1")).thenReturn(realtimeDTO);
        when(positionMapper.selectList(any())).thenAnswer(invocation ->
                storedPosition.get() == null ? List.of() : List.of(storedPosition.get()));

        doAnswer(invocation -> {
            TradeOrder order = invocation.getArgument(0);
            order.setId(102L);
            return 1;
        }).when(tradeOrderMapper).insert(any(TradeOrder.class));
        when(positionMapper.deleteById(anyLong())).thenAnswer(invocation -> {
            storedPosition.set(null);
            return 1;
        });

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        when(accountMapper.updateById(accountCaptor.capture())).thenReturn(1);

        OrderVO orderVO = tradeService.sell(userId, dto);

        assertNotNull(orderVO);
        assertMoneyEquals("999980.00", accountCaptor.getValue().getAvailableCash());
        assertMoneyEquals("0.00", accountCaptor.getValue().getPositionValue());
        assertMoneyEquals("999980.00", accountCaptor.getValue().getTotalAssets());
        assertMoneyEquals("-20.00", accountCaptor.getValue().getTotalProfit());
        assertMoneyEquals("-0.0020", accountCaptor.getValue().getProfitRate());
    }

    @Test
    void getAccountShouldRefreshSnapshotFromStoredPositions() {
        Long userId = 9L;
        Account account = baseAccount(userId, "998000.00");
        account.setTotalAssets(new BigDecimal("1000000.00"));
        account.setPositionValue(BigDecimal.ZERO);
        account.setTotalProfit(BigDecimal.ZERO);
        account.setProfitRate(BigDecimal.ZERO);

        Position position = Position.builder()
                .id(901L)
                .userId(userId)
                .stockCode("000001")
                .quantity(100)
                .availableQuantity(100)
                .costPrice(new BigDecimal("20.00"))
                .currentPrice(new BigDecimal("20.50"))
                .marketValue(new BigDecimal("2050.00"))
                .profit(new BigDecimal("50.00"))
                .profitRate(new BigDecimal("2.5000"))
                .build();

        when(accountMapper.selectOne(any())).thenReturn(account);
        when(positionMapper.selectList(any())).thenReturn(List.of(position));

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        when(accountMapper.updateById(accountCaptor.capture())).thenReturn(1);

        AccountVO accountVO = tradeService.getAccount(userId);

        assertMoneyEquals("2050.00", accountVO.getPositionValue());
        assertMoneyEquals("1000050.00", accountVO.getTotalAssets());
        assertMoneyEquals("50.00", accountVO.getTotalProfit());
        assertMoneyEquals("0.0050", accountVO.getProfitRate());
        assertMoneyEquals("0.0050", accountCaptor.getValue().getProfitRate());
    }

    @Test
    void getTradeRecordsShouldRejectInvalidDateFormat() {
        assertThrows(BusinessException.class,
                () -> tradeService.getTradeRecords(1L, null, null, "2026-13-40", null, 1, 20));
    }

    @Test
    void getTradeRecordsShouldRejectStartDateAfterEndDate() {
        assertThrows(BusinessException.class,
                () -> tradeService.getTradeRecords(1L, null, null, "2026-03-20", "2026-03-01", 1, 20));
    }

    @Test
    void getTradeRecordsShouldAcceptDateTimeRangeToSeconds() {
        when(tradeRecordMapper.selectPage(any(), any())).thenReturn(new Page<>(1, 20));

        PageVO<TradeRecordVO> result = assertDoesNotThrow(() ->
                tradeService.getTradeRecords(1L, null, null,
                        "2026-03-18 09:30:15", "2026-03-18 14:56:59", 1, 20));

        assertNotNull(result);
        assertEquals(0L, result.getTotal());
        assertEquals(0, result.getRecords().size());
    }

    @Test
    void getTradeRecordsShouldAcceptIsoLocalDateTimeRange() {
        when(tradeRecordMapper.selectPage(any(), any())).thenReturn(new Page<>(1, 20));

        PageVO<TradeRecordVO> result = assertDoesNotThrow(() ->
                tradeService.getTradeRecords(1L, null, null,
                        "2026-03-18T09:30:15", "2026-03-18T14:56:59", 1, 20));

        assertNotNull(result);
        assertEquals(0L, result.getTotal());
    }

    @Test
    void getTradeRecordsShouldRejectInvalidDateTimeFormat() {
        assertThrows(BusinessException.class,
                () -> tradeService.getTradeRecords(1L, null, null, "2026/03/18 09:30", null, 1, 20));
    }

    @Test
    void getPositionsShouldPreferTradableAStockWhenIndexSharesSameCode() {
        Long userId = 7L;
        Position position = Position.builder()
                .id(301L)
                .userId(userId)
                .stockCode("000001")
                .quantity(100)
                .availableQuantity(100)
                .costPrice(new BigDecimal("10.98"))
                .currentPrice(new BigDecimal("10.98"))
                .marketValue(new BigDecimal("1098.00"))
                .profit(BigDecimal.ZERO)
                .profitRate(BigDecimal.ZERO)
                .build();
        Account account = baseAccount(userId, "998902.00");
        StockRealtimeDTO realtimeDTO = new StockRealtimeDTO();
        realtimeDTO.setCurrentPrice(new BigDecimal("10.96"));

        when(positionMapper.selectList(any())).thenReturn(List.of(position));
        when(stockInfoMapper.selectList(any())).thenReturn(List.of(
                StockInfo.builder().stockCode("000001").stockName("上证指数").market("SH").industry("指数").build(),
                StockInfo.builder().stockCode("000001").stockName("平安银行").market("SZ").industry("银行").build()
        ));
        when(eastMoneyClient.getRealtimeQuote("000001", "0")).thenReturn(realtimeDTO);
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(accountMapper.updateById(any(Account.class))).thenReturn(1);
        when(positionMapper.updateById(any(Position.class))).thenReturn(1);

        var result = tradeService.getPositions(userId);

        assertEquals(1, result.size());
        assertEquals("平安银行", result.get(0).getStockName());
        assertEquals(new BigDecimal("10.96"), result.get(0).getCurrentPrice());
        assertTrue(result.get(0).getProfit().compareTo(BigDecimal.ZERO) < 0);
    }

    @Test
    void getPositionsShouldRepairCorruptedStoredIndexRecord() {
        Long userId = 8L;
        Position position = Position.builder()
                .id(401L)
                .userId(userId)
                .stockCode("000001")
                .quantity(100)
                .availableQuantity(100)
                .costPrice(new BigDecimal("10.98"))
                .currentPrice(new BigDecimal("10.98"))
                .marketValue(new BigDecimal("1098.00"))
                .profit(BigDecimal.ZERO)
                .profitRate(BigDecimal.ZERO)
                .build();
        Account account = baseAccount(userId, "998902.00");
        StockInfo storedIndex = StockInfo.builder().id(2L).stockCode("000001").stockName("上证指数").market("SH").industry("指数").build();
        StockRealtimeDTO realtimeDTO = new StockRealtimeDTO();
        realtimeDTO.setCurrentPrice(new BigDecimal("10.96"));

        when(positionMapper.selectList(any())).thenReturn(List.of(position));
        when(stockInfoMapper.selectList(any())).thenReturn(List.of(storedIndex));
        when(stockInfoMapper.selectOne(any())).thenReturn(storedIndex);
        when(eastMoneyClient.searchStocks("000001")).thenReturn(List.of(
                java.util.Map.of("stockCode", "000001", "stockName", "平安银行", "market", "SZ", "industry", "银行")
        ));
        when(eastMoneyClient.getRealtimeQuote("000001", "0")).thenReturn(realtimeDTO);
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(accountMapper.updateById(any(Account.class))).thenReturn(1);
        when(positionMapper.updateById(any(Position.class))).thenReturn(1);
        when(stockInfoMapper.updateById(any(StockInfo.class))).thenReturn(1);

        var result = tradeService.getPositions(userId);

        assertEquals(1, result.size());
        assertEquals("平安银行", result.get(0).getStockName());
        assertEquals(new BigDecimal("10.96"), result.get(0).getCurrentPrice());
    }

    private Account baseAccount(Long userId, String availableCash) {
        return Account.builder()
                .id(1L)
                .userId(userId)
                .totalAssets(new BigDecimal("1000000.00"))
                .availableCash(new BigDecimal(availableCash))
                .frozenCash(BigDecimal.ZERO)
                .positionValue(BigDecimal.ZERO)
                .totalProfit(BigDecimal.ZERO)
                .profitRate(BigDecimal.ZERO)
                .build();
    }

    private void assertMoneyEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
