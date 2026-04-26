package com.smartstock.controller;

import com.smartstock.common.Result;
import com.smartstock.dto.BacktestRunDTO;
import com.smartstock.service.BacktestService;
import com.smartstock.vo.BacktestResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/backtest")
@RequiredArgsConstructor
public class BacktestController {

    private final BacktestService backtestService;

    @PostMapping("/run")
    public Result<BacktestResultVO> run(@Valid @RequestBody BacktestRunDTO dto) {
        return Result.ok(backtestService.runBuyAndHold(dto));
    }
}
