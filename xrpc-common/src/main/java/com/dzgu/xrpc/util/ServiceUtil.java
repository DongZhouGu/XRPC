package com.dzgu.xrpc.util;

/**
 * @description:  服务工具类
 * @Author： dzgu
 * @Date： 2022/4/21 23:05
 */
public class ServiceUtil {
    public static final String SERVICE_CONCAT_TOKEN = "#";

    public static String makeServiceKey(String interfaceName, String version) {
        String serviceKey = interfaceName;
        if (version != null && version.trim().length() > 0) {
            serviceKey += SERVICE_CONCAT_TOKEN.concat(version);
        }
        return serviceKey;
    }
}
