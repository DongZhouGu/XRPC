package com.dzgu.xrpc.register;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

/**
 * @description: 注册中心
 * @Author： dzgu
 * @Date： 2022/5/2 15:00
 */
public interface Register {
    default void registerServiceMap(Map<String, Object> serviceMap, InetSocketAddress serverAddress) {
        for (String rpcServiceName : serviceMap.keySet()) {
            registerService(rpcServiceName, serverAddress);
        }
    }

    /**
     * 向注册中心注册服务
     */
    void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress);

    /**
     * 取消所有本机的服务，用于关机的时候
     */
    void unregisterAllMyService(InetSocketAddress inetSocketAddress);

    /**
     *  查找含有某个服务的所有服务端地址
     */
    public List<String> lookupService(String serviceKey);

    /**
     *  关闭注册中心
     */
    public void stop();
}
