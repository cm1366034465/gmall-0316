package com.atguigu.gmall.oms.mapper;

import com.atguigu.gmall.oms.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author cfy
 * @email cfy@qq.com
 * @date 2020-08-21 16:50:34
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {
	
}
