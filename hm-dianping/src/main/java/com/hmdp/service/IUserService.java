package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {
    /**
     * 发送手机验证码
     */
    public Result sendCode(String phone, HttpSession session);

    /**
     * 实现用户登录，包括登录信息的校验
     * @return 登录是否成功
     */
    public Result login(LoginFormDTO loginForm, HttpSession session);

}
