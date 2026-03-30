package com.smartstock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartstock.entity.StockInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockInfoMapper extends BaseMapper<StockInfo> {
}
