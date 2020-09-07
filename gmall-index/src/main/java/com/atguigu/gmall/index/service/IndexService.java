package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.aspect.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.utils.DistributedLock;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Auther: cfy
 * @Date: 2020/09/01/16:05
 * @Description: TODO
 */
@Service
public class IndexService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    private static final String KEY_PREFIX = "index:category:";

    private static final String KEY_LV = "index:category:lv1:";

    public List<CategoryEntity> querylvl1Categories() {
        String json = this.redisTemplate.opsForValue().get(KEY_LV);
        if (StringUtils.isNotBlank(json)) {
            return JSON.parseArray(json, CategoryEntity.class);
        }
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesByPid(0L);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();
        // if (!CollectionUtils.isEmpty(categoryEntities)) 不用判断，防止缓存穿透
        // 30 + new Random().nextInt(10) 防止缓存雪崩
        // 加分布式锁解决缓存击穿问题
        this.redisTemplate.opsForValue().set(KEY_LV, JSON.toJSONString(categoryEntities), 30 + new Random().nextInt(10), TimeUnit.DAYS);
        return categoryEntities;
    }

    @GmallCache(prefix = KEY_PREFIX, lock = "lock", timeout = 43200, random = 10080)
    public List<CategoryEntity> queryCategoriesWithSubByPid(Long pid) {
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesWithSubByPid(pid);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();
        return categoryEntities;
    }

    /**
     * 旧方式
     *
     * @param pid
     * @return
     */
    public List<CategoryEntity> queryCategoriesWithSubByPid2(Long pid) {
        // 1.先查询缓存
        // key的设计 一级分类的id作为key，添加前缀key_prefix
        String json = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNotBlank(json)) {
            return JSON.parseArray(json, CategoryEntity.class);
        }
        // 20200902 19:22 增加分布式锁
        // 为了防止缓存的击穿，需要添加分布式锁
        RLock lock = this.redissonClient.getFairLock("lock:" + pid);
        lock.lock();
        // 再查询缓存，缓存中有，直接返回
        String json2 = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNotBlank(json2)) {
            lock.unlock();
            return JSON.parseArray(json2, CategoryEntity.class);
        }

        // 2.在远程查询数据库，并放入缓存
        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesWithSubByPid(pid);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();

        // 为了解决缓存穿透，数据即使为null也要缓存，为了防止缓存雪崩，给缓存时间添加随机值
        this.redisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntities), 30 + new Random().nextInt(10), TimeUnit.DAYS);

        // 释放分布式锁 或者再finally中完成释放锁
        lock.unlock();

        return categoryEntities;
    }

    @Autowired
    private DistributedLock distributedLock;

    // 测试可重入性
    private void testSubLock(String uuid) {
        // 加锁
        Boolean lock = this.distributedLock.tryLock("lock", uuid, 30l);

        if (lock) {
            System.out.println("分布式可重入锁...");
            this.distributedLock.unlock("lock", uuid);
        }
    }

    public void testLock() {

        RLock lock = this.redissonClient.getLock("lock");
        lock.lock(10, TimeUnit.SECONDS);
        // 查询redis中的num值
        String value = this.redisTemplate.opsForValue().get("num");
        // 没有该值return
        if (StringUtils.isBlank(value)) {
            return;
        }
        // 有值就转成成int
        int num = Integer.parseInt(value);
        // 把redis中的num值+1
        this.redisTemplate.opsForValue().set("num", String.valueOf(++num));
        // lock.unlock();
    }

    public void testLock1() {
        // 加锁
        String uuid = UUID.randomUUID().toString();
        Boolean lock = this.distributedLock.tryLock("lock", uuid, 30l);

        if (lock) {
            // 读取redis中的num值
            String numString = this.redisTemplate.opsForValue().get("num");
            if (StringUtils.isBlank(numString)) {
                return;
            }

            // ++操作
            Integer num = Integer.parseInt(numString);
            num++;

            // 放入redis
            this.redisTemplate.opsForValue().set("num", String.valueOf(num));

            // 睡眠60s，锁过期时间30s。每隔20s自动续期
            try {
                TimeUnit.SECONDS.sleep(60);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 测试可重入性
            // this.testSubLock(uuid);

            // 释放锁
            this.distributedLock.unlock("lock", uuid);
        }
    }

    public String readLock() {
        // 初始化读写锁
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("readwriteLock");
        RLock rLock = readWriteLock.readLock(); // 获取读锁

        rLock.lock(10, TimeUnit.SECONDS); // 加10s锁

        String msg = this.redisTemplate.opsForValue().get("msg");

        //rLock.unlock(); // 解锁
        return msg;
    }

    public String writeLock() {
        // 初始化读写锁
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("readwriteLock");
        RLock rLock = readWriteLock.writeLock(); // 获取写锁

        rLock.lock(10, TimeUnit.SECONDS); // 加10s锁

        this.redisTemplate.opsForValue().set("msg", UUID.randomUUID().toString());

        //rLock.unlock(); // 解锁
        return "成功写入了内容。。。。。。";
    }

    public String latch() {
        RCountDownLatch countDownLatch = this.redissonClient.getCountDownLatch("countdown");
        try {
            countDownLatch.trySetCount(6);
            countDownLatch.await();

            return "关门了。。。。。";
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String countDown() {
        RCountDownLatch countDownLatch = this.redissonClient.getCountDownLatch("countdown");

        countDownLatch.countDown();
        return "出来了一个人。。。";
    }

}
