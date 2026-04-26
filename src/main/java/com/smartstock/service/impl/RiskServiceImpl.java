package com.smartstock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartstock.entity.Account;
import com.smartstock.entity.Position;
import com.smartstock.mapper.AccountMapper;
import com.smartstock.mapper.PositionMapper;
import com.smartstock.service.RiskService;
import com.smartstock.vo.RiskAlertVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RiskServiceImpl implements RiskService {

    private static final BigDecimal CONCENTRATION_THRESHOLD = new BigDecimal("60.00");
    private static final BigDecimal DRAWDOWN_THRESHOLD = new BigDecimal("-10.00");

    private final AccountMapper accountMapper;
    private final PositionMapper positionMapper;

    @Override
    public List<RiskAlertVO> getAlerts(Long userId) {
        Account account = accountMapper.selectOne(new LambdaQueryWrapper<Account>().eq(Account::getUserId, userId));
        List<Position> positions = positionMapper.selectList(new LambdaQueryWrapper<Position>().eq(Position::getUserId, userId));
        List<RiskAlertVO> alerts = new ArrayList<>();
        if (account == null || account.getTotalAssets() == null || account.getTotalAssets().compareTo(BigDecimal.ZERO) == 0) {
            return alerts;
        }

        for (Position position : positions) {
            BigDecimal concentration = concentration(account, position);
            if (concentration.compareTo(CONCENTRATION_THRESHOLD) >= 0) {
                alerts.add(alert("concentration", "high", position.getStockCode(),
                        "单只股票持仓占比过高", concentration));
            }
            BigDecimal profitRate = position.getProfitRate();
            if (profitRate != null && profitRate.compareTo(DRAWDOWN_THRESHOLD) <= 0) {
                alerts.add(alert("drawdown", "medium", position.getStockCode(),
                        "持仓亏损幅度超过 10%", profitRate));
            }
        }
        return alerts;
    }

    private BigDecimal concentration(Account account, Position position) {
        BigDecimal marketValue = position.getMarketValue() == null ? BigDecimal.ZERO : position.getMarketValue();
        return marketValue.multiply(new BigDecimal("100"))
                .divide(account.getTotalAssets(), 2, RoundingMode.HALF_UP);
    }

    private RiskAlertVO alert(String type, String level, String stockCode, String message, BigDecimal value) {
        RiskAlertVO vo = new RiskAlertVO();
        vo.setAlertType(type);
        vo.setAlertLevel(level);
        vo.setStockCode(stockCode);
        vo.setMessage(message);
        vo.setValue(value);
        return vo;
    }
}
