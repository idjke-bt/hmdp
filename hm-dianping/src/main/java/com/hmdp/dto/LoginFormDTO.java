package com.hmdp.dto;

import lombok.Data;
/**
 * 两种登录方式，验证码登录or密码登录
 */
@Data
public class LoginFormDTO {
    private String phone;
    /**
     * 验证码
     */
    private String code;
    private String password;
}
