package com.atguigu.gmall.search.pojo;

import lombok.Data;

import java.util.List;

/**
 * @Auther: cfy
 * @Date: 2020/08/31/0:17
 * @Description: TODO
 */
@Data
public class SearchResponseAttrVo {
    private Long attrId;
    private String attrName;
    private List<String> attrValues;
}
