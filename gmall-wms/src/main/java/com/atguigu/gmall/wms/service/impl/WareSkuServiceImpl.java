package com.atguigu.gmall.wms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuMapper, WareSkuEntity> implements WareSkuService {

    @Autowired
    private WareSkuMapper wareSkuMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String KEY_PREFIX = "wms:lock:";

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<WareSkuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageResultVo(page);
    }

    @Transactional
    @Override
    public List<SkuLockVo> checkAndLock(List<SkuLockVo> lockVos, String orderToken) {
        // 判断商品是否为空
        if (CollectionUtils.isEmpty(lockVos)) {
            return null;
        }
        // 遍历所有商品，验库存并锁库存
        lockVos.forEach(lockVo -> {
            this.checkLock(lockVo);
        });

        // 判断又没有锁定失败的，有-所有成功的商品要释放库存。锁定失败的响应给order。 无-不需要显示锁定情况
        // 只要有一个锁定失败，进入if
        if (lockVos.stream().anyMatch(lockVo -> !lockVo.getLock())) {
            List<SkuLockVo> successLockVos = lockVos.stream().filter(SkuLockVo::getLock).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(successLockVos)) {
                // 解锁
                successLockVos.forEach(lockVo -> {
                    this.wareSkuMapper.unlockStock(lockVo.getSkuId(), lockVo.getCount());
                });
            }
            // 响应失败的商品列表
            return lockVos.stream().filter(skuLockVo -> !skuLockVo.getLock()).collect(Collectors.toList());
        }

        // 锁定成功信息，放入redis
        this.redisTemplate.opsForValue().set(KEY_PREFIX + orderToken, JSON.toJSONString(lockVos));

        // 发送消息
        this.rabbitTemplate.convertAndSend("ORDER-EXCHANGE", "stock.ttl", orderToken);

        // 锁定成功，返回null
        return null;
    }

    /**
     * 验库存并锁库存
     * 为了保证原子性，需要使用分布式锁
     */
    private void checkLock(SkuLockVo lockVo) {
        RLock fairLock = this.redissonClient.getFairLock("lock:" + lockVo.getSkuId());
        fairLock.lock();

        // 验库存，查询库存
        List<WareSkuEntity> wareSkuEntities = this.wareSkuMapper.checkStock(lockVo.getSkuId(), lockVo.getCount());
        if (CollectionUtils.isEmpty(wareSkuEntities)) {
            lockVo.setLock(false);
            fairLock.unlock();
            return;
        }
        // 锁库存。一般会根据运输距离，就近调配。这里就锁定第一个仓库的库存
        Long id = wareSkuEntities.get(0).getId();
        int lock = this.wareSkuMapper.lockStock(id, lockVo.getCount());
        if (lock == 1) {
            lockVo.setLock(true);
            lockVo.setWareSkuId(id); // 方便将来解锁对应仓库的信息，记录锁定库存记录wms_ware_sku的id
        } else {
            // 一般不会执行到这一步
            lockVo.setLock(false);
        }
        fairLock.unlock();
    }

}