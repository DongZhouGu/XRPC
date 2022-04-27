package com.dzgu.xrpc.server.registry.zk;

import com.dzgu.xrpc.server.registry.ServiceRegistry;
import com.dzgu.xrpc.zookeeper.CuratorClient;
import com.dzgu.xrpc.zookeeper.CuratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @description: 服务注册功能
 * @Author： dzgu
 * @Date： 2022/4/20 23:55
 */
@Slf4j
public class ZkServiceRegistry implements ServiceRegistry {
    private CuratorFramework zkClient;

    public ZkServiceRegistry() {
        this.zkClient = CuratorUtils.getZkClient();
    }

    @Override
    public void registerServiceMap(InetSocketAddress serverAddress, Map<String, Object> serviceMap) {
        for (String rpcServiceName : serviceMap.keySet()) {
            registerService(rpcServiceName, serverAddress);
        }
    }

    @Override
    public void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress) {
        String servicePath = CuratorUtils.ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName + inetSocketAddress.toString();
        CuratorUtils.createPersistentNode(zkClient, servicePath);
    }

    @Override
    public void unregisterService(InetSocketAddress inetSocketAddress) {
        CuratorUtils.clearRegistry(zkClient, inetSocketAddress);
        zkClient.close();
    }
}
