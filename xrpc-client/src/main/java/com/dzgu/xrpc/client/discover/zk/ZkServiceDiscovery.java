package com.dzgu.xrpc.client.discover.zk;

import cn.hutool.core.collection.CollUtil;
import com.dzgu.xrpc.client.discover.ServiceDiscovery;
import com.dzgu.xrpc.client.loadbalance.LoadBalance;
import com.dzgu.xrpc.config.enums.RpcErrorMessageEnum;
import com.dzgu.xrpc.dto.RpcRequest;
import com.dzgu.xrpc.exception.RpcException;
import com.dzgu.xrpc.extension.ExtensionLoader;
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
    private final LoadBalance loadBalance;
    private CuratorFramework zkClient;

    public ZkServiceDiscovery() {
        this.loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension("loadBalance");
        this.zkClient = CuratorUtils.getZkClient();
    }

    @Override
    public InetSocketAddress lookupService(RpcRequest rpcRequest) {
        String rpcServiceName = rpcRequest.getClassName();
        String version=rpcRequest.getVersion();
        String serviceKey = ServiceUtil.makeServiceKey(rpcServiceName, version);
        // 从注册中心 拿到该rpcService下的所有server的Address
        List<String> serviceUrlList = CuratorUtils.getChildrenNodes(zkClient, serviceKey);
        if (CollUtil.isEmpty(serviceUrlList)) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND, rpcServiceName);
        }
        // 负载均衡
        String targetServiceUrl = loadBalance.selectServiceAddress(serviceUrlList, rpcRequest);
        log.info("Successfully found the service address:[{}]", targetServiceUrl);
        String[] socketAddressArray = targetServiceUrl.split(":");
        String host = socketAddressArray[0];
        int port = Integer.parseInt(socketAddressArray[1]);
        return new InetSocketAddress(host, port);
    }

    @Override
    public void stop() {
        zkClient.close();
    }
}
