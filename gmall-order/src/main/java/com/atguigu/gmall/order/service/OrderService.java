package com.atguigu.gmall.order.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.bean.Cart;
import com.atguigu.gmall.cart.bean.UserInfo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Auther: cfy
 * @Date: 2020/09/07/22:31
 * @Description: TODO
 */
@Service
public class OrderService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private GmallCartClient cartClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallOmsClient omsClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "order:token:";

    // @Autowired
    // private ThreadPoolExecutor threadPoolExecutor;

    /**
     * 订单确认页
     * 由于存在大量的远程调用，这里使用异步编排做优化
     *
     * @return
     */
    public OrderConfirmVo confirm() {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();

        // 收货地址列表
        ResponseVo<List<UserAddressEntity>> addressResponseVo = this.umsClient.queryAddressByUserId(userId);
        List<UserAddressEntity> addresses = addressResponseVo.getData();
        confirmVo.setAddresses(addresses);

        // 从购物车中查询用户选中的购物车记录
        ResponseVo<List<Cart>> cartResponseVo = this.cartClient.queryCheckedCartByUserId(userId);
        List<Cart> carts = cartResponseVo.getData();
        if (CollectionUtils.isEmpty(carts)) {
            throw new RuntimeException("没有选中的购物车记录，请先选择要购买的商品！");
        }

        List<OrderItemVo> items = carts.stream().map(cart -> {
            OrderItemVo orderItemVo = new OrderItemVo();

            // skuId 和 count 可以直接设置
            orderItemVo.setSkuId(cart.getSkuId());
            orderItemVo.setCount(cart.getCount());

            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            orderItemVo.setTitle(skuEntity.getTitle());
            orderItemVo.setPrice(skuEntity.getPrice());
            orderItemVo.setDefaultImage(skuEntity.getDefaultImage());
            orderItemVo.setWeight(new BigDecimal(skuEntity.getWeight()));

            ResponseVo<List<WareSkuEntity>> wareSkuBySkuId = this.wmsClient.queryWareSkuBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = wareSkuBySkuId.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                orderItemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }

            ResponseVo<List<ItemSaleVo>> salesResponseVo = this.smsClient.querySalesBySkuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVos = salesResponseVo.getData();
            orderItemVo.setSales(itemSaleVos);

            ResponseVo<List<SkuAttrValueEntity>> skuAttrValueResponseVo = this.pmsClient.querySaleAttrValueBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueResponseVo.getData();
            orderItemVo.setSaleAttrs(skuAttrValueEntities);

            return orderItemVo;
        }).collect(Collectors.toList());

        confirmVo.setItems(items);

        // 积分
        ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUserById(userId);
        UserEntity userEntity = userEntityResponseVo.getData();
        confirmVo.setBounds(userEntity.getIntegration());

        // 防重，生成唯一标识，响应给页面，保存到redis中一份
        String orderToken = IdWorker.getTimeId();
        confirmVo.setOrderToken(orderToken);
        this.redisTemplate.opsForValue().set(KEY_PREFIX + orderToken, orderToken, 3, TimeUnit.HOURS);

        return confirmVo;
    }

    public OrderEntity submit(OrderSubmitVo submitVo) {
        // 1.防重 vo中的orderToken 查redis 有-放行，无-拦截  查和删要具备原子性
        // keys prefix+orderToken  argv orderToken
        String orderToken = submitVo.getOrderToken();
        if (StringUtils.isBlank(orderToken)) {
            throw new RuntimeException("没有orderToken");
        }
        String scripts = "if redis.call('get', KEYS[1]) == ARGV[1] \" +\n" +
                "        \"then return redis.call('del', KEYS[1]) \" +\n" +
                "        \"else return 0 end";
        Boolean flag = this.redisTemplate.execute(new DefaultRedisScript<>(scripts, Boolean.class), Arrays.asList(KEY_PREFIX + orderToken), orderToken);

        if (!flag) {
            throw new OrderException("请不要重复提交");
        }

        // 2.验价
        BigDecimal totalPrice = submitVo.getTotalPrice();
        List<OrderItemVo> items = submitVo.getItems();
        if (CollectionUtils.isEmpty(items)) {
            throw new OrderException("选择要购买的商品！");
        }

        BigDecimal currentTotalPrice = items.stream().map(item -> {
            // 根据skuId查询数据库中的实时单价
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(item.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null) {
                return new BigDecimal(0);
            }
            return skuEntity.getPrice().multiply(item.getCount());
        }).reduce((a, b) -> a.add(b)).get();

        if (totalPrice.compareTo(currentTotalPrice) != 0) {
            throw new OrderException("页面已过期,请刷新后再试!");
        }

        // 3.验库存并且锁库存
        List<SkuLockVo> lockVos = items.stream().map(item -> {
            SkuLockVo lockVo = new SkuLockVo();
            lockVo.setSkuId(item.getSkuId());
            lockVo.setSkuId(item.getCount().longValue());
            return lockVo;
        }).collect(Collectors.toList());
        ResponseVo<List<SkuLockVo>> skuLockResponseVo = this.wmsClient.checkAndLock(lockVos, orderToken);
        List<SkuLockVo> skuLockVos = skuLockResponseVo.getData();
        if (!CollectionUtils.isEmpty(skuLockVos)) {
            // 非空情况 转换成json
            throw new OrderException(JSON.toJSONString(skuLockVos));
        }

        // 4.下单
        Long userId = null;
        OrderEntity orderEntity = null;
        try {
            UserInfo userInfo = LoginInterceptor.getUserInfo();
            userId = (Long) userInfo.getUserId();
            ResponseVo<OrderEntity> orderEntityResponseVo = this.omsClient.saveOrder(submitVo, userId);
            orderEntity = orderEntityResponseVo.getData();
        } catch (Exception e) {
            e.printStackTrace();
            // MQ最终一致性 发送消息 追求性能
            // 发送消息解锁库存
            // 1、创建订单失败，本地事务解决
            // 2、创建订单成功，响应超时
            // 3、order发送oms远程请求超时
            // 1/3 只要解锁库存即可
            // 2、发送wms和oms 解锁库存 和 把订单标记未无效订单
            // 发送消息给库存和oms，解锁库存，订单标记未无效订单
            this.rabbitTemplate.convertAndSend("ORDER-EXCHANGE", "order.fail", orderToken);
        }

        // 5.发送消息给购物车删除对应购物信息
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", userId);
            List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
            map.put("skuIds", JSON.toJSONString(skuIds));
            this.rabbitTemplate.convertAndSend("ORDER-EXCHANGE", "cart.delete", map);
        } catch (AmqpException e) {
            e.printStackTrace();
        }

        return orderEntity;
    }
}
