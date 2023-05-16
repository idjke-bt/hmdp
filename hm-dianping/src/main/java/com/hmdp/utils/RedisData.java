package com.hmdp.utils;

import lombok.Data;

import java.time.LocalDateTime;

/**
* @Description: 对类进行封装，添加expireTime属性
* @Author: yf-idjke
* @Date: 2023/5/15
*/

@Data
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;
}
