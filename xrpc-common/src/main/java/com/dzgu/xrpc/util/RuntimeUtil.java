package com.dzgu.xrpc.util;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/4/24 14:41
 */
public class RuntimeUtil {
    /**
     * 获取CPU的核心数
     *
     * @return cpu的核心数
     */
    public static int cpus() {
        return Runtime.getRuntime().availableProcessors();
    }
}
