package com.smartstock.convert;

import com.smartstock.entity.UserWatchlist;
import com.smartstock.vo.WatchlistVO;
import org.springframework.stereotype.Component;

@Component
public class WatchlistStructMapper {

    public WatchlistVO toWatchlistVO(UserWatchlist watchlist, String stockName, String market) {
        if (watchlist == null) {
            return null;
        }
        WatchlistVO vo = new WatchlistVO();
        vo.setStockCode(watchlist.getStockCode());
        vo.setSortOrder(watchlist.getSortOrder());
        vo.setStockName(stockName);
        vo.setMarket(market);
        return vo;
    }
}
