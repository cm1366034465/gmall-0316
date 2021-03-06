package com.atguigu.gmall.oms.service;

import com.atguigu.gmall.oms.entity.RefundInfoEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

/**
 * 退款信息
 *
 * @author cfy
 * @email cfy@qq.com
 * @date 2020-08-21 16:50:34
 */
public interface RefundInfoService extends IService<RefundInfoEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

