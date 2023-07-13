package com.hmdp.utils.redis;

import cn.hutool.bloomfilter.bitMap.LongMap;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @description: 全局唯一ID生成器
 * @author: yf-idjke
 * @create: 2023-05-17 14:18
 **/

@Component
public class RedisIDWorker {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 开始的时间戳
     */
    private static final long BEGIN_TIMESTAMP = 1684334069L;

    private static final int COUNT_BITS = 32;

    /**
     *
     * @param keyPrefix 前缀
     * @return 生成一个商品唯一Id
     */
    public long nextId(String keyPrefix){
        //1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowEpochSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp = nowEpochSecond - BEGIN_TIMESTAMP;
        //2.生成序列号
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        Long serId = stringRedisTemplate.opsForValue().increment("incr:" + keyPrefix +":"+ date);
        //3.拼接并返回
        return timeStamp<<COUNT_BITS | serId;
    }


}
