package com.dzgu.xrpc.consts.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @description: 序列化类型枚举
 * @Author： dzgu
 * @Date： 2022/4/22 19:15
 */
@AllArgsConstructor
@Getter
public enum SerializerTypeEnum {
    HESSIAN((byte) 1, "hessian"),
    KRYO((byte) 2, "kryo"),
    PROTOSTUFF((byte) 3, "protostuff");

    private final byte code;
    private final String name;

    public static byte getCode(String name) {
        for (SerializerTypeEnum c : SerializerTypeEnum.values()) {
            if (c.getName().equals(name)) {
                return c.code;
            }
        }
        return HESSIAN.getCode();
    }

    public static String getName(byte code) {
        for (SerializerTypeEnum c : SerializerTypeEnum.values()) {
            if (c.getCode() == code) {
                return c.name;
            }
        }
        return null;
    }
}
