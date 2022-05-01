package com.dzgu.xrpc.client.faultTolerantInvoker;

import com.dzgu.xrpc.client.core.NettyClient;
import com.dzgu.xrpc.dto.RpcMessage;
import com.dzgu.xrpc.dto.RpcRequest;
import com.dzgu.xrpc.dto.RpcResponse;
import com.dzgu.xrpc.extension.SPI;

/**
 * @description: 集群容错
 * @Author： dzgu
 * @Date： 2022/5/1 16:03
 */
@SPI(value = "fail-fast")
public interface FaultTolerantInvoker {
    RpcResponse<Object> doinvoke(NettyClient nettyClient, RpcMessage rpcMessage, String targetServiceUrl);
}
