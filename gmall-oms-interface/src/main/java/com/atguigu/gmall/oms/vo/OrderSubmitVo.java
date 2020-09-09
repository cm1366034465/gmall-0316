package com.atguigu.gmall.oms.vo;

import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Auther: cfy
 * @Date: 2020/09/08/8:35
 * @Description: TODO
 */
@Data
public class OrderSubmitVo {
    private String orderToken; // 防重
    private BigDecimal totalPrice; // 总价，校验价格变化
    private UserAddressEntity address; // 收货人信息
    private Integer payType; // 支付方式
    private String deliveryCompany; // 配送方式
    private List<OrderItemVo> items; // 订单详情信息
    private Integer bounds; // 积分信息

    /**
     * 20200908 15:15 增加邮费字段
     */
    private BigDecimal postFee;

    // 发票信息TODO

    // 营销信息TODO
}
