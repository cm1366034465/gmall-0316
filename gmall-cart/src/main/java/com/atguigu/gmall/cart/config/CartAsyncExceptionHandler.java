package com.atguigu.gmall.cart.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @Auther: cfy
 * @Date: 2020/09/06/16:03
 * @Description: TODO
 */
@Slf4j
@Component
public class CartAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY = "cart:async:exception";

    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... objects) {
        log.error("有一个子任务出现异常。异常信息：{},异常方法：{}，方法参数：{}", throwable.getMessage(), method, objects);

        // 异常用户信息存入redis
        String userId = objects[0].toString();
        if (StringUtils.isNotBlank(userId)) {
            BoundListOperations<String, String> listOps = this.redisTemplate.boundListOps(KEY);
            listOps.leftPush(userId);
        } else {
            throw new RuntimeException("用户id为空!");
        }
    }
}
