package com.smartstock.service;

import com.smartstock.dto.WatchlistAddDTO;
import com.smartstock.vo.WatchlistVO;

import java.util.List;

public interface WatchlistService {

    List<WatchlistVO> getWatchlist(Long userId);

    void addToWatchlist(Long userId, WatchlistAddDTO dto);

    void removeFromWatchlist(Long userId, String stockCode);
}
