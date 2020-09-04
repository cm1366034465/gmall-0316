package com.atguigu.gmall.index.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * @Auther: cfy
 * @Date: 2020/09/02/16:14
 * @Description: TODO
 */
@Component
public class DistributedLock {

    @Autowired
    private StringRedisTemplate redisTemplate;

    public Boolean tryLock(String lockName, String uuid, Long expire) {
        String script = "if (redis.call('exists',KEYS[1])==0 or redis.call('hexists',KEYS[1],ARGV[1])==1) then redis.call('hincrby',KEYS[1],ARGV[1],1) redis.call('expire',KEYS[1],ARGV[2]) return 1 else return 0 end";
        Boolean flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lockName), uuid, expire.toString());
        if (!flag) {
            try {
                // 没有获取到锁，重试
                Thread.sleep(100);
                tryLock(lockName, uuid, expire);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public void unlock(String lockName, String uuid) {
        String script = "if redis.call('hexists',KEYS[1],ARGV[1])==0 then return nil end if(redis.call('hincrby',KEYS[1],ARGV[1],-1)>0) then return 0 else redis.call('del',KEYS[1]) return 1 end";
        Boolean flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lockName), uuid);
        if (flag == null) {
            throw new RuntimeException("您正常尝试解锁别人的锁！ lockName = " + lockName + ", uuid = " + uuid);
        }
    }

    /**
     * 自动续期实现 20200904 09:20
     */

    public void renewTime(String lockName, Long expire) {
        String script = "";
        new Thread(() -> {
            while (this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lockName), expire.toString())) {
                try {
                    Thread.sleep(expire*1000/3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "").start();
    }
}
