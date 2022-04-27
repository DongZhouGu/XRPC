package com.dzgu.xrpc.config.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/4/22 19:15
 */
@AllArgsConstructor
@Getter
public enum SerializerTypeEnum {
    HESSIAN((byte) 1, "hessian"),

    KRYO((byte) 2, "kryo"),

    PROTOSTUFF((byte) 2, "protostuff");


    private final byte code;
    private final String name;

    public static String getName(byte code) {
        for (SerializerTypeEnum c : SerializerTypeEnum.values()) {
            if (c.getCode() == code) {
                return c.name;
            }
        }
        return null;
    }
}
