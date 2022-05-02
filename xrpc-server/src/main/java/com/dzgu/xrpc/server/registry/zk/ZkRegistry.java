package com.dzgu.xrpc.server.registry.zk;

import com.dzgu.xrpc.server.registry.Registry;
import com.dzgu.xrpc.zookeeper.CuratorUtils;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;

/**
 * @description: zk注册中心
 * @Author： dzgu
 * @Date： 2022/5/2 14:58
 */
public class ZkRegistry implements Registry {
    private CuratorFramework zkClient;

    public ZkRegistry(String address) {
        this.zkClient = CuratorUtils.getZkClient(address);
        ;
    }

    @Override
    public void register(String rpcServiceName, InetSocketAddress inetSocketAddress) {
        String servicePath = CuratorUtils.ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName + inetSocketAddress.toString();
        CuratorUtils.createPersistentNode(zkClient, servicePath);
    }


    @Override
    public void unregisterAllMyService(InetSocketAddress inetSocketAddress) {
        CuratorUtils.clearRegistry(zkClient, inetSocketAddress);
        zkClient.close();
    }
}
