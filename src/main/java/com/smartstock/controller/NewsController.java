package com.smartstock.controller;

import com.smartstock.common.BusinessException;
import com.smartstock.common.ErrorCode;
import com.smartstock.common.Result;
import com.smartstock.service.NewsService;
import com.smartstock.vo.NewsFlashVO;
import com.smartstock.vo.PageVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Tag(name = "News", description = "市场资讯与快讯接口")
public class NewsController {

    private final NewsService newsService;

    @GetMapping("/flash")
    public Result<List<NewsFlashVO>> getFlashNews(@RequestParam(defaultValue = "10") int limit,
                                                  @RequestParam(required = false) String source,
                                                  @RequestParam(required = false) String stockCode,
                                                  @RequestParam(required = false) String keyword) {
        validateLimit(limit);
        return Result.ok(newsService.getFlashNews(limit, source, stockCode, keyword));
    }

    @GetMapping("/flash/page")
    public Result<PageVO<NewsFlashVO>> getFlashNewsPage(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "20") int pageSize,
                                                        @RequestParam(required = false) String source,
                                                        @RequestParam(required = false) String stockCode,
                                                        @RequestParam(required = false) String keyword) {
        validatePage(page, pageSize);
        return Result.ok(newsService.getFlashNewsPage(page, pageSize, source, stockCode, keyword));
    }

    private void validateLimit(int limit) {
        if (limit < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "limit 最小为 1");
        }
        if (limit > 300) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "limit 最大为 300");
        }
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "page 最小为 1");
        }
        if (pageSize < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "pageSize 最小为 1");
        }
        if (pageSize > 100) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "pageSize 最大为 100");
        }
    }
}
