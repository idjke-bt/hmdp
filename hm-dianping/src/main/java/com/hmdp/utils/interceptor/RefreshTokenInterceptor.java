package com.hmdp.utils.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.redis.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RefreshTokenInterceptor implements HandlerInterceptor {
    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1.获取请求头中的token
        String token = request.getHeader("authorization"); //前端
        //1.1 如果token不存在，表明还没有login
        if (StrUtil.isBlank(token)) {
            return true;
        }
        //2.基于token获取redis中的用户
        String key = RedisConstants.LOGIN_USER_KEY+token;
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        if(entries.isEmpty()){
            return true;
        }
        //3.将查询到的用hash存储的user数据再转为UserDTO对象
        UserDTO user = BeanUtil.fillBeanWithMap(entries, new UserDTO(), false);

        // 4.存在，保存到ThreadLocal中
        UserHolder.saveUser(user);

        // 5.刷新token的有效期
        stringRedisTemplate.expire(key, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        // 6.放行
        return true;
    }
}
