package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import lombok.Data;

import java.util.List;

/**
 * @Auther: cfy
 * @Date: 2020/08/24/20:55
 * @Description: TODO
 */
@Data
public class GroupVo extends AttrGroupEntity {
    private List<AttrEntity> attrEntities;
}
