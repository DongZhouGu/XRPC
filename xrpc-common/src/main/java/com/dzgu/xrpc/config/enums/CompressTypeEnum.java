package com.dzgu.xrpc.config.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/4/22 17:49
 */
@AllArgsConstructor
@Getter
public enum CompressTypeEnum {
    /**
     * 伪压缩器，等于不使用压缩
     */
    DUMMY((byte) 0, "dummy"),
    GZIP((byte) 1, "gzip"),
    UNZIP((byte) 2, "unzip");

    private final byte code;
    private final String name;

    public static String getName(byte code) {
        for (CompressTypeEnum c : CompressTypeEnum.values()) {
            if (c.getCode() == code) {
                return c.getName();
            }
        }
        return null;
    }


}
