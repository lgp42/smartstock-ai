package com.smartstock.service;

import com.smartstock.dto.WatchlistAddDTO;
import com.smartstock.dto.WatchlistBatchDTO;
import com.smartstock.dto.WatchlistSortDTO;
import com.smartstock.vo.WatchlistVO;

import java.util.List;

public interface WatchlistService {

    List<WatchlistVO> getWatchlist(Long userId);

    void addToWatchlist(Long userId, WatchlistAddDTO dto);

    void addBatch(Long userId, WatchlistBatchDTO dto);

    void removeFromWatchlist(Long userId, String stockCode);

    void removeBatch(Long userId, WatchlistBatchDTO dto);

    void updateSort(Long userId, WatchlistSortDTO dto);
}
