package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.aspect.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.utils.DistributedLock;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RLock;
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
        RLock lock = this.redissonClient.getFairLock("lock:"+pid);
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

    // 测试可重入锁
    public void testLock() {
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

            // 测试可重入性
            this.testSubLock(uuid);

            // 释放锁
            this.distributedLock.unlock("lock", uuid);
        }
    }

    // synchronized 本地锁在集群状况下也无法解决问题
    public void testLock2() {
        String uuid = UUID.randomUUID().toString();
        Boolean lock = this.redisTemplate.opsForValue().setIfAbsent("lock", uuid, 10, TimeUnit.SECONDS);
        if (!lock) {
            // 获取失败则重试
            try {
                Thread.sleep(50);
                testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            // 获取成功执行业务

            // 查询redis中的num值
            String value = this.redisTemplate.opsForValue().get("num");
            // 没有该值 设置后return
            if (StringUtils.isBlank(value)) {
                this.redisTemplate.opsForValue().set("num", "0");
                return;
            }
            // 有值就转成成int
            int num = Integer.parseInt(value);
            // 把redis中的num值+1
            this.redisTemplate.opsForValue().set("num", String.valueOf(++num));

            // 执行完毕释放锁
            // 防止误删，需要判断是不是自己的锁  但判断和删除操作没有原子性
            // if get lock 是否等于 uuid then  return del lock   else return 0
/*            if (StringUtils.equals(uuid, this.redisTemplate.opsForValue().get("lock"))) {
                this.redisTemplate.delete("lock");
            }*/
            // lua 脚本
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList("lock"), uuid);
        }
    }

    public void testLockyx() {
        // 1. 从redis中获取锁,setnx
        String uuid = UUID.randomUUID().toString();
        Boolean lock = this.redisTemplate.opsForValue().setIfAbsent("lock", uuid, 30, TimeUnit.SECONDS);
        if (lock) {
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

            // 2. 释放锁 del
/*            if (StringUtils.equals(uuid, this.redisTemplate.opsForValue().get("lock"))) {
                this.redisTemplate.delete("lock");
            }*/
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            this.redisTemplate.execute(new DefaultRedisScript<>(script), Arrays.asList("lock"), uuid);
        } else {
            // 3. 每隔1秒钟回调一次，再次尝试获取锁
            try {
                Thread.sleep(1000);
                testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
