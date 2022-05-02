package com.dzgu.xrpc.client.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @description: 注册中心客户端缓存
 * @Author： dzgu
 * @Date： 2022/5/2 22:30
 */
public class ServerDiscoveryCache {
    /**
     * key: serviceKey
     */
    private static final Map<String, List<String>> SERVER_MAP = new ConcurrentHashMap<>();

    /**
     * 客户端注入的远程服务service class
     */
    public static final List<String> SERVER_CLASS_NAMES = new ArrayList<>();


    /**
     * 添加缓存
     *
     * @param serviceName
     * @param serviceList
     */
    public static void put(String serviceName, List<String> serviceList) {
        SERVER_MAP.put(serviceName, serviceList);
    }

    /**
     * 去除指定缓存
     *
     * @param serviceName
     * @param serviceAddress
     */
    public static void remove(String serviceName, String serviceAddress) {
        SERVER_MAP.computeIfPresent(serviceName, (key, value) ->
                value.stream().filter(s -> !s.toString().equals(serviceAddress)).collect(Collectors.toList())
        );
    }

    /**
     * 删除指定服务的缓存
     *
     * @param serviceName
     */
    public static void removeAll(String serviceName) {
        SERVER_MAP.remove(serviceName);
    }

    /**
     * 指定服务是否有缓存节点
     *
     * @param serviceName
     * @return
     */
    public static boolean isEmpty(String serviceName) {
        return SERVER_MAP.get(serviceName) == null || SERVER_MAP.get(serviceName).isEmpty();
    }

    /**
     * 获取指定服务的缓存节点
     *
     * @param serviceName
     * @return
     */
    public static List<String> get(String serviceName) {
        return SERVER_MAP.get(serviceName);
    }
}
