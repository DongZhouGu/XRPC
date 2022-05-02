package com.dzgu.xrpc.client.discover.zk;

import com.dzgu.xrpc.client.discover.DiscoveryFactory;
import com.dzgu.xrpc.client.discover.ServiceDiscovery;

import java.rmi.registry.Registry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/5/2 15:45
 */
public class ZkDiscoveryFactory implements DiscoveryFactory {
    private static final Map<String, ZkServiceDiscovery> cache = new ConcurrentHashMap<>();
    @Override
    public ServiceDiscovery getDiscovery(String address) {
        if (cache.containsKey(address)) {
            return cache.get(address);
        }
        ZkServiceDiscovery zkRegistry = new ZkServiceDiscovery(address);
        cache.putIfAbsent(address, zkRegistry);
        return cache.get(address);
    }
}
