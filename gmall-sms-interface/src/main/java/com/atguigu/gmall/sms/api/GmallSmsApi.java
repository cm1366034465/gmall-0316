package com.atguigu.gmall.sms.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @Auther: cfy
 * @Date: 2020/08/24/23:53
 * @Description: TODO
 */
public interface GmallSmsApi {
    @PostMapping("/sms/skubounds/skusale/save")
    public ResponseVo<Object> saveSkuSaleInfo(@RequestBody SkuSaleVo skuSaleVO);

    @GetMapping("sms/skubounds/sku/{skuId}")
    public ResponseVo<List<ItemSaleVo>> querySalesBySkuId(@PathVariable("skuId") Long skuId);
}
