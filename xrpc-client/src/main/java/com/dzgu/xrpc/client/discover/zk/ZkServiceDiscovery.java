package com.dzgu.xrpc.client.discover.zk;

import cn.hutool.core.collection.CollUtil;
import com.dzgu.xrpc.client.discover.ServiceDiscovery;
import com.dzgu.xrpc.client.loadbalance.LoadBalance;
import com.dzgu.xrpc.consts.enums.RpcErrorMessageEnum;
import com.dzgu.xrpc.dto.RpcRequest;
import com.dzgu.xrpc.exception.RpcException;
import com.dzgu.xrpc.extension.ExtensionLoader;
import com.dzgu.xrpc.properties.RpcConfig;
import com.dzgu.xrpc.util.ServiceUtil;
import com.dzgu.xrpc.zookeeper.CuratorClient;
import com.dzgu.xrpc.zookeeper.CuratorUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/4/25 9:20
 */
@Slf4j
public class ZkServiceDiscovery implements ServiceDiscovery {
    private LoadBalance loadBalance;
    private CuratorFramework zkClient;

    public ZkServiceDiscovery() {
    }


    @Override
    public void setRegisterAddress(String registerAddress) {
        this.zkClient = CuratorUtils.getZkClient(registerAddress);
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
        zkClient.close();
    }
}
