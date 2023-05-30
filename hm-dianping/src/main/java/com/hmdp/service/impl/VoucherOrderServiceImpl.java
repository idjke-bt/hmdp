package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.UserHolder;
import com.hmdp.utils.redis.RedisConstants;
import com.hmdp.utils.redis.RedisIDWorker;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 处理秒杀券订单表
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Transactional
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIDWorker redisIDWorker;

    @Override
    public Result seckillVoucher(Long voucherId) {
        //1.查询优惠券 :
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);   //对象的值不会随数据库的变化而变化
        //2.判断秒杀是否开始
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始！");
        }
        //3.判断秒杀是否结束
        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已经结束！");
        }
        //4.判断库存(stock)是否充足
        if (seckillVoucher.getStock()<1) {
            return Result.fail("秒杀券库存不足！");
        }
        Long userId = UserHolder.getUser().getId();
        synchronized (userId.toString().intern()){
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }
    }

    @Transactional
    public  Result createVoucherOrder(Long voucherId){
        //7.实现一人一单
        //7.1 查询当前线程的用户id
        Long id = UserHolder.getUser().getId();
        //7.2 查询该用户是否已经拥有过秒杀券
        Integer count = query().eq("user_id", id).eq("voucher_id", voucherId).count();
        if (count>0){
            return Result.fail("您已抢购过该优惠券！");
        }

        //5.扣减库存
        boolean flag = seckillVoucherService.update().
                setSql("stock = stock -1").
                eq("voucher_id", voucherId).gt("stock",0).update();

        if(!flag){
            return Result.fail("秒杀券库存不足！");
        }

        //6.创建订单,将订单写入数据库
        VoucherOrder voucherOrder = new VoucherOrder();
        //6.1 填写订单id, 用户id, 代金券id
        Long orderId = redisIDWorker.nextId("order");
        voucherOrder.setId(orderId);

        Long userId = UserHolder.getUser().getId();
        voucherOrder.setUserId(userId);

        voucherOrder.setVoucherId(voucherId);

        //6.2写入数据库
        save(voucherOrder);

        //7.返回订单id
        return Result.ok(orderId);
    }
}
