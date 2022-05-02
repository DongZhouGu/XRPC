package com.dzgu.xrpc.server.invoke;

import com.dzgu.xrpc.dto.RpcRequest;
import com.dzgu.xrpc.exception.RpcException;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/5/2 15:59
 */
@Slf4j
public class InvokerJDK implements Invoker{
    @Override
    public Object invoke(RpcRequest rpcRequest, Object service) {
        Object result;
        try {
            Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParameterTypes());
            method.setAccessible(true);
            result = method.invoke(service, rpcRequest.getParameters());
            log.info("com.dzgu.xprc.service:[{}] successful invoke method:[{}]", rpcRequest.getClassName(), rpcRequest.getMethodName());
        } catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
            throw new RpcException(e.getMessage(), e);
        }
        return result;
    }
}
