package com.smartstock.service;

import com.smartstock.dto.BuyOrderDTO;
import com.smartstock.dto.SellOrderDTO;
import com.smartstock.vo.AccountVO;
import com.smartstock.vo.OrderVO;
import com.smartstock.vo.PageVO;
import com.smartstock.vo.PositionVO;
import com.smartstock.vo.TradeRecordVO;

import java.util.List;

public interface TradeService {

    AccountVO getAccount(Long userId);

    OrderVO buy(Long userId, BuyOrderDTO dto);

    OrderVO sell(Long userId, SellOrderDTO dto);

    OrderVO cancelOrder(Long userId, Long orderId);

    PageVO<OrderVO> getOrders(Long userId, String stockCode, String orderType, String status,
                              int page, int pageSize);

    List<PositionVO> getPositions(Long userId);

    PageVO<TradeRecordVO> getTradeRecords(Long userId, String stockCode, String tradeType,
                                          String startDate, String endDate, int page, int pageSize);
}
