package com.atguigu.gmall.pms.vo;

import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * @Auther: cfy
 * @Date: 2020/09/02/21:29
 * @Description: TODO
 */
@Data
public class SaleAttrValueVo {
    private Long attrId;
    private String attrName;
    private Set<String> attrValues;
}
