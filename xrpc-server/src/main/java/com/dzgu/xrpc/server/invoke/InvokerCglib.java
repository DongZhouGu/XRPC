package com.dzgu.xrpc.server.invoke;

import com.dzgu.xrpc.dto.RpcRequest;
import com.dzgu.xrpc.exception.RpcException;
import net.sf.cglib.reflect.FastClass;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/5/2 16:01
 */
public class InvokerCglib implements Invoker{
    @Override
    public Object invoke(RpcRequest rpcRequest, Object service) {
        Object result;
        try {
            Class<?> serviceClass = service.getClass();
            FastClass fastClass = FastClass.create(serviceClass);
            int methodIndex = fastClass.getIndex(rpcRequest.getMethodName(), rpcRequest.getParameterTypes());
            result = fastClass.invoke(methodIndex, service, rpcRequest.getParameters());
        } catch (Exception e) {
            throw new RpcException(e.getMessage(), e);
        }
        return result;
    }
}
