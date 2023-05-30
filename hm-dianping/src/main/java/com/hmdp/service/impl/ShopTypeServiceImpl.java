package com.hmdp.service.impl;

import cn.hutool.core.collection.CollectionUtil;
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

import static com.hmdp.utils.redis.RedisConstants.CACHE_SHOP_TYPE_KEY;

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
    public Result queryTypeList() {
        //1. 从redis中查询数据
        List<String> typeList = stringRedisTemplate.opsForList().range(CACHE_SHOP_TYPE_KEY, 0, -1);
        if(CollectionUtil.isNotEmpty(typeList)){
            //将typeList进行反序列化
            List<ShopType> shopTypeList = new ArrayList<>();
            for(String str: typeList){
                shopTypeList.add(JSONUtil.toBean(str, ShopType.class));
            }
            return Result.ok(shopTypeList);
        }

        //2.若redis中没有该数据，则查询数据库，并加载到redis中
        //2.1 查询数据库
        List<ShopType> shopTypeList = query().orderByDesc("sort").list();
        if(shopTypeList==null)
            return Result.fail("不存在该数据！");
        //2.2 序列化，并存储到redis中
        List<String> strList = new ArrayList<>();
        for(ShopType st : shopTypeList){
            strList.add(JSONUtil.toJsonStr(st));
        }
        stringRedisTemplate.opsForList().leftPushAll(CACHE_SHOP_TYPE_KEY, strList);
        return Result.ok(shopTypeList);

    }
}
