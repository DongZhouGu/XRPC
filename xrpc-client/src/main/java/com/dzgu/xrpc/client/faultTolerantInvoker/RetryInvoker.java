package com.dzgu.xrpc.client.faultTolerantInvoker;

import com.dzgu.xrpc.client.core.NettyClient;
import com.dzgu.xrpc.client.discover.ServerDiscoveryCache;
import com.dzgu.xrpc.dto.RpcMessage;
import com.dzgu.xrpc.dto.RpcRequest;
import com.dzgu.xrpc.dto.RpcResponse;
import com.dzgu.xrpc.exception.RpcException;
import com.dzgu.xrpc.util.ServiceUtil;
import io.protostuff.Rpc;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static com.dzgu.xrpc.consts.enums.RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE;

/**
 * @description: 容错策略-重试
 * @Author： dzgu
 * @Date： 2022/5/1 23:58
 */
@Slf4j
public class RetryInvoker implements FaultTolerantInvoker {
    /**
     * 默认重试次数
     */
    public static int DEFAULT_RETRY_TIMES = 3;


    @Override
    public RpcResponse<Object> doinvoke(NettyClient nettyClient, RpcMessage rpcMessage, String targetServiceUrl) {
        for (int i = 0; i < DEFAULT_RETRY_TIMES; i++) {
            try {
                RpcResponse<Object> result = nettyClient.sendRequest( rpcMessage, targetServiceUrl);
                if (result != null) {
                    return result;
                }
            } catch (RpcException ex) {
                log.error("invoke error. retry times=" + i, ex);
            }
        }
        throw new RpcException(SERVICE_INVOCATION_FAILURE);
    }
}
