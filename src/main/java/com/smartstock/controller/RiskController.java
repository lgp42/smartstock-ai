package com.smartstock.controller;

import com.smartstock.common.Result;
import com.smartstock.service.RiskService;
import com.smartstock.util.UserContext;
import com.smartstock.vo.RiskAlertVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
public class RiskController {

    private final RiskService riskService;

    @GetMapping("/alerts")
    public Result<List<RiskAlertVO>> getAlerts(HttpServletRequest request) {
        Long userId = UserContext.getUserId(request);
        return Result.ok(riskService.getAlerts(userId));
    }
}
