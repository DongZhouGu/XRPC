package com.dzgu.xrpc.register.zk;

import com.dzgu.xrpc.register.Register;
import com.dzgu.xrpc.register.RegisterFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description: zookeeper 注册中心工厂
 * @Author： dzgu
 * @Date： 2022/5/2 22:20
 */
public class ZkRegistryFactory implements RegisterFactory {
    private static final Map<String, ZkRegister> cache = new ConcurrentHashMap<>();

    @Override
    public Register getRegister(String address) {
        if (cache.containsKey(address)) {
            return cache.get(address);
        }
        ZkRegister zkRegister = new ZkRegister(address);
        cache.putIfAbsent(address, zkRegister);
        return cache.get(address);
    }
}
