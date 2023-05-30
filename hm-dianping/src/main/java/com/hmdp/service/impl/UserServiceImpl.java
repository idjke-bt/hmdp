package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.redis.RedisConstants;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1.判断提交的手机号是否合法，不合法则返回错误信息
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号不合要求！");
        }
        //2.若手机号符合要求，则生成验证码
        String code = RandomUtil.randomNumbers(6);
        //3.将验证码保存到redis中，用于校验
        //第四个位置是时间单位，第三个位置是过期时间
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY +phone, code, RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);

        //4.发送验证码 -->跳过这一步
        log.debug("发送验证码:{}成功！",code);
        return Result.ok();
    }

    /**
     *
     * @param loginForm 请求中参数
     * @param session 服务器上保存的session，含有用户的验证码等信息
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1.使用redis校验
        //1.1 校验手机号
        String phone = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone))
            return Result.fail("phone格式错误!");

        // 1.2 校验验证码
        String code = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone);
        if(code==null ||!code.equals(loginForm.getCode()))
            return Result.fail("验证码错误！");

        //2.根据手机号查找用户  select * from tb_user where phone=xx
        User user = query().eq("phone", phone).one();
        //3.如果用户不存在，则创建新用户
        if(user==null){
            user = createUserWithPhone(phone);
        }
        // 4.用户存在，则将用户信息保存到redis中
        // 4.1 随机生成token,作为key值
        String token = UUID.randomUUID().toString(true);
        // 4.2 将User转换为HashMap存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        //转为HashMap，并保证所有的字段都是字符串的类型
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true)
                        .setFieldValueEditor((filedName,filedValue)->filedValue.toString()) );
        // 4.3将数据存入redis
        stringRedisTemplate.opsForHash().putAll(RedisConstants.LOGIN_USER_KEY+token, userMap);
        // 4.4 为token设置有效期，避免redis中数据过多
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY+token, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        // 4.5 将token返回给浏览器
        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(8));
        //保存用户到数据库
        save(user);
        return user;
    }


}
