package com.smartstock.convert;

import com.smartstock.entity.Account;
import com.smartstock.entity.Position;
import com.smartstock.entity.TradeOrder;
import com.smartstock.entity.TradeRecord;
import com.smartstock.vo.AccountVO;
import com.smartstock.vo.OrderVO;
import com.smartstock.vo.PositionVO;
import com.smartstock.vo.TradeRecordVO;
import org.springframework.stereotype.Component;

@Component
public class TradeStructMapper {

    public AccountVO toAccountVO(Account account) {
        if (account == null) {
            return null;
        }
        AccountVO vo = new AccountVO();
        vo.setUserId(account.getUserId());
        vo.setTotalAssets(account.getTotalAssets());
        vo.setAvailableCash(account.getAvailableCash());
        vo.setFrozenCash(account.getFrozenCash());
        vo.setPositionValue(account.getPositionValue());
        vo.setTotalProfit(account.getTotalProfit());
        vo.setProfitRate(account.getProfitRate());
        return vo;
    }

    public PositionVO toPositionVO(Position position, String stockName) {
        if (position == null) {
            return null;
        }
        PositionVO vo = new PositionVO();
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
    }

    public OrderVO toOrderVO(TradeOrder order, String stockName) {
        if (order == null) {
            return null;
        }
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
    }

    public TradeRecordVO toTradeRecordVO(TradeRecord record, String stockName) {
        if (record == null) {
            return null;
        }
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
    }
}
