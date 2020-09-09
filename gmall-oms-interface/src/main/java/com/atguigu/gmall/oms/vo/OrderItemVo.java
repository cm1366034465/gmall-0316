package com.atguigu.gmall.oms.vo;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Auther: cfy
 * @Date: 2020/09/07/21:05
 * @Description: TODO
 */
@Data
public class OrderItemVo {

    private Long skuId;
    private String title;
    private String defaultImage;
    private BigDecimal price;
    private BigDecimal count;
    private BigDecimal weight;
    private List<SkuAttrValueEntity> saleAttrs; // 销售属性
    private List<ItemSaleVo> sales; // 营销信息
    private Boolean store = false; // 库存信息
}
