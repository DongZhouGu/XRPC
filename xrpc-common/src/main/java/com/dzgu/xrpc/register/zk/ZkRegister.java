package com.dzgu.xrpc.register.zk;

import cn.hutool.core.collection.CollUtil;
import com.dzgu.xrpc.consts.enums.RpcErrorMessageEnum;
import com.dzgu.xrpc.exception.RpcException;
import com.dzgu.xrpc.register.Register;
import com.dzgu.xrpc.zookeeper.CuratorUtils;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @description: zookeeper注册中心
 * @Author： dzgu
 * @Date： 2022/5/2 22:19
 */
public class ZkRegister implements Register {
    private CuratorFramework zkClient;

    public ZkRegister(String address) {
        this.zkClient = CuratorUtils.getZkClient(address);
    }

    @Override
    public void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress) {
        String servicePath = CuratorUtils.ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName + inetSocketAddress.toString();
        CuratorUtils.createPersistentNode(zkClient, servicePath);
    }

    @Override
    public void unregisterAllMyService(InetSocketAddress inetSocketAddress) {
        CuratorUtils.clearRegistry(zkClient, inetSocketAddress);
        zkClient.close();
    }

    @Override
    public List<String> lookupService(String serviceKey) {
        // 从注册中心 拿到该rpcService下的所有server的Address
        List<String> serviceUrlList = CuratorUtils.getChildrenNodes(zkClient, serviceKey);
        if (CollUtil.isEmpty(serviceUrlList)) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND, serviceKey);
        }
        return serviceUrlList;
    }

    @Override
    public void stop() {
        this.zkClient.close();
    }
}
