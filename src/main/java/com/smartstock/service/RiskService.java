package com.smartstock.service;

import com.smartstock.vo.RiskAlertVO;

import java.util.List;

public interface RiskService {

    List<RiskAlertVO> getAlerts(Long userId);
}
