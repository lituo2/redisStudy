package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisldWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService iSeckillVoucherService;
    @Resource
    private RedisldWorker redisldWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result setkillVoucher(Long voucherId) {
        //1查询优惠劵
        SeckillVoucher voucher = iSeckillVoucherService.getById(voucherId);

        //判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀还未开始~");
        }
        //判断秒杀是否结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已经结束");
        }
        //判断库存是否充足
        Integer stock = voucher.getStock();
        if (stock < 1) {
            return Result.fail("库存不足");
        }

        //拿取用户id
//        Long id = UserHolder.getUser().getId();
//        synchronized (id.toString().intern()) {
//
//            IVoucherOrderService proxy =(IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        }
        //改进  使用redis锁
        Long id = UserHolder.getUser().getId();

        //创建锁对象
        SimpleRedisLock lock = new SimpleRedisLock("order" + id, stringRedisTemplate);

        boolean isLock = lock.tryLock(1200);

        if (!isLock) {
            return Result.fail("一个用户只能购买一单！服务器繁忙");
        }
        try {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }finally {
            lock.unlock();
        }
    }
    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        //拿取用户id
        Long id = UserHolder.getUser().getId();

        //如果加到方法上，不同的用户也会有悲观锁，老师这里把锁加到了用户上，、
        // 同一用户数次的高并发才会加悲观锁，但是我没理解，不同的用户不正是高并发问题吗？为什么要把锁加到用户上呢？

            //一人一单问题
            int count = query().eq("user_id", id).eq("voucher_id", voucherId).count();
            if (count > 0) {
                return Result.fail("用户已经购买过一次");
            }

            //扣减库存(外加乐观锁的实现，用stock库存量还判断
            //这里判断库存和之前相同会导致失败率太高，其实库存情况只需要判断>0即可
            boolean success = iSeckillVoucherService.
                    update().setSql("stock = stock - 1")
                    .eq("voucher_id", voucherId).gt("stock",/*voucher.getStock()*/0) // where id = ? and stock = ?
                    .update();
            if (!success) {
                return Result.fail("修改库存失败");
            }

            //创建订单
            VoucherOrder voucherOrder = new VoucherOrder();

            long orderid = redisldWorker.nextId("order");
            voucherOrder.setId(orderid);


            voucherOrder.setUserId(id);

            voucherOrder.setVoucherId(voucherId);

            //插入用户秒杀成功表
            save(voucherOrder);

            return Result.ok(orderid);

    }
}
