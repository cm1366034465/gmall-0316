package com.atguigu.gmall.oms.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import org.springframework.web.bind.annotation.*;

/**
 * @Auther: cfy
 * @Date: 2020/09/08/13:52
 * @Description: TODO
 */
public interface GmallOmsApi {
    @PostMapping("oms/order/submit/{userId}")
    public ResponseVo<OrderEntity> saveOrder(@RequestBody OrderSubmitVo submitVo, @PathVariable("userId") Long userId);

    @GetMapping("oms/order/token/{orderToken}")
    public ResponseVo<OrderEntity> queryOrderByToken(@PathVariable("orderToken") String orderToken, @RequestParam("userId") Long userId);
}
