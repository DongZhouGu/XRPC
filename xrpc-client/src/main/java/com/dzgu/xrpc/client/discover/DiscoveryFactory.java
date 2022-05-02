package com.dzgu.xrpc.client.discover;

import com.dzgu.xrpc.extension.SPI;

import java.rmi.registry.Registry;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/5/2 15:39
 */
@SPI(value = "zookeeper")
public interface DiscoveryFactory {
    /**
     * 获取注册中心
     *
     * @param address 注册中心的地址。
     * @return 如果协议类型跟注册中心匹配上了，返回对应的配置中心实例
     */
    ServiceDiscovery getDiscovery(String address);
}
