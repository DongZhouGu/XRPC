package com.dzgu.xrpc.client.loadbalance;

import cn.hutool.core.collection.CollectionUtil;
import com.dzgu.xrpc.dto.RpcRequest;
import com.dzgu.xrpc.extension.SPI;

import java.util.List;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/4/25 9:18
 */
@SPI
public interface LoadBalance {
    String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest);

    default String selectServiceAddress(List<String> serviceAddresses, RpcRequest rpcRequest) {
        if (CollectionUtil.isEmpty(serviceAddresses)) {
            return null;
        }
        if (serviceAddresses.size() == 1) {
            return serviceAddresses.get(0);
        }
        return doSelect(serviceAddresses, rpcRequest);
    }
}
