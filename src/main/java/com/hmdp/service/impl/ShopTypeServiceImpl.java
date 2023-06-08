package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;



    @Override
    public Result queryList() {
        String key = "typelist";
        List<String> shopTypeStrList = stringRedisTemplate.opsForList().range(key, 0, -1);
        if (shopTypeStrList != null && shopTypeStrList.size() != 0) {
            List<ShopType> typeList = new ArrayList<>();
            for (String shopType : shopTypeStrList) {
                typeList.add(JSONUtil.toBean(shopType, ShopType.class));
            }
            return Result.ok(typeList);
        }
        List<ShopType> typelist = this.query().orderByAsc("sort").list();

        ArrayList<String> stringArrayList = new ArrayList<>();

        for (ShopType shopType : typelist) {
            stringArrayList.add(JSONUtil.toJsonStr(shopType));
        }

        stringRedisTemplate.opsForList().rightPushAll(key,stringArrayList);

        return Result.ok(typelist);
    }
}
