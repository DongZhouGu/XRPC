package com.dzgu.xrpc.server.registry;
import java.net.InetSocketAddress;
import java.util.Map;

/**
 * @description: 注册中心
 * @Author： dzgu
 * @Date： 2022/5/2 15:00
 */
public interface Registry {
    default void registerServiceMap(Map<String, Object> serviceMap, InetSocketAddress serverAddress){
        for (String rpcServiceName : serviceMap.keySet()) {
            register(rpcServiceName, serverAddress);
        }
    }

    /**
     * 向注册中心注册服务
     */
    void register(String rpcServiceName, InetSocketAddress inetSocketAddress);

    /**
     * 取消所有本机的服务，用于关机的时候
     */
    void unregisterAllMyService(InetSocketAddress inetSocketAddress);
}
