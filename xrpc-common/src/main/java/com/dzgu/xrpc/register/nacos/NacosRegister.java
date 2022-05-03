package com.dzgu.xrpc.register.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.dzgu.xrpc.consts.enums.RpcErrorMessageEnum;
import com.dzgu.xrpc.exception.RpcException;
import com.dzgu.xrpc.register.Register;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/5/3 9:29
 */
@Slf4j
public class NacosRegister implements Register {
    private NamingService namingService;

    public NacosRegister(String address) {
        this.namingService = NacosUtils.getNacosClient(address);
    }

    @Override
    public void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress) {
        try {
            NacosUtils.registerInstance(namingService, rpcServiceName, inetSocketAddress);
        } catch (NacosException e) {
            log.error("register service [{}] fail", rpcServiceName, e);
        }
    }

    @Override
    public void unregisterAllMyService(InetSocketAddress inetSocketAddress) {
        NacosUtils.clearRegistry(namingService, inetSocketAddress);
    }

    @Override
    public List<String> lookupService(String serviceKey) {
        // 从注册中心 拿到该rpcService下的所有server的Address
        List<String> serviceUrlList = null;
        try {
            serviceUrlList = NacosUtils.getAllInstance(namingService, serviceKey);
        } catch (NacosException e) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND, serviceKey);
        }

        return serviceUrlList;
    }

    @Override
    public void stop() {
        namingService=null;
    }
}
