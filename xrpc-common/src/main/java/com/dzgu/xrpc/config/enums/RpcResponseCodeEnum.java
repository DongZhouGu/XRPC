package com.dzgu.xrpc.config.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * @description: RPC响应结果枚举
 * @Author： dzgu
 * @Date： 2022/4/22 23:40
 */
@AllArgsConstructor
@Getter
@ToString
public enum RpcResponseCodeEnum {
    SUCCESS(200, "success"),
    FAIL(500, "fail");
    private final int code;

    private final String message;
}
