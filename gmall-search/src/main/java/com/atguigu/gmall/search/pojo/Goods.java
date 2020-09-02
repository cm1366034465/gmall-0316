package com.atguigu.gmall.search.pojo;

import com.atguigu.gmall.search.vo.SearchAttrValueVo;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;
import java.util.List;

/**
 * @Auther: cfy
 * @Date: 2020/08/28/12:50
 * @Description: TODO
 */
@Data
@Document(indexName = "goods", type = "info", shards = 3, replicas = 2)
public class Goods {
    // 商品列表 搜索列表字段
    @Id
    @Field(type = FieldType.Long)
    private Long skuId;
    @Field(type = FieldType.Keyword, index = false)
    private String defaultImage;
    @Field(type = FieldType.Double)
    private Double price;
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String title;
    @Field(type = FieldType.Keyword, index = false)
    private String subTitle;

    // 排序和筛选字段
    @Field(type = FieldType.Long)
    private Long sales; //销量
    @Field(type = FieldType.Date)
    private Date createTime; // 新品
    @Field(type = FieldType.Boolean)
    private Boolean store; // 是否有货 20200831 16:10 去掉默认false设置

    // 聚合字段
    // 品牌过滤
    @Field(type = FieldType.Long)
    private Long brandId;
    @Field(type = FieldType.Keyword)
    private String brandName;
    @Field(type = FieldType.Keyword)
    private String logo;

    // 分类过滤
    // 20200831 14:51 原先配置为keyword类型,分类id改为Long类型
    @Field(type = FieldType.Long)
    private Long categoryId;
    @Field(type = FieldType.Keyword)
    private String categoryName;

    @Field(type = FieldType.Nested)
    private List<SearchAttrValueVo> searchAttrs;
}
