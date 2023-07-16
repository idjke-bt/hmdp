package com.hmdp;


import com.hmdp.utils.redis.RedisConstants;
import com.hmdp.utils.redis.RedisIDWorker;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class HmDianPingApplicationTests {

    @Autowired
    private RedissonClient redissonClient;

    @Resource
    private RedisIDWorker idWorker;

    @Test
    void testIdWorker(){
        Long id = idWorker.nextId(RedisConstants.CACHE_SHOP_KEY);
        System.out.println(id);
    }

    @Test
    void testRedisson() throws InterruptedException {
        RLock lock = redissonClient.getLock("testRock");
        boolean flag = lock.tryLock(20, 200,TimeUnit.SECONDS);
        if(flag){
            try {
                System.out.println("i have a lock");
                Thread.sleep(10000);
            }finally {
                lock.unlock();
            }
        }
    }
    @Test
    void testCopilot(){
        //写一个单例模式

    }

}
