package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.cart.bean.Cart;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @Auther: cfy
 * @Date: 2020/09/06/16:26
 * @Description: TODO
 */
@Service
public class CartAsyncService {
    @Autowired
    private CartMapper cartMapper;

    @Async
    public void updateCartByUserIdAndSkuId(String userId, Cart cart) {
        this.cartMapper.update(cart, new UpdateWrapper<Cart>().eq("user_id", userId).eq("sku_id", cart.getSkuId()));
    }

    @Async
    public void insertCart(String userId, Cart cart) {
        // 20200907 保存购物车到数据库异常问题 手动制造异常
        // int i = 1 / 0;
        this.cartMapper.insert(cart);
    }

    @Async
    public void deleteCartsByUserId(String userId) {
        this.cartMapper.delete(new QueryWrapper<Cart>().eq("user_id", userId));
    }

    @Async
    public void deleteCartByUserAndSkuId(String userId, Long skuId) {
        this.cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id", userId).eq("sku_id", skuId));
    }

    @Async
    public void scheduledDeleteByUserId(String userId) {
        this.cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id", userId));
    }

    @Async
    public void scheduledInsertCart(Cart cart) {
        this.cartMapper.insert(cart);
    }
}
