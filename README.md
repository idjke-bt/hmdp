# hmdp

## v0.1.0 
使用session解决用户登录问题

## v0.2.0
使用redis解决shop的缓存问题：
  1. 缓存空值解决缓存穿透
  2. 互斥锁和逻辑过期结果缓存击穿

## v0.2.1
使用自定义redis工具类CacheClient解决缓存问题

## v0.2.2
实现一人一单功能，同时保证不同用户抢票时可以并发进行(涉及synchronized中锁的选取,spring事务失效的处理)

## v0.2.3
1. 当有多台服务器时，之前一人一单的功能可能失效，故使用redis实现自定义分布式锁替代之前的方案；
2. 使用`Lua脚本`保证释放锁的`判断`操作和`释放`操作的原子性

## v0.2.4
使用redisson实现分布式锁