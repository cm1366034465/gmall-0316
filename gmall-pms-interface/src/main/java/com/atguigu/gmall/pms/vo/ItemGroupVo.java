package com.atguigu.gmall.pms.vo;

import lombok.Data;

import java.util.List;

/**
 * @Auther: cfy
 * @Date: 2020/09/03/9:22
 * @Description: TODO
 */
@Data
public class ItemGroupVo {
    private Long groupId;
    private String groupName;
    private List<AttrValueVo> attrValues;
}
