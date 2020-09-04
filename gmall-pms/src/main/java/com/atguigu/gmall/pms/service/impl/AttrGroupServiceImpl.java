package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.mapper.SpuAttrValueMapper;
import com.atguigu.gmall.pms.vo.AttrValueVo;
import com.atguigu.gmall.pms.vo.GroupVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.AttrGroupMapper;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.service.AttrGroupService;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupMapper, AttrGroupEntity> implements AttrGroupService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<AttrGroupEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageResultVo(page);
    }

    @Autowired
    private AttrMapper attrMapper;

    @Autowired
    private SpuAttrValueMapper spuAttrValueMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Override
    public List<GroupVo> queryByCid(Long cid) {
        // 查询所有的分组
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("category_id", cid));

        // 查询出每组下的规格参数
        return attrGroupEntities.stream().map(attrGroupEntity -> {
            GroupVo groupVo = new GroupVo();
            BeanUtils.copyProperties(attrGroupEntity, groupVo);
            // 查询规格参数，只需查询出每个分组下的通用属性就可以了（不需要销售属性）
            List<AttrEntity> attrEntities = this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("group_id", attrGroupEntity.getId()).eq("type", 1));
            groupVo.setAttrEntities(attrEntities);
            return groupVo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ItemGroupVo> queryGroupWithAttrValue(Long cid, Long spuId, Long skuId) {
        // 遍历分类id查询所有组
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("category_id", cid));
        if (CollectionUtils.isEmpty(attrGroupEntities)) {
            return null;
        }
        List<ItemGroupVo> collect = attrGroupEntities.stream().map(attrGroupEntity -> {
            ItemGroupVo groupVo = new ItemGroupVo();
            // 遍历所有组，查询组下的规格参数
            groupVo.setGroupId(attrGroupEntity.getId());
            groupVo.setGroupName(attrGroupEntity.getName());

            List<AttrEntity> attrEntities = this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("group_id", attrGroupEntity.getId()));
            if (CollectionUtils.isEmpty(attrEntities)) {
                return groupVo;
            }
            List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());
            List<AttrValueVo> attrValueVos = new ArrayList<>();

            // 查询通用的规格参数及值
            List<SpuAttrValueEntity> spuAttrValueEntities = this.spuAttrValueMapper.selectList(new QueryWrapper<SpuAttrValueEntity>().in("attr_id", attrIds).eq("spu_id", spuId));
            if (!CollectionUtils.isEmpty(spuAttrValueEntities)) {
                List<AttrValueVo> attrValues = spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                    AttrValueVo attrValueVo = new AttrValueVo();
                    BeanUtils.copyProperties(spuAttrValueEntity, attrValueVo);
                    return attrValueVo;
                }).collect(Collectors.toList());
                attrValueVos.addAll(attrValues);
            }

            // 查询销售的规格参数及值
            List<SkuAttrValueEntity> skuAttrValueEntities = this.skuAttrValueMapper.selectList(new QueryWrapper<SkuAttrValueEntity>().in("attr_id", attrIds).eq("sku_id", skuId));
            if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                List<AttrValueVo> attrValueSales = skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                    AttrValueVo attrValueVo = new AttrValueVo();
                    BeanUtils.copyProperties(skuAttrValueEntity, attrValueVo);
                    return attrValueVo;
                }).collect(Collectors.toList());
                attrValueVos.addAll(attrValueSales);
            }
            groupVo.setAttrValues(attrValueVos);
            return groupVo;
        }).collect(Collectors.toList());

        return collect;
    }

}