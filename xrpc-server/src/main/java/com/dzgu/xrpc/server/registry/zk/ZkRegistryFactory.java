package com.dzgu.xrpc.server.registry.zk;

import com.dzgu.xrpc.server.registry.Registry;
import com.dzgu.xrpc.server.registry.RegistryFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/5/2 14:58
 */
public class ZkRegistryFactory implements RegistryFactory {
    private static final Map<String, ZkRegistry> cache = new ConcurrentHashMap<>();
    @Override
    public Registry getRegistry(String address) {
        if (cache.containsKey(address)) {
            return cache.get(address);
        }
        ZkRegistry zkRegistry = new ZkRegistry(address);
        cache.putIfAbsent(address, zkRegistry);
        return cache.get(address);
    }
}
