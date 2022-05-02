package com.dzgu.xrpc.server.invoke;

import com.dzgu.xrpc.dto.RpcRequest;
import com.dzgu.xrpc.extension.SPI;
import com.dzgu.xrpc.util.ServiceUtil;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/5/2 15:58
 */
@SPI(value = "jdk")
public interface Invoker {
    Object invoke(RpcRequest rpcRequest, Object service);

}
