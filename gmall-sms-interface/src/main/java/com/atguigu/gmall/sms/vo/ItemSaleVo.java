package com.atguigu.gmall.sms.vo;

import lombok.Data;

/**
 * @Auther: cfy
 * @Date: 2020/09/02/21:27
 * @Description: TODO
 */
@Data
public class ItemSaleVo {
    private String type; // 积分 满减 打折
    private String desc; // 描述信息
}
