package com.smartstock.controller;

import com.smartstock.common.BusinessException;
import com.smartstock.common.ErrorCode;
import com.smartstock.common.Result;
import com.smartstock.dto.BuyOrderDTO;
import com.smartstock.dto.SellOrderDTO;
import com.smartstock.service.TradeService;
import com.smartstock.util.UserContext;
import com.smartstock.vo.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trade")
@RequiredArgsConstructor
@Tag(name = "Trade", description = "模拟交易、资产与订单接口")
public class TradeController {

    private final TradeService tradeService;

    @GetMapping("/account")
    public Result<AccountVO> getAccount(HttpServletRequest request) {
        Long userId = UserContext.getUserId(request);
        return Result.ok(tradeService.getAccount(userId));
    }

    @PostMapping("/buy")
    public Result<OrderVO> buy(HttpServletRequest request,
                               @Valid @RequestBody BuyOrderDTO dto) {
        Long userId = UserContext.getUserId(request);
        return Result.ok(tradeService.buy(userId, dto));
    }

    @PostMapping("/sell")
    public Result<OrderVO> sell(HttpServletRequest request,
                                @Valid @RequestBody SellOrderDTO dto) {
        Long userId = UserContext.getUserId(request);
        return Result.ok(tradeService.sell(userId, dto));
    }

    @DeleteMapping("/orders/{orderId}")
    public Result<OrderVO> cancelOrder(HttpServletRequest request,
                                       @PathVariable Long orderId) {
        Long userId = UserContext.getUserId(request);
        return Result.ok(tradeService.cancelOrder(userId, orderId));
    }

    @GetMapping("/orders")
    public Result<PageVO<OrderVO>> getOrders(HttpServletRequest request,
                                             @RequestParam(required = false) String stockCode,
                                             @RequestParam(required = false) String orderType,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int pageSize) {
        validateOrderType(orderType);
        validateOrderStatus(status);
        validatePage(page, pageSize);
        Long userId = UserContext.getUserId(request);
        return Result.ok(tradeService.getOrders(userId, stockCode, orderType, status, page, pageSize));
    }

    @GetMapping("/positions")
    public Result<List<PositionVO>> getPositions(HttpServletRequest request) {
        Long userId = UserContext.getUserId(request);
        return Result.ok(tradeService.getPositions(userId));
    }

    @GetMapping("/records")
    public Result<PageVO<TradeRecordVO>> getTradeRecords(
            HttpServletRequest request,
            @RequestParam(required = false) String stockCode,
            @RequestParam(required = false) String tradeType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        validateTradeType(tradeType);
        validatePage(page, pageSize);
        Long userId = UserContext.getUserId(request);
        return Result.ok(tradeService.getTradeRecords(userId, stockCode, tradeType,
                startDate, endDate, page, pageSize));
    }

    private void validateTradeType(String tradeType) {
        if (tradeType == null) {
            return;
        }
        if (!"buy".equalsIgnoreCase(tradeType) && !"sell".equalsIgnoreCase(tradeType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "tradeType 仅支持 buy 或 sell");
        }
    }

    private void validateOrderType(String orderType) {
        if (orderType == null) {
            return;
        }
        if (!"buy".equalsIgnoreCase(orderType) && !"sell".equalsIgnoreCase(orderType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "orderType 仅支持 buy 或 sell");
        }
    }

    private void validateOrderStatus(String status) {
        if (status == null) {
            return;
        }
        if (!"pending".equalsIgnoreCase(status)
                && !"filled".equalsIgnoreCase(status)
                && !"cancelled".equalsIgnoreCase(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "status 仅支持 pending/filled/cancelled");
        }
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "page 最小为 1");
        }
        if (pageSize < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "pageSize 最小为 1");
        }
        if (pageSize > 100) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "pageSize 最大为 100");
        }
    }
}
