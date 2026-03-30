package com.smartstock.service;

import com.smartstock.vo.NewsFlashVO;
import com.smartstock.vo.PageVO;

import java.util.List;

public interface NewsService {

    List<NewsFlashVO> getFlashNews(int limit);

    List<NewsFlashVO> getFlashNews(int limit, String source, String stockCode, String keyword);

    PageVO<NewsFlashVO> getFlashNewsPage(int page, int pageSize);

    PageVO<NewsFlashVO> getFlashNewsPage(int page, int pageSize, String source, String stockCode, String keyword);
}
