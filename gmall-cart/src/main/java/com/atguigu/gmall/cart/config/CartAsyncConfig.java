package com.atguigu.gmall.cart.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;

import java.util.concurrent.Executor;

/**
 * @Auther: cfy
 * @Date: 2020/09/06/16:05
 * @Description: TODO
 */
@Configuration
public class CartAsyncConfig implements AsyncConfigurer {

    @Autowired
    private CartAsyncExceptionHandler cartAsyncExceptionHandler;

    /**
     * springTask定义专有线程池
     *
     * @return
     */
    @Override
    public Executor getAsyncExecutor() {
        return null;
    }

    /**
     * 注册统一异常处理
     *
     * @return
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return cartAsyncExceptionHandler;
    }
}
