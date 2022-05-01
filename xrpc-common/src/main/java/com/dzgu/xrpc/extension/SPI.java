package com.dzgu.xrpc.extension;

import java.lang.annotation.*;

/**
 * @description: 被此注解标记的类，表示是一个扩展接口
 * @Author： dzgu
 * @Date： 2022/4/22 19:04
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SPI {
    /**
     * 默认扩展类全路径
     *
     * @return 默认不填是 default
     */
    String value() default "default";
}
