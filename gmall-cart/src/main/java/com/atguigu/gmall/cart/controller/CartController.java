package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.bean.Cart;
import com.atguigu.gmall.cart.bean.UserInfo;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.SuccessCallback;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @Auther: cfy
 * @Date: 2020/09/05/15:03
 * @Description: TODO
 */
@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * 添加购物车成功，重定向到购物车成功界面
     *
     * @param cart
     * @return
     */
    @GetMapping
    public String addCart(Cart cart) {
        if (cart == null || cart.getSkuId() == null) {
            throw new RuntimeException("没有选择添加到购物车的商品信息!");
        }
        this.cartService.addCart(cart);
        return "redirect:http://cart.gmall.com/addCart?skuId=" + cart.getSkuId();
    }

    /**
     * 跳转到成功页面
     *
     * @return
     */
    @GetMapping("addCart")
    public String queryCartBuySkuId(@RequestParam("skuId") Long skuId, Model model) {
        Cart cart = this.cartService.queryCartBuySkuId(skuId);
        model.addAttribute("cart", cart);
        return "addCart";
    }

    @GetMapping("cart.html")
    public String queryCarts(Model model) {
        List<Cart> carts = this.cartService.queryCarts();
        model.addAttribute("carts", carts);
        return "cart";
    }

    @PostMapping("updateNum")
    @ResponseBody
    public ResponseVo<Object> updateNum(@RequestBody Cart cart) {
        this.cartService.updateNum(cart);
        return ResponseVo.ok();
    }

    @PostMapping("deleteCart")
    @ResponseBody
    public ResponseVo<Object> deleteCart(@RequestParam("skuId") Long skuId) {
        this.cartService.deleteCart(skuId);
        return ResponseVo.ok();
    }

    /**
     * 被定时任务调用，清除userId对应的记录
     */
    @PostMapping("scheduled/delete/{userId}")
    @ResponseBody
    public ResponseVo<Object> scheduledDeleteByUserId(@PathVariable("userId") String userId) {
        this.cartService.scheduledDeleteByUserId(userId);
        return ResponseVo.ok();
    }

    @PostMapping("scheduled/insert")
    @ResponseBody
    public ResponseVo<Object> scheduledInsertCart(@RequestBody Cart cart) {
        this.cartService.scheduledInsertCart(cart);
        return ResponseVo.ok();
    }

    @GetMapping("test")
    @ResponseBody
    public String test() throws ExecutionException, InterruptedException {
        long now = System.currentTimeMillis();
        System.out.println("controller.test方法开始执行！");
        this.cartService.executor2();
        System.out.println("controller.test方法结束执行！！！" + (System.currentTimeMillis() - now));

        return "hello cart!";
    }
}
