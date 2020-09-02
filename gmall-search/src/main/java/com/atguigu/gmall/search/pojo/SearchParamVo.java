package com.atguigu.gmall.search.pojo;

import lombok.Data;

import java.util.List;

/**
 * @Auther: cfy
 * @Date: 2020/08/29/11:39
 * @Description: TODO
 */
@Data
public class SearchParamVo {
    // 关键字
    private String keyword;
    // 品牌id列表
    private List<Long> brandId;
    // 分类列表 255手机
    private List<Long> categoryId;
    // 规格参数过滤
    private List<String> props;
    // 排序字段 0得分排序 1价格升序 2价格降序 3新品降序 4销量降序
    private Integer sort;
    // 是否有货
    private Boolean store;

    // 价格区间过滤
    private Double priceFrom;
    private Double priceTo;

    // 分页 有默认值
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
