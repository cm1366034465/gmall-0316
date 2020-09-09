package com.atguigu.gmall.cart.api;

import com.atguigu.gmall.cart.bean.Cart;
import com.atguigu.gmall.common.bean.ResponseVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @Auther: cfy
 * @Date: 2020/09/07/13:58
 * @Description: TODO
 */
public interface GmallCartApi {
    @PostMapping("scheduled/delete/{userId}")
    public ResponseVo<Object> scheduledDeleteByUserId(@PathVariable("userId") String userId);

    @PostMapping("scheduled/insert")
    public ResponseVo<Object> scheduledInsertCart(@RequestBody Cart cart);

    @GetMapping("user/{userId}")
    public ResponseVo<List<Cart>> queryCheckedCartByUserId(@PathVariable("userId") Long userId);
}
