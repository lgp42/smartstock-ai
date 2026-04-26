package com.smartstock.service.impl;

import com.smartstock.dto.BacktestRunDTO;
import com.smartstock.service.MarketService;
import com.smartstock.vo.BacktestResultVO;
import com.smartstock.vo.KlineVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BacktestServiceImplTest {

    @Mock
    private MarketService marketService;

    private BacktestServiceImpl backtestService;

    @BeforeEach
    void setUp() {
        backtestService = new BacktestServiceImpl(marketService);
    }

    @Test
    void runBuyAndHoldShouldCalculateReturnFromFirstAndLastClose() {
        BacktestRunDTO dto = new BacktestRunDTO();
        dto.setStockCode("600519");
        dto.setInitialCapital(new BigDecimal("100000.00"));
        dto.setLimit(3);

        when(marketService.getKlineData("600519", "day", 3)).thenReturn(List.of(
                kline("2026-01-01", "10.00"),
                kline("2026-01-02", "8.00"),
                kline("2026-01-03", "12.00")
        ));

        BacktestResultVO result = backtestService.runBuyAndHold(dto);

        assertEquals("600519", result.getStockCode());
        assertEquals(new BigDecimal("100000.00"), result.getInitialCapital());
        assertEquals(new BigDecimal("120000.00"), result.getFinalCapital());
        assertEquals(new BigDecimal("20000.00"), result.getTotalReturn());
        assertEquals(new BigDecimal("20.0000"), result.getReturnRate());
        assertEquals(new BigDecimal("20.0000"), result.getMaxDrawdown());
    }

    private KlineVO kline(String date, String close) {
        KlineVO vo = new KlineVO();
        vo.setDate(date);
        vo.setClose(new BigDecimal(close));
        return vo;
    }
}
