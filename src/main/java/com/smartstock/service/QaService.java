package com.smartstock.service;

import com.smartstock.dto.QaAskRequestDTO;
import com.smartstock.vo.QaAnswerVO;
import com.smartstock.vo.QaHistoryVO;
import com.smartstock.vo.QaSessionDetailVO;
import com.smartstock.vo.QaSessionVO;

import java.util.List;

public interface QaService {

    QaAnswerVO ask(Long userId, QaAskRequestDTO request);

    List<QaHistoryVO> getHistory(Long userId, int limit);

    List<QaSessionVO> getSessions(Long userId, String stockCode, int limit);

    QaSessionDetailVO getSessionDetail(Long userId, String sessionId, int limit);
}
