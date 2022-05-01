package com.dzgu.xrpc.extension;

import java.lang.annotation.*;

/**
 * @description:与 {@link SPI} 联合使用，在方法上面标记。
 *  * 代理生成的扩展类会自动读取 URL 参数，再根据这个参数类型，获取对应的扩展类
 * @Author： dzgu
 * @Date： 2022/4/30 21:33
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Adaptive {
    String value() default "";
}
