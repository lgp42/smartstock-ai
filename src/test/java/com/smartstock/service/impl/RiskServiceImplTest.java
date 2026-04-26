package com.smartstock.service.impl;

import com.smartstock.entity.Account;
import com.smartstock.entity.Position;
import com.smartstock.mapper.AccountMapper;
import com.smartstock.mapper.PositionMapper;
import com.smartstock.vo.RiskAlertVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskServiceImplTest {

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private PositionMapper positionMapper;

    private RiskServiceImpl riskService;

    @BeforeEach
    void setUp() {
        riskService = new RiskServiceImpl(accountMapper, positionMapper);
    }

    @Test
    void getAlertsShouldWarnWhenSinglePositionExceedsSixtyPercent() {
        when(accountMapper.selectOne(any())).thenReturn(account("100000.00"));
        when(positionMapper.selectList(any())).thenReturn(List.of(position("600519", "70000.00", "3.00")));

        List<RiskAlertVO> alerts = riskService.getAlerts(1L);

        assertEquals(1, alerts.size());
        assertEquals("concentration", alerts.get(0).getAlertType());
        assertEquals("high", alerts.get(0).getAlertLevel());
    }

    @Test
    void getAlertsShouldWarnWhenPositionDrawdownExceedsTenPercent() {
        when(accountMapper.selectOne(any())).thenReturn(account("100000.00"));
        when(positionMapper.selectList(any())).thenReturn(List.of(position("600519", "20000.00", "-12.50")));

        List<RiskAlertVO> alerts = riskService.getAlerts(1L);

        assertEquals(1, alerts.size());
        assertEquals("drawdown", alerts.get(0).getAlertType());
        assertEquals("medium", alerts.get(0).getAlertLevel());
    }

    private Account account(String totalAssets) {
        return Account.builder()
                .userId(1L)
                .totalAssets(new BigDecimal(totalAssets))
                .build();
    }

    private Position position(String stockCode, String marketValue, String profitRate) {
        return Position.builder()
                .stockCode(stockCode)
                .marketValue(new BigDecimal(marketValue))
                .profitRate(new BigDecimal(profitRate))
                .build();
    }
}
