package com.hmdp.utils.redis;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @description: redis分布式锁1
 * @author: yf-idjke
 * @create: 2023-06-15 21:51
 **/

public class SimpleRedisLock implements ILock{
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 提前将lua脚本初始化
     */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    /**
     * 不同业务的锁是不一样的，在key中加入name作为区分
     */
    private String name;
    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID() +"-";

    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    @Override
    public boolean tryLock(Long timeoutSec) {
        String key  = KEY_PREFIX+name;
        String value = ID_PREFIX + Thread.currentThread().getId();
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, value, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(flag);
    }

    @Override
    public void unlock() {
        //调用lua脚本
        stringRedisTemplate.execute(UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX+name),
                ID_PREFIX+Thread.currentThread().getId());

//        String key = KEY_PREFIX+name;
//        //获取redis中的锁标识
//        String value = stringRedisTemplate.opsForValue().get(key);
//        //获取线程中的标识
//        String threadValue = ID_PREFIX + Thread.currentThread().getId();

//        if(threadValue.equals(value))
//            stringRedisTemplate.delete(KEY_PREFIX+name);


    }
}
