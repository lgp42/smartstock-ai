package com.smartstock.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartstock.entity.TradeOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TradeOrderMapper extends BaseMapper<TradeOrder> {
}
