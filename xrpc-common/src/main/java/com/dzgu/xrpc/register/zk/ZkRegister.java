package com.dzgu.xrpc.register.zk;

import com.dzgu.xrpc.register.Register;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/5/2 22:19
 */
public class ZkRegister implements Register {
    @Override
    public void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress) {

    }

    @Override
    public void unregisterAllMyService(InetSocketAddress inetSocketAddress) {

    }

    @Override
    public List<String> lookupService(String serviceKey) {
        return null;
    }

    @Override
    public void stop() {

    }
}
