package com.hmdp;


import com.hmdp.utils.redis.RedisConstants;
import com.hmdp.utils.redis.RedisIDWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    private RedisIDWorker idWorker;

    @Test
    void testIdWorker(){
        Long id = idWorker.nextId(RedisConstants.CACHE_SHOP_KEY);
        System.out.println(id);
    }

}
