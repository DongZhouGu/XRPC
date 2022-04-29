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
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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

    /**
     * 获取被调用服务的动态代理类
     */
    public static <T> T createService(Class<T> interfaceClass, String version) {
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new ObjectProxy<T>(interfaceClass, version)
        );
    }
    /**
     *  客户端主要逻辑，包括发送请求，相应结果与请求的绑定
     *
     */
    @SneakyThrows
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.info("client invoked method: [{}]", method.getName());
        RpcRequest rpcRequest = RpcRequest.builder()
                .methodName(method.getName())
                .parameters(args)
                .parameterTypes(method.getParameterTypes())
                .className(method.getDeclaringClass().getName())
                .requestId(UUID.randomUUID().toString())
                .version(version)
                .build();
        // 向服务端发送请求
        CompletableFuture<RpcResponse<Object>> completableFuture = (CompletableFuture<RpcResponse<Object>>) NettyClient.getInstance().sendRequest(rpcRequest);
        // 阻塞等待调用请求的结果，当 Netty Client 收到对应请求的回复时，future.complete（response）,完成相应
        // TODO 异步调用
        RpcResponse<Object> rpcResponse = completableFuture.get(10, TimeUnit.SECONDS);
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
