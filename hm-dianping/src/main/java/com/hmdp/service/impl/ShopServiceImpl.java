package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
* @Description: ShopServiceImpl
* @Author: yf-idjke
* @Date: 2023/5/15
*/


@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    /**
     * 创建线程池
     */
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(
            10);

    /**
     * 利用存储空值解决缓存穿透, 互斥锁or逻辑过期解决缓存穿透
     * @param id shop id
     */
    @Override
    public Result queryById(Long id) {
        Shop shop = queryWithMutex(id);
//        Shop shop = queryWithLogicalExpire(id);

        //使用Redis工具类测试缓存穿透
//        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        //使用Redis工具类测试互斥锁缓存击穿
//        Shop shop = cacheClient.queryWithMutex(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        //使用Redis工具类测试逻辑过期缓存击穿
//        Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, 5L, TimeUnit.SECONDS);

        if(shop==null)
            return Result.fail("店铺不存在！");
        return Result.ok(shop);
    }

    /**
     * 使用互斥锁解决缓存击穿的问题
     * @param id shop id
     * @return shop
     */
    private Shop queryWithMutex(Long id) {
        String shopKey = CACHE_SHOP_KEY+ id;
        //1.根据id从redis缓存中查询商铺信息
        String shopStr = stringRedisTemplate.opsForValue().get(shopKey);
        //2.若存在，则返回
        if(StrUtil.isNotBlank(shopStr)){
            //从字符串中反序列出对象
            return JSONUtil.toBean(shopStr, Shop.class);
        }
        //缓存中有空字符串防止缓存穿透
        if("".equals(shopStr)){
            return null;
        }
        /* 3.实现缓存重建 */
        //3.1 获取互斥锁，判断是否获取成功
        String lockKey = LOCK_SHOP_KEY+id;
        Boolean isLocked = tryLock(lockKey);
        Shop shop;
        try {
            //3.2 若失败，则休眠并重试
            if(!isLocked){
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            //3.2 若成功，则---->4.根据id查询数据库，并存入redis
            //此时应该再次查看是否存在缓存，避免当前拿到的锁是刚被其他线程释放的
            shopStr = stringRedisTemplate.opsForValue().get(shopKey);
            //若存在，则返回
            if(StrUtil.isNotBlank(shopStr)){
                //从字符串中反序列出对象
                return JSONUtil.toBean(shopStr, Shop.class);
            }
            //根据id查询数据库
            shop = getById(id);
            //模拟重建延时
//            Thread.sleep(200);

            //4.1 数据库中商铺信息不存在，返回404
            if(shop==null){
                //将空值写入redis，防止缓存穿透
                stringRedisTemplate.opsForValue().set(shopKey,"", CACHE_NULL_TTL,TimeUnit.MINUTES);
                return null;
            }
            //4.2 数据库中存在该数据，存入redis
            stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //3.3 释放互斥锁
            unLock(lockKey);
        }
        return shop;
    }

    /**
     * 使用逻辑过期决缓存击穿的问题
     * 这里处理的都是热点商铺，即事先就已经存储在redis中，只需要在特定时间更新，而不会删除
     * @param id shop id
     * @return shop
     */
    private Shop queryWithLogicalExpire(Long id) {
        String redisShopKey = CACHE_SHOP_KEY+ id;
        //1.根据id从redis缓存中查询商铺信息
        String redisShopStr = stringRedisTemplate.opsForValue().get(redisShopKey);
        //2.若不存在，则返回
        if(StrUtil.isBlank(redisShopStr))
            return null;

        //3.将json反序列化为对象
        RedisData redisData = JSONUtil.toBean(redisShopStr, RedisData.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
        //4.判断是否过期：未过期，直接返回
        if(expireTime.isAfter(LocalDateTime.now()))
            return shop;
        //5.已过期：缓存重建
        //5.1 获取互斥锁
        String lockKey = LOCK_SHOP_KEY+id;
        if (tryLock(lockKey)) {
            //5.3 获取成功，开启独立线程
            CACHE_REBUILD_EXECUTOR.submit(
                    ()-> {
                        try {
                            //5.4 根据id查询数据库，重建redis，设置过期时间
                            this.saveShopToRedis(id, 20L);
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
        return shop;
    }


    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(id==null)
            return Result.fail("店铺id为空，不允许更新！");

        //1.更新数据库
        updateById(shop);

        //2.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY+id);
        return Result.ok();
    }

    //获取互斥锁
    private Boolean tryLock(String key){
        return stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
    }

    //释放锁
    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }

    /**
     * 将热点数据提前添加到缓存中
     * @param id 店铺id
     * @param expireSeconds 逻辑过期时间
     */
    public void saveShopToRedis(Long id, Long expireSeconds) {
        //1.查询店铺数据
        Shop shop = getById(id);
        //2.封装逻辑过期时间expireTime
        RedisData redisShop = new RedisData();
        redisShop.setData(shop);
        redisShop.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        //3.写入Redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id, JSONUtil.toJsonStr(redisShop));
    }
}
