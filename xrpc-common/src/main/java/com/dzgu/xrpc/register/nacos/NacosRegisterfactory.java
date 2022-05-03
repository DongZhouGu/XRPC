package com.dzgu.xrpc.register.nacos;

import com.dzgu.xrpc.register.Register;
import com.dzgu.xrpc.register.RegisterFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/5/3 9:30
 */
public class NacosRegisterfactory implements RegisterFactory {
    private static final Map<String, NacosRegister> cache = new ConcurrentHashMap<>();
    @Override
    public Register getRegister(String address) {
        if (cache.containsKey(address)) {
            return cache.get(address);
        }
        NacosRegister nacosRegister = new NacosRegister(address);
        cache.putIfAbsent(address, nacosRegister);
        return cache.get(address);
    }
}
