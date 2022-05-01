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
     void setRegisterAddress(String registerAddress);

    void registerServiceMap(InetSocketAddress serverAddress, Map<String, Object> serviceMap);

    /**
     * register com.dzgu.xprc.service
     *
     * @param rpcServiceName    rpc com.dzgu.xprc.service name
     * @param inetSocketAddress com.dzgu.xprc.service address
     */
    void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress);

    void unregisterService(InetSocketAddress inetSocketAddress);


}

