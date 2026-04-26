package com.smartstock.service;

import com.smartstock.dto.BacktestRunDTO;
import com.smartstock.vo.BacktestResultVO;

public interface BacktestService {

    BacktestResultVO runBuyAndHold(BacktestRunDTO dto);
}
