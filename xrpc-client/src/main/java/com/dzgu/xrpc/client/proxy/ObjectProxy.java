package com.dzgu.xrpc.client.proxy;

import com.dzgu.xrpc.client.core.NettyClient;
import com.dzgu.xrpc.config.enums.RpcErrorMessageEnum;
import com.dzgu.xrpc.config.enums.RpcResponseCodeEnum;
import com.dzgu.xrpc.dto.RpcRequest;
import com.dzgu.xrpc.dto.RpcResponse;
import com.dzgu.xrpc.exception.RpcException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @description: 动态代理，调用一个方法时，实际调用它的代理方法
 * 代理方法中增强了 向服务端发送request，远程调用获取结果的逻辑
 * @Author： dzgu
 * @Date： 2022/4/25 9:31
 */
@Slf4j
public class ObjectProxy<T> implements InvocationHandler {
    private Class<T> clazz;
    private String version;

    public ObjectProxy(Class<T> clazz, String version) {
        this.clazz = clazz;
        this.version = version;
    }

    @SneakyThrows
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.info("invoked method: [{}]", method.getName());
        RpcRequest rpcRequest = RpcRequest.builder()
                .methodName(method.getName())
                .parameters(args)
                .parameterTypes(method.getParameterTypes())
                .className(method.getDeclaringClass().getName())
                .requestId(UUID.randomUUID().toString())
                .version(version)
                .build();
        CompletableFuture<RpcResponse<Object>> completableFuture = (CompletableFuture<RpcResponse<Object>>) NettyClient.getInstance().sendRequest(rpcRequest);
        RpcResponse<Object> rpcResponse = completableFuture.get();
        this.check(rpcResponse, rpcRequest);
        return rpcResponse.getData();

    }

    private void check(RpcResponse<Object> rpcResponse, RpcRequest rpcRequest) {
        if (rpcResponse == null) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, "interfaceName" + ":" + rpcRequest.getMethodName());
        }

        if (!rpcRequest.getRequestId().equals(rpcResponse.getRequestId())) {
            throw new RpcException(RpcErrorMessageEnum.REQUEST_NOT_MATCH_RESPONSE, "interfaceName" + ":" + rpcRequest.getMethodName());
        }

        if (rpcResponse.getCode() == null || !rpcResponse.getCode().equals(RpcResponseCodeEnum.SUCCESS.getCode())) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, "interfaceName" + ":" + rpcRequest.getMethodName());
        }
    }
}
