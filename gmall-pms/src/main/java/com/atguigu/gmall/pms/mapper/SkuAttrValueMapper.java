package com.atguigu.gmall.pms.mapper;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * sku销售属性&值
 * 
 * @author cfy
 * @email cfy@qq.com
 * @date 2020-08-21 16:53:28
 */
@Mapper
public interface SkuAttrValueMapper extends BaseMapper<SkuAttrValueEntity> {

    List<SkuAttrValueEntity> querySearchAttrValuesBySkuId(Long skuId);

    List<Map<String, Object>> querySaleAttrMappingSkuIdBySpuId(Long spuId);
}
