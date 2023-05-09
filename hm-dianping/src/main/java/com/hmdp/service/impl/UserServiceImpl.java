package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

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
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1.判断提交的手机号是否合法，不合法则返回错误信息
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号不合要求！");
        }
        //2.若手机号符合要求，则生成验证码
        String code = RandomUtil.randomNumbers(6);
        //3.将验证码保存到session中，用于校验
        session.setAttribute("code", code);
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
        //1.校验验证码与手机号
        Object code = session.getAttribute("code");
        if(code==null ||!code.toString().equals(loginForm.getCode()))
            return Result.fail("验证码错误！");

        String phone = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone))
            return Result.fail("phone格式错误!");

        //2.根据手机号查找用户  select * from tb_user where phone=xx
        User user = query().eq("phone", phone).one();
        //3.如果用户不存在，则创建新用户
        if(user==null){
            user = createUserWithPhone(phone);
        }
        //4.用户存在，则将用户信息保存到session中
        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));
        return Result.ok();
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
