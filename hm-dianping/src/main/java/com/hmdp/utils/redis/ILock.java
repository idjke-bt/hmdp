package com.hmdp.utils.redis;

public interface ILock {
    /**
     * 基于set key value nx ex ttl 实现获取分布式锁
     * @param timeoutSec 设置过期时间，到期自动释放锁
     * @return true表示获取锁成功,false表示获取锁失败
     */
    boolean tryLock(Long timeoutSec);

    /**
     * 释放锁
     */
    void unlock();
}
