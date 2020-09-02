package com.atguigu.gmall.search.vo;

import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * @Auther: cfy
 * @Date: 2020/08/28/12:57
 * @Description: TODO
 */
@Data
public class SearchAttrValueVo {
    @Field(type = FieldType.Long)
    private Long attrId;
    @Field(type = FieldType.Keyword)
    private String attrName;
    @Field(type = FieldType.Keyword)
    private String attrValue;
}
