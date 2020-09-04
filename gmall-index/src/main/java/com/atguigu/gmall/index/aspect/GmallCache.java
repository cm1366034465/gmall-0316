package com.atguigu.gmall.index.aspect;

import java.lang.annotation.*;

/**
 * @Auther: cfy
 * @Date: 2020/09/02/19:48
 * @Description: TODO
 */

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GmallCache {

    /**
     * 缓存key前缀
     * @return
     */
    String prefix() default "";

    /**
     * 缓存的过期时间
     * 单位：分钟
     */
    int timeout() default 5;

    /**
     * 防止缓存雪崩，指定随机值范围
     * 单位：分钟
     */
    int random() default 5 ;

    /**
     * 防止缓存击穿，添加分布式锁
     * 通过该属性可以指定分布式锁的名称
     * @return
     */
    String lock() default "lock";

}
