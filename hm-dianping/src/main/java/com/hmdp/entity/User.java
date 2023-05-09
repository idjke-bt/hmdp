package com.hmdp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 要求每个用户至少有手机号(phone)和用户名（nickName)
 * 手机号用户输入，用户名可以随机生成
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Data  //lombok注解
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("tb_user")  //mp注解：标识实体类对应的表
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键注解：
     * value--主键字段名，
     * type--主键类型，IdType.Auto表示id会自动增长
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 手机号码
     */
    private String phone;

    /**
     * 密码，加密存储
     */
    private String password;

    /**
     * 昵称，默认是随机字符
     */
    private String nickName;

    /**
     * 用户头像
     */
    private String icon = "";

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;


}
