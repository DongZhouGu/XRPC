package com.dzgu.xrpc.client.faultTolerantInvoker;

import com.dzgu.xrpc.client.core.NettyClient;
import com.dzgu.xrpc.dto.RpcMessage;
import com.dzgu.xrpc.dto.RpcResponse;

/**
 * @description: 容错策略-快速失败
 * @Author： dzgu
 * @Date： 2022/5/1 23:59
 */
public class FailFastInvoker implements FaultTolerantInvoker {
    @Override
    public RpcResponse<Object> doinvoke(NettyClient nettyClient, RpcMessage rpcMessage, String targetServiceUrl) {
        return nettyClient.sendRequest(rpcMessage, targetServiceUrl);
    }
}
