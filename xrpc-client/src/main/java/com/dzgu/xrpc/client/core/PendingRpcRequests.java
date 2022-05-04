package com.dzgu.xrpc.client.core;

import com.dzgu.xrpc.client.async.RpcFuture;
import com.dzgu.xrpc.dto.RpcResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description: 未收到回复的请求
 * @Author： dzgu
 * @Date： 2022/4/25 22:01
 */
public class PendingRpcRequests {
    public static final Map<String, RpcFuture> PENDING_RESPONSE_FUTURES = new ConcurrentHashMap<>();

    public void put(String requestId, RpcFuture future) {
        PENDING_RESPONSE_FUTURES.put(requestId, future);
    }

    public void remove(String requestId) {
        PENDING_RESPONSE_FUTURES.remove(requestId);
    }

    /**
     * 将请求与调用结果响应绑定
     *
     * @param rpcResponse 收到服务端发来的调用结果
     */
    public void complete(RpcResponse<Object> rpcResponse) {
        RpcFuture future = PENDING_RESPONSE_FUTURES.remove(rpcResponse.getRequestId());
        if (null != future) {
            future.complete(rpcResponse);
        } else {
            throw new IllegalStateException();
        }
    }
}
