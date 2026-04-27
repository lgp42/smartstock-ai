package com.smartstock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartstock.client.EastMoneyClient;
import com.smartstock.client.StockRealtimeDTO;
import com.smartstock.common.BusinessException;
import com.smartstock.common.ErrorCode;
import com.smartstock.convert.WatchlistStructMapper;
import com.smartstock.dto.WatchlistAddDTO;
import com.smartstock.dto.WatchlistBatchDTO;
import com.smartstock.dto.WatchlistSortDTO;
import com.smartstock.entity.StockInfo;
import com.smartstock.entity.UserWatchlist;
import com.smartstock.mapper.StockInfoMapper;
import com.smartstock.mapper.UserWatchlistMapper;
import com.smartstock.service.WatchlistService;
import com.smartstock.vo.WatchlistVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatchlistServiceImpl implements WatchlistService {

    private final UserWatchlistMapper watchlistMapper;
    private final StockInfoMapper stockInfoMapper;
    private final EastMoneyClient eastMoneyClient;
    private final WatchlistStructMapper watchlistStructMapper;

    private static final int MAX_WATCHLIST_SIZE = 50;

    @Override
    public List<WatchlistVO> getWatchlist(Long userId) {
        List<UserWatchlist> watchlist = watchlistMapper.selectList(
                new LambdaQueryWrapper<UserWatchlist>()
                        .eq(UserWatchlist::getUserId, userId)
                        .orderByAsc(UserWatchlist::getSortOrder)
                        .orderByAsc(UserWatchlist::getCreatedAt));

        List<WatchlistVO> result = new ArrayList<>();
        for (UserWatchlist item : watchlist) {
            String stockCode = item.getStockCode();
            StockInfo stockInfo = findStockInfo(stockCode);

            String stockName = stockInfo != null ? stockInfo.getStockName() : null;
            String market = stockInfo != null ? stockInfo.getMarket() : null;
            WatchlistVO vo = watchlistStructMapper.toWatchlistVO(item, stockName, market);
            result.add(vo);
        }

        result.parallelStream().forEach(vo -> {
            if (StringUtils.hasText(vo.getMarket())) {
                try {
                    String marketCode = StockInfo.toEastMoneyMarketCode(vo.getMarket());
                    StockRealtimeDTO realtimeDTO = eastMoneyClient.getRealtimeQuote(vo.getStockCode(), marketCode);
                    if (realtimeDTO != null) {
                        vo.setCurrentPrice(realtimeDTO.getCurrentPrice());
                        vo.setChangeRate(realtimeDTO.getChangeRate());
                    }
                } catch (Exception e) {
                    log.warn("Failed to get realtime quote for watchlist stock: {}", vo.getStockCode(), e);
                }
            }
        });
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addToWatchlist(Long userId, WatchlistAddDTO dto) {
        String stockCode = dto.getStockCode();

        // 检查股票是否存在
        StockInfo stockInfo = findStockInfo(stockCode);
        if (stockInfo == null) {
            throw new BusinessException(ErrorCode.STOCK_NOT_FOUND, "股票不存在: " + stockCode);
        }

        // 检查自选股数量是否已达上限
        Long count = watchlistMapper.selectCount(
                new LambdaQueryWrapper<UserWatchlist>().eq(UserWatchlist::getUserId, userId));
        if (count >= MAX_WATCHLIST_SIZE) {
            throw new BusinessException(ErrorCode.WATCHLIST_FULL, "自选股已达上限（" + MAX_WATCHLIST_SIZE + "只）");
        }

        // 检查是否已添加
        Long existCount = watchlistMapper.selectCount(
                new LambdaQueryWrapper<UserWatchlist>()
                        .eq(UserWatchlist::getUserId, userId)
                        .eq(UserWatchlist::getStockCode, stockCode));
        if (existCount > 0) {
            throw new BusinessException(ErrorCode.WATCHLIST_ALREADY_EXISTS, "该股票已在自选股列表中");
        }

        // 插入记录
        UserWatchlist watchlistItem = UserWatchlist.builder()
                .userId(userId)
                .stockCode(stockCode)
                .sortOrder(count.intValue())
                .build();
        watchlistMapper.insert(watchlistItem);
        log.info("Added stock to watchlist: userId={}, stockCode={}", userId, stockCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addBatch(Long userId, WatchlistBatchDTO dto) {
        for (String stockCode : dto.getStockCodes()) {
            WatchlistAddDTO addDTO = new WatchlistAddDTO();
            addDTO.setStockCode(stockCode);
            try {
                addToWatchlist(userId, addDTO);
            } catch (BusinessException e) {
                if (e.getCode() != ErrorCode.WATCHLIST_ALREADY_EXISTS) {
                    throw e;
                }
            }
        }
    }

    @Override
    public void removeFromWatchlist(Long userId, String stockCode) {
        watchlistMapper.delete(
                new LambdaQueryWrapper<UserWatchlist>()
                        .eq(UserWatchlist::getUserId, userId)
                        .eq(UserWatchlist::getStockCode, stockCode));
        log.info("Removed stock from watchlist: userId={}, stockCode={}", userId, stockCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeBatch(Long userId, WatchlistBatchDTO dto) {
        for (String stockCode : dto.getStockCodes()) {
            removeFromWatchlist(userId, stockCode);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSort(Long userId, WatchlistSortDTO dto) {
        for (WatchlistSortDTO.Item item : dto.getItems()) {
            UserWatchlist watchlist = watchlistMapper.selectOne(
                    new LambdaQueryWrapper<UserWatchlist>()
                            .eq(UserWatchlist::getUserId, userId)
                            .eq(UserWatchlist::getStockCode, item.getStockCode()));
            if (watchlist != null) {
                watchlist.setSortOrder(item.getSortOrder());
                watchlistMapper.updateById(watchlist);
            }
        }
    }

    private StockInfo findStockInfo(String stockCode) {
        List<StockInfo> matches = stockInfoMapper.selectList(
                new LambdaQueryWrapper<StockInfo>().eq(StockInfo::getStockCode, stockCode));
        StockInfo preferred = selectPreferredStoredStockInfo(stockCode, matches);
        if (preferred != null) {
            return preferred;
        }

        for (java.util.Map<String, String> remote : eastMoneyClient.searchStocks(stockCode)) {
            if (!stockCode.equals(remote.get("stockCode"))
                    || !StockInfo.isTradableAStock(stockCode, remote.get("market"))) {
                continue;
            }
            return upsertStockInfo(remote);
        }
        return null;
    }

    private StockInfo selectPreferredStoredStockInfo(String stockCode, List<StockInfo> matches) {
        if (matches == null || matches.isEmpty()) {
            return null;
        }
        String preferredMarket = stockCode != null && stockCode.startsWith("6") ? "SH" : "SZ";
        for (StockInfo match : matches) {
            if (preferredMarket.equalsIgnoreCase(match.getMarket())
                    && StockInfo.isTradableAStock(match.getStockCode(), match.getMarket())) {
                return match;
            }
        }
        for (StockInfo match : matches) {
            if (StockInfo.isTradableAStock(match.getStockCode(), match.getMarket())) {
                return match;
            }
        }
        for (StockInfo match : matches) {
            if (preferredMarket.equalsIgnoreCase(match.getMarket())) {
                return match;
            }
        }
        return null;
    }

    private StockInfo upsertStockInfo(java.util.Map<String, String> remote) {
        String stockCode = remote.get("stockCode");
        String market = remote.get("market");
        if (!StringUtils.hasText(stockCode) || !StringUtils.hasText(market)) {
            return null;
        }
        StockInfo existing = stockInfoMapper.selectOne(new LambdaQueryWrapper<StockInfo>()
                .eq(StockInfo::getStockCode, stockCode));
        if (existing != null) {
            existing.setMarket(market);
            if (StringUtils.hasText(remote.get("stockName"))) {
                existing.setStockName(remote.get("stockName"));
            }
            if (StringUtils.hasText(remote.get("industry"))) {
                existing.setIndustry(remote.get("industry"));
            }
            existing.setBoard(StockInfo.resolveBoard(stockCode, market));
            existing.setIsSt(StockInfo.isStStock(remote.get("stockName")) ? 1 : 0);
            existing.setIsDelisted(StockInfo.isDelistedStock(remote.get("stockName")) ? 1 : 0);
            existing.setSource("WATCHLIST");
            existing.setStatus(existing.getIsDelisted() != null && existing.getIsDelisted() == 1 ? 0 : 1);
            stockInfoMapper.updateById(existing);
            return existing;
        }

        StockInfo created = StockInfo.builder()
                .stockCode(stockCode)
                .stockName(remote.get("stockName"))
                .market(market)
                .board(StockInfo.resolveBoard(stockCode, market))
                .industry(remote.get("industry"))
                .isSt(StockInfo.isStStock(remote.get("stockName")) ? 1 : 0)
                .isDelisted(StockInfo.isDelistedStock(remote.get("stockName")) ? 1 : 0)
                .source("WATCHLIST")
                .status(1)
                .build();
        stockInfoMapper.insert(created);
        return created;
    }
}
