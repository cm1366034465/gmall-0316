package com.atguigu.gmall.cart.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.bean.Cart;
import com.atguigu.gmall.cart.bean.UserInfo;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Auther: cfy
 * @Date: 2020/09/06/13:50
 * @Description: TODO
 */
@Service
public class CartService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CartAsyncService cartAsyncService;

    private static final String KEY_PREFIX = "cart:info:";

    private static final String PRICE_PREFIX = "cart:price:";

    public void addCart(Cart cart) {
        // 获取登录信息
        String userId = null;
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        if (userInfo.getUserId() != null) {
            userId = userInfo.getUserId().toString();
        } else {
            userId = userInfo.getUserKey();
        }
        String key = KEY_PREFIX + userId;

        // 获取内层map的操作对象
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);

        // 判断购物车是否有商品，有-更新 无-新增
        String skuId = cart.getSkuId().toString();
        BigDecimal count = cart.getCount();
        if (hashOps.hasKey(skuId)) {
            // 有-更新数量
            String cartJson = hashOps.get(skuId).toString();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(cart.getCount().add(count));

            // 重新放入mysql
            this.cartAsyncService.updateCartByUserIdAndSkuId(userId, cart);
        } else {
            // 无-新增一条记录
            cart.setUserId(userId);
            cart.setCheck(true);

            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity != null) {
                cart.setTitle(skuEntity.getTitle());
                cart.setPrice(skuEntity.getPrice());
                cart.setDefaultImage(skuEntity.getDefaultImage());
            }

            ResponseVo<List<SkuAttrValueEntity>> skuResponseVo = this.pmsClient.querySaleAttrValueBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = skuResponseVo.getData();
            if (skuAttrValueEntities != null) {
                cart.setSaleAttrs(JSON.toJSONString(skuAttrValueEntities));
            }

            ResponseVo<List<ItemSaleVo>> itemResponseVo = this.smsClient.querySalesBySkuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVos = itemResponseVo.getData();
            if (itemSaleVos != null) {
                cart.setSales(JSON.toJSONString(itemSaleVos));
            }

            ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryWareSkuBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }

            // 缓存实时价格
            this.redisTemplate.opsForValue().set(PRICE_PREFIX + skuId, skuEntity.getPrice().toString());

            // 新增mysql数据库
            this.cartAsyncService.insertCart(userId, cart);
        }
        // 通用-重新放入redis
        hashOps.put(skuId, JSON.toJSONString(cart));
    }

    public Cart queryCartBuySkuId(Long skuId) {
        String key = KEY_PREFIX;
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        if (userInfo.getUserId() != null) {
            key += userInfo.getUserId();
        } else {
            key += userInfo.getUserKey();
        }
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        if (skuId != null && hashOps.hasKey(skuId.toString())) {
            String cartJson = hashOps.get(skuId.toString()).toString();
            return JSON.parseObject(cartJson, Cart.class);
        } else {
            throw new RuntimeException("不存在购物车信息");
        }
    }

    /**
     * 查询购物车
     *
     * @return
     */
    public List<Cart> queryCarts() {
        // 查询未登录的购物车
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userKey = userInfo.getUserKey();
        String unLoginKey = KEY_PREFIX + userKey;
        BoundHashOperations<String, Object, Object> unLoginHashOps = this.redisTemplate.boundHashOps(unLoginKey);

        // 未登录的购物车集合
        List<Cart> unLoginCarts = null;
        List<Object> unLoginCartJsons = unLoginHashOps.values();
        if (!CollectionUtils.isEmpty(unLoginCartJsons)) {
            unLoginCarts = unLoginCartJsons.stream().map(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                String currentPrice = this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId());
                cart.setCurrentPrice(new BigDecimal(currentPrice));
                return cart;
            }).collect(Collectors.toList());
        }

        // 判断登录状态
        Long userId = userInfo.getUserId();
        if (userId == null) {
            // 未登录-返回未登录的购物车
            return unLoginCarts;
        }

        // 登录-合并未登录的购物车到登录状态的购物车
        String loginKey = KEY_PREFIX + userId;
        BoundHashOperations<String, Object, Object> loginHashOps = this.redisTemplate.boundHashOps(loginKey);
        if (!CollectionUtils.isEmpty(unLoginCarts)) {
            unLoginCarts.forEach(cart -> {
                BigDecimal count = cart.getCount(); // 未登录购物车的数量
                if (loginHashOps.hasKey(cart.getSkuId().toString())) {
                    String cartJson = loginHashOps.get(cart.getSkuId().toString()).toString();
                    cart = JSON.parseObject(cartJson, Cart.class); // 登录状况下购物车
                    cart.setCount(cart.getCount().add(count));
                    // 更新mysql中的数量
                    this.cartAsyncService.updateCartByUserIdAndSkuId(userId.toString(), cart);
                } else {
                    cart.setUserId(userId.toString());
                    this.cartAsyncService.insertCart(userId.toString(), cart);
                }
                // 更新redis
                loginHashOps.put(cart.getSkuId().toString(), JSON.toJSONString(cart));
            });
        }

        // 删除未登录的购物车
        this.cartAsyncService.deleteCartsByUserId(userKey);
        this.redisTemplate.delete(unLoginKey);

        // 查询登录状态的购物车
        List<Object> loginCartJsons = loginHashOps.values();

        if (!CollectionUtils.isEmpty(loginCartJsons)) {
            return loginCartJsons.stream().map(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                String currentPrice = this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId());
                cart.setCurrentPrice(new BigDecimal(currentPrice));
                return cart;
            }).collect(Collectors.toList());
        }

        return null;
    }

    public void updateNum(Cart cart) {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userId = null;
        if (userInfo.getUserId() != null) {
            userId = userInfo.getUserId().toString();
        } else {
            userId = userInfo.getUserKey();
        }

        String key = KEY_PREFIX + userId;

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        BigDecimal count = cart.getCount();
        if (hashOps.hasKey(cart.getSkuId().toString())) {
            String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(count);
            this.cartAsyncService.updateCartByUserIdAndSkuId(userId, cart);
            hashOps.put(cart.getSkuId().toString(), JSON.toJSONString(cart));
        }
    }

    public void deleteCart(Long skuId) {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userId = null;
        if (userInfo.getUserId() != null) {
            userId = userInfo.getUserId().toString();
        } else {
            userId = userInfo.getUserKey();
        }

        String key = KEY_PREFIX + userId;

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);

        if (hashOps.hasKey(skuId.toString())) {
            this.cartAsyncService.deleteCartByUserAndSkuId(userId, skuId);
            hashOps.delete(skuId.toString());
        }
    }

    @Async
    public ListenableFuture<String> executor1() {
        try {
            System.out.println("executor1方法开始执行");
            TimeUnit.SECONDS.sleep(4);
            System.out.println("executor1方法结束执行。。。");
            return AsyncResult.forValue("executor1"); // 正常响应
        } catch (InterruptedException e) {
            e.printStackTrace();
            return AsyncResult.forExecutionException(e); // 异常响应
        }
    }

    @Async
    public String executor2() {
        try {
            System.out.println("executor2方法开始执行");
            TimeUnit.SECONDS.sleep(5);
            System.out.println("executor2方法结束执行。。。");
            int i = 1 / 0; // 制造异常
            return "executor2"; // 正常响应
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void scheduledDeleteByUserId(String userId) {
        if (StringUtils.isNotBlank(userId)) {
            this.cartAsyncService.scheduledDeleteByUserId(userId);
        }
    }

    public void scheduledInsertCart(Cart cart) {
        if (cart != null) {
            this.cartAsyncService.scheduledInsertCart(cart);
        }
    }

    public List<Cart> queryCheckedCartByUserId(Long userId) {
        String key = KEY_PREFIX + userId;
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        List<Object> cartJsons = hashOps.values();
        if (CollectionUtils.isEmpty(cartJsons)) {
            return null;
        }
        return cartJsons.stream().map(cartJson -> JSON.parseObject(cartJson.toString(), Cart.class)).filter(cart -> cart.getCheck()).collect(Collectors.toList());
    }
}
