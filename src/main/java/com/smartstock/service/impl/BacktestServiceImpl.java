package com.smartstock.service.impl;

import com.smartstock.common.BusinessException;
import com.smartstock.common.ErrorCode;
import com.smartstock.dto.BacktestRunDTO;
import com.smartstock.service.BacktestService;
import com.smartstock.service.MarketService;
import com.smartstock.vo.BacktestResultVO;
import com.smartstock.vo.KlineVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BacktestServiceImpl implements BacktestService {

    private final MarketService marketService;

    @Override
    public BacktestResultVO runBuyAndHold(BacktestRunDTO dto) {
        int limit = dto.getLimit() == null ? 120 : dto.getLimit();
        List<KlineVO> klines = marketService.getKlineData(dto.getStockCode(), "day", limit);
        if (klines.size() < 2) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "回测至少需要 2 条 K 线");
        }

        BigDecimal firstClose = closeAt(klines, 0);
        BigDecimal lastClose = closeAt(klines, klines.size() - 1);
        BigDecimal finalCapital = dto.getInitialCapital()
                .multiply(lastClose)
                .divide(firstClose, 2, RoundingMode.HALF_UP);
        BigDecimal totalReturn = finalCapital.subtract(dto.getInitialCapital()).setScale(2, RoundingMode.HALF_UP);

        BacktestResultVO vo = new BacktestResultVO();
        vo.setStockCode(dto.getStockCode());
        vo.setStrategyType("buy_hold");
        vo.setInitialCapital(dto.getInitialCapital().setScale(2, RoundingMode.HALF_UP));
        vo.setFinalCapital(finalCapital);
        vo.setTotalReturn(totalReturn);
        vo.setReturnRate(percent(totalReturn, dto.getInitialCapital()));
        vo.setMaxDrawdown(maxDrawdown(klines, dto.getInitialCapital(), firstClose));
        vo.setTradeCount(2);
        return vo;
    }

    private BigDecimal maxDrawdown(List<KlineVO> klines, BigDecimal initialCapital, BigDecimal firstClose) {
        BigDecimal peak = initialCapital;
        BigDecimal maxDrawdown = BigDecimal.ZERO;
        for (KlineVO kline : klines) {
            BigDecimal equity = initialCapital.multiply(kline.getClose())
                    .divide(firstClose, 2, RoundingMode.HALF_UP);
            if (equity.compareTo(peak) > 0) {
                peak = equity;
            }
            BigDecimal drawdown = peak.subtract(equity)
                    .multiply(new BigDecimal("100"))
                    .divide(peak, 4, RoundingMode.HALF_UP);
            if (drawdown.compareTo(maxDrawdown) > 0) {
                maxDrawdown = drawdown;
            }
        }
        return maxDrawdown;
    }

    private BigDecimal closeAt(List<KlineVO> klines, int index) {
        BigDecimal close = klines.get(index).getClose();
        if (close == null || close.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "K 线收盘价无效");
        }
        return close;
    }

    private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        return numerator.multiply(new BigDecimal("100"))
                .divide(denominator, 4, RoundingMode.HALF_UP);
    }
}
