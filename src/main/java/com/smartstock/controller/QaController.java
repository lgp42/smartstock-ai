package com.smartstock.controller;

import com.smartstock.common.Result;
import com.smartstock.dto.QaAskRequestDTO;
import com.smartstock.service.QaService;
import com.smartstock.util.UserContext;
import com.smartstock.vo.QaAnswerVO;
import com.smartstock.vo.QaHistoryVO;
import com.smartstock.vo.QaSessionDetailVO;
import com.smartstock.vo.QaSessionVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
@Tag(name = "QA", description = "AI 问答接口")
public class QaController {

    private final QaService qaService;

    @PostMapping("/ask")
    public Result<QaAnswerVO> ask(HttpServletRequest request,
                                  @Valid @RequestBody QaAskRequestDTO dto) {
        Long userId = UserContext.getUserId(request);
        return Result.ok(qaService.ask(userId, dto));
    }

    @GetMapping("/history")
    public Result<List<QaHistoryVO>> getHistory(HttpServletRequest request,
                                                @RequestParam(defaultValue = "10") int limit) {
        Long userId = UserContext.getUserId(request);
        return Result.ok(qaService.getHistory(userId, limit));
    }

    @GetMapping("/sessions")
    public Result<List<QaSessionVO>> getSessions(HttpServletRequest request,
                                                 @RequestParam(required = false) String stockCode,
                                                 @RequestParam(defaultValue = "20") int limit) {
        Long userId = UserContext.getUserId(request);
        return Result.ok(qaService.getSessions(userId, stockCode, limit));
    }

    @GetMapping("/sessions/{sessionId}")
    public Result<QaSessionDetailVO> getSessionDetail(HttpServletRequest request,
                                                      @PathVariable String sessionId,
                                                      @RequestParam(defaultValue = "100") int limit) {
        Long userId = UserContext.getUserId(request);
        return Result.ok(qaService.getSessionDetail(userId, sessionId, limit));
    }
}
