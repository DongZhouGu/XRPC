package com.dzgu.xrpc.client.core;

import com.dzgu.xrpc.dto.RpcResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/4/25 22:01
 */
public class PendingRpcRequests {
    public static final Map<String, CompletableFuture<RpcResponse<Object>>> PENDING_RESPONSE_FUTURES=new ConcurrentHashMap<>();
    public void put(String requestId, CompletableFuture<RpcResponse<Object>> future) {
        PENDING_RESPONSE_FUTURES.put(requestId, future);
    }
    public void complete(RpcResponse<Object> rpcResponse) {
        CompletableFuture<RpcResponse<Object>> future = PENDING_RESPONSE_FUTURES.remove(rpcResponse.getRequestId());
        if (null != future) {
            future.complete(rpcResponse);
        } else {
            throw new IllegalStateException();
        }
    }
}
