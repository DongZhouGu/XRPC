package com.dzgu.xrpc.server.registry;

import com.dzgu.xrpc.extension.SPI;

/**
 * @description: 注册中心工厂
 * @Author： dzgu
 * @Date： 2022/5/2 14:54
 */
@SPI(value = "zk")
public interface RegistryFactory {
    /**
     * 获取注册中心
     *
     * @param address 注册中心的地址。
     * @return 如果协议类型跟注册中心匹配上了，返回对应的配置中心实例
     */
    Registry getRegistry(String address);
}
