package com.hmdp.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;

/**
 * @description: redis工具类
 * @author: yf-idjke
 * @create: 2023-05-16 14:37
 **/

@Slf4j
@Component
public class CacheClient {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 创建线程池
     */
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(
            10);

    /**
     * 向Redis中存入数据
     * @param key  存入数据的key
     * @param value 要存入的数据
     * @param time 过期时间
     * @param unit 过期时间的单位
     */
    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 使用逻辑过期向redis中存如数据
     * @param key  存入数据的key
     * @param value 要存入的数据
     * @param time 过期时间
     * @param unit 过期时间的单位
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit){
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));

        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 查询数据，解决了缓存穿透的问题
     * @param keyPrefix key前缀
     * @param id 所查询数据的id
     * @param returnType R的运行时类
     * @return 要查询的数据
     * @param <R> 查询的数据的类型
     * @param <ID> id的类型
     */
    public <R, ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> returnType, Function<ID,R> dbQueryFun,Long time, TimeUnit unit){
        String key = keyPrefix + id;
        //1.根据id从redis缓存中查询商铺信息
        String jsonStr = stringRedisTemplate.opsForValue().get(key);
        //2.若存在，则返回
        if(StrUtil.isNotBlank(jsonStr)){
            //从字符串中反序列出对象
            return JSONUtil.toBean(jsonStr, returnType);
        }
        //缓存中有空字符串防止缓存穿透
        if("".equals(jsonStr)){
            return null;
        }

        //3.redis中不存在，根据id查询数据库
        R qs = dbQueryFun.apply(id);
        if (qs==null){
            //将空值写入redis，防止缓存穿透
            stringRedisTemplate.opsForValue().set(key,"", CACHE_NULL_TTL,TimeUnit.MINUTES);
            return null;
        }
        //3.2 数据库中存在该数据，存入redis
        this.set(key,qs,time,unit);

        return qs;
    }

    /**
     * 查询数据，利用互斥锁解决了缓存击穿的问题
     * @param keyPrefix key前缀
     * @param id 所查询数据的id
     * @param returnType R的运行时类
     * @return 要查询的数据
     * @param <R> 查询的数据的类型
     * @param <ID> id的类型
     */
    public  <R,ID> R queryWithMutex(String keyPrefix, ID id, Class<R> returnType, Function<ID,R> dbQueryFun,Long time, TimeUnit unit) {
        String key = keyPrefix+ id;
        //1.根据id从redis缓存中查询商铺信息
        String jsonKey = stringRedisTemplate.opsForValue().get(key);
        //2.若存在，则返回
        if(StrUtil.isNotBlank(jsonKey)){
            //从字符串中反序列出对象
            return JSONUtil.toBean(jsonKey,returnType);
        }
        //缓存中有空字符串防止缓存穿透
        if("".equals(jsonKey)){
            return null;
        }
        /* 3.实现缓存重建 */
        //3.1 获取互斥锁，判断是否获取成功
        String lockKey = LOCK_SHOP_KEY+id;
        Boolean isLocked = tryLock(lockKey);
        R qr;
        try {
            //3.2 若失败，则休眠并重试
            if(!isLocked){
                Thread.sleep(50);
                return queryWithMutex(keyPrefix,id,returnType,dbQueryFun,time,unit);
            }
            //3.2 若成功，则---->4.根据id查询数据库，并存入redis
            //此时应该再次查看是否存在缓存，避免当前拿到的锁是刚被其他线程释放的
            jsonKey = stringRedisTemplate.opsForValue().get(key);
            //若存在，则返回
            if(StrUtil.isNotBlank(jsonKey)){
                //从字符串中反序列出对象
                return JSONUtil.toBean(jsonKey, returnType);
            }
            //根据id查询数据库
            qr = dbQueryFun.apply(id);
            //模拟重建延时
//            Thread.sleep(200);

            //4.1 数据库中商铺信息不存在，返回404
            if(qr==null){
                //将空值写入redis，防止缓存穿透
                stringRedisTemplate.opsForValue().set(key,"", CACHE_NULL_TTL,TimeUnit.MINUTES);
                return null;
            }
            //4.2 数据库中存在该数据，存入redis
            this.set(key,qr,time,unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //3.3 释放互斥锁
            unLock(lockKey);
        }
        return qr;
    }

    /**
     * 查询数据，利用逻辑过期解决了缓存击穿的问题
     * @param keyPrefix key前缀
     * @param id 所查询数据的id
     * @param returnType R的运行时类
     * @return 要查询的数据
     * @param <R> 查询的数据的类型
     * @param <ID> id的类型
     */
    public  <R,ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> returnType, Function<ID,R> dbQueryFun,Long time, TimeUnit unit) {
        String key = keyPrefix+ id;
        //1.根据id从redis缓存中查询商铺信息
        String jsonStr = stringRedisTemplate.opsForValue().get(key);
        //2.若不存在，则返回
        if(StrUtil.isBlank(jsonStr))
            return null;

        //3.将json反序列化为对象
        RedisData redisData = JSONUtil.toBean(jsonStr, RedisData.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        R qr = JSONUtil.toBean((JSONObject) redisData.getData(),returnType);
        //4.判断是否过期：未过期，直接返回
        if(expireTime.isAfter(LocalDateTime.now()))
            return qr;
        //5.已过期：缓存重建
        //5.1 获取互斥锁
        String lockKey = LOCK_SHOP_KEY+id;
        if (tryLock(lockKey)) {
            //5.3 获取成功，开启独立线程
            CACHE_REBUILD_EXECUTOR.submit(
                    ()-> {
                        try {
                            //5.4 根据id查询数据库，重建redis，设置过期时间
                            R qr_new = dbQueryFun.apply(id);
                            this.setWithLogicalExpire(key,qr_new,time,unit);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        } finally {
                            //释放锁
                            unLock(lockKey);
                        }
                    }
            );
        }
        //5.5 返回店铺信息  && 5.2 获取失败，返回过期的店铺信息
        return qr;
    }

    //获取互斥锁
    private Boolean tryLock(String key){
        return stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
    }

    //释放锁
    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }


}
