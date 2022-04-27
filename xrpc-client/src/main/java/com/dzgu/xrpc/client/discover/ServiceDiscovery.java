package com.dzgu.xrpc.client.discover;

import com.dzgu.xrpc.dto.RpcRequest;
import com.dzgu.xrpc.extension.SPI;

import java.net.InetSocketAddress;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/4/25 9:20
 */
@SPI
public interface ServiceDiscovery {
    public InetSocketAddress lookupService(RpcRequest rpcRequest);
    public void stop();
}
