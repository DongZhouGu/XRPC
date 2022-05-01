package com.dzgu.xrpc.client.discover;

import com.dzgu.xrpc.client.loadbalance.LoadBalance;
import com.dzgu.xrpc.dto.RpcRequest;
import com.dzgu.xrpc.extension.SPI;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/4/25 9:20
 */
@SPI
public interface ServiceDiscovery {

    public void setRegisterAddress(String registerAddress);

    public List<String> lookupService(String serviceKey);

    public void stop();
}
