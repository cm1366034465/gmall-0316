package com.atguigu.gmall.pms.controller;

import java.util.List;

import com.atguigu.gmall.pms.vo.GroupVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.service.AttrGroupService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.PageParamVo;

/**
 * 属性分组
 *
 * @author cfy
 * @email cfy@qq.com
 * @date 2020-08-21 16:53:28
 */
@Api(tags = "属性分组 管理")
@RestController
@RequestMapping("pms/attrgroup")
public class AttrGroupController {

    @Autowired
    private AttrGroupService attrGroupService;

    @GetMapping("withattrs/withvalue/{cid}")
    public ResponseVo<List<ItemGroupVo>> queryGroupWithAttrValue(@PathVariable("cid") Long cid,
                                                                 @RequestParam("spuId") Long spuId,
                                                                 @RequestParam("skuId") Long skuId) {
        List<ItemGroupVo> groupVos = this.attrGroupService.queryGroupWithAttrValue(cid, spuId, skuId);
        return ResponseVo.ok(groupVos);
    }

    /**
     * 根据三级分类id查询规格组
     */
    @GetMapping("category/{cid}")
    public ResponseVo<List<AttrGroupEntity>> queryGroupsByCid(@PathVariable("cid") Long cid) {
        QueryWrapper<AttrGroupEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category_id", cid);
        List<AttrGroupEntity> list = this.attrGroupService.list(queryWrapper);
        return ResponseVo.ok(list);
    }

    /**
     * 列表
     */
    @GetMapping
    @ApiOperation("分页查询")
    public ResponseVo<PageResultVo> queryAttrGroupByPage(PageParamVo paramVo) {
        PageResultVo pageResultVo = attrGroupService.queryPage(paramVo);

        return ResponseVo.ok(pageResultVo);
    }


    /**
     * 信息
     */
    @GetMapping("{id}")
    @ApiOperation("详情查询")
    public ResponseVo<AttrGroupEntity> queryAttrGroupById(@PathVariable("id") Long id) {
        AttrGroupEntity attrGroup = attrGroupService.getById(id);

        return ResponseVo.ok(attrGroup);
    }

    /**
     * 保存
     */
    @PostMapping
    @ApiOperation("保存")
    public ResponseVo<Object> save(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.save(attrGroup);

        return ResponseVo.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @ApiOperation("修改")
    public ResponseVo update(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.updateById(attrGroup);

        return ResponseVo.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ApiOperation("删除")
    public ResponseVo delete(@RequestBody List<Long> ids) {
        attrGroupService.removeByIds(ids);

        return ResponseVo.ok();
    }

    @ApiOperation("根据三级分类id查询分组及组下的规格参数")
    @GetMapping("/withattrs/{catId}")
    public ResponseVo<List<GroupVo>> queryByCid(@PathVariable("catId") Long cid) {

        List<GroupVo> groupVos = this.attrGroupService.queryByCid(cid);
        return ResponseVo.ok(groupVos);
    }
}
