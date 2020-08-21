package com.atguigu.gmall.pms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.pms.entity.SkuEntity;

import java.util.Map;

/**
 * sku信息
 *
 * @author cfy
 * @email cfy@qq.com
 * @date 2020-08-21 16:53:28
 */
public interface SkuService extends IService<SkuEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

