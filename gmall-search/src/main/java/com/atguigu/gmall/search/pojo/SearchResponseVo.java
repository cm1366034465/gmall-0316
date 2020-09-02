package com.atguigu.gmall.search.pojo;

import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import lombok.Data;

import java.util.List;

/**
 * @Auther: cfy
 * @Date: 2020/08/31/0:18
 * @Description: TODO
 */
@Data
public class SearchResponseVo {
    private List<BrandEntity> brands; // 品牌
    private List<CategoryEntity> categories; // 分类
    private List<SearchResponseAttrVo> filters; // 属性过滤 规格参数

    // 分页参数
    private Long total;
    private Integer pageNum;
    private Integer pageSize;
    private List<Goods> goodsList;
}
