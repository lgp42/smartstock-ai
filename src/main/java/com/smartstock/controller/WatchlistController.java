package com.smartstock.controller;

import com.smartstock.common.Result;
import com.smartstock.dto.WatchlistAddDTO;
import com.smartstock.service.WatchlistService;
import com.smartstock.util.UserContext;
import com.smartstock.vo.WatchlistVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
@Tag(name = "Watchlist", description = "自选股管理接口")
public class WatchlistController {

    private final WatchlistService watchlistService;

    @GetMapping
    public Result<List<WatchlistVO>> getWatchlist(HttpServletRequest request) {
        Long userId = UserContext.getUserId(request);
        return Result.ok(watchlistService.getWatchlist(userId));
    }

    @PostMapping
    public Result<Void> addToWatchlist(HttpServletRequest request,
                                       @Valid @RequestBody WatchlistAddDTO dto) {
        Long userId = UserContext.getUserId(request);
        watchlistService.addToWatchlist(userId, dto);
        return Result.ok();
    }

    @DeleteMapping("/{stockCode}")
    public Result<Void> removeFromWatchlist(HttpServletRequest request,
                                            @PathVariable String stockCode) {
        Long userId = UserContext.getUserId(request);
        watchlistService.removeFromWatchlist(userId, stockCode);
        return Result.ok();
    }
}
