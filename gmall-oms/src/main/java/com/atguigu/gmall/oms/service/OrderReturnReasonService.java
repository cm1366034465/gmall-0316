package com.atguigu.gmall.oms.service;

import com.atguigu.gmall.oms.entity.OrderReturnReasonEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

/**
 * 退货原因
 *
 * @author cfy
 * @email cfy@qq.com
 * @date 2020-08-21 16:50:34
 */
public interface OrderReturnReasonService extends IService<OrderReturnReasonEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

