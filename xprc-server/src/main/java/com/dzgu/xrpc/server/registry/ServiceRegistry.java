package com.dzgu.xrpc.server.registry;

import com.dzgu.xrpc.extension.SPI;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/4/26 22:25
 */
@SPI
public interface ServiceRegistry {
    void registerServiceMap(InetSocketAddress serverAddress, Map<String, Object> serviceMap) ;
    /**
     * register service
     *
     * @param rpcServiceName    rpc service name
     * @param inetSocketAddress service address
     */
    void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress);

    void unregisterService(InetSocketAddress inetSocketAddress);


}

