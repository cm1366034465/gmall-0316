package com.atguigu.gmall.index.aspect;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import sun.nio.cs.SingleByte;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @Auther: cfy
 * @Date: 2020/09/02/19:58
 * @Description: TODO
 */
@Aspect
@Component
public class GmallCacheAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Around("@annotation(com.atguigu.gmall.index.aspect.GmallCache)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 获取目标方法
        Method method = signature.getMethod();
        // 获取目标方法的注解
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);
        // 获取注解中的前缀
        String prefix = gmallCache.prefix();
        // 获取目标方法的形参
        Object[] args = joinPoint.getArgs();
        // 获取目标方法返回值类型
        Class returnType = signature.getReturnType();
        // prefix + args 组成缓存key
        String key = prefix + Arrays.asList(args);

        // 1.查询缓存，缓存中有，直接反序列化返回
        String json = this.redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(json)) {
            return JSON.parseObject(json, returnType);
        }

        // 2.加分布式锁，防止缓存击穿
        String lock = gmallCache.lock();
        RLock fairLock = this.redissonClient.getFairLock(lock + Arrays.asList(args));// 不影响其他参数
        fairLock.lock();

        // 3.在查缓存，有直接返回
        String json2 = this.redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(json2)) {
            fairLock.unlock();
            return JSON.parseObject(json2, returnType);
        }

        // 4.再去执行目标方法
        Object result = joinPoint.proceed(joinPoint.getArgs());

        // 5.把目标方法的返回值放入缓存
        int timeout = gmallCache.timeout();
        int random = gmallCache.random();
        this.redisTemplate.opsForValue().set(key, JSON.toJSONString(result), timeout + new Random().nextInt(random), TimeUnit.MINUTES);

        // 6.释放锁
        fairLock.unlock();

        return result;
    }
}
