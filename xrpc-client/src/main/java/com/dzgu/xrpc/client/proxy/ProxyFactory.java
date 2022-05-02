package com.dzgu.xrpc.client.proxy;

import com.dzgu.xrpc.client.core.NettyClient;
import com.dzgu.xrpc.client.core.ServerDiscoveryCache;
import com.dzgu.xrpc.client.faultTolerantInvoker.FaultTolerantInvoker;
import com.dzgu.xrpc.client.faultTolerantInvoker.RetryInvoker;
import com.dzgu.xrpc.client.loadbalance.LoadBalance;
import com.dzgu.xrpc.consts.RpcConstants;
import com.dzgu.xrpc.consts.enums.CompressTypeEnum;
import com.dzgu.xrpc.consts.enums.RpcErrorMessageEnum;
import com.dzgu.xrpc.consts.enums.RpcResponseCodeEnum;
import com.dzgu.xrpc.consts.enums.SerializerTypeEnum;
import com.dzgu.xrpc.dto.RpcMessage;
import com.dzgu.xrpc.dto.RpcRequest;
import com.dzgu.xrpc.dto.RpcResponse;
import com.dzgu.xrpc.exception.RpcException;
import com.dzgu.xrpc.register.Register;
import com.dzgu.xrpc.util.ServiceUtil;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.dzgu.xrpc.consts.RpcConstants.REQUEST_ID;

/**
 * @description: 客户端代理工程
 * @Author： dzgu
 * @Date： 2022/5/1 19:06
 */
@Setter
@Accessors(chain = true)
@Slf4j
public class ProxyFactory {
    private Register register;

    private NettyClient nettyClient;

    private LoadBalance loadBalance;

    private FaultTolerantInvoker faultTolerantInvoker;
    private int retryTime = 3;
    private String compress;
    private String serializer;

    private Map<String, Object> objectCache = new HashMap<>();


    /**
     * 获取被调用服务的动态代理类
     */
    public <T> T getProxy(Class<T> interfaceClass, String version) {
        return (T) objectCache.computeIfAbsent(interfaceClass.getName() + version, clz ->
                Proxy.newProxyInstance(
                        interfaceClass.getClassLoader(),
                        new Class<?>[]{interfaceClass},
                        new ObjectProxy<T>(interfaceClass, version)
                )
        );
    }

    private class ObjectProxy<T> implements InvocationHandler {
        private Class<T> clazz;
        private String version;

        public ObjectProxy(Class<T> clazz, String version) {
            this.clazz = clazz;
            this.version = version;
        }

        /**
         * 客户端主要逻辑，包括发送请求，相应结果与请求的绑定
         */
        @SneakyThrows
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            log.info("client invoked method: [{}]", method.getName());
            // 构建request对象
            RpcRequest rpcRequest = RpcRequest.builder()
                    .methodName(method.getName())
                    .parameters(args)
                    .parameterTypes(method.getParameterTypes())
                    .className(method.getDeclaringClass().getName())
                    .requestId(UUID.randomUUID().toString())
                    .version(version)
                    .build();
            String rpcServiceName = rpcRequest.getClassName();
            String version = rpcRequest.getVersion();
            String serviceKey = ServiceUtil.makeServiceKey(rpcServiceName, version);
            // 从注册中心 拿到该rpcService下的所有server的Address
            List<String> serviceUrlList = getServiceList(serviceKey);
            // 负载均衡
            String targetServiceUrl = loadBalance.selectServiceAddress(serviceUrlList, rpcRequest);
            log.info("Successfully found the com.dzgu.xprc.service address:[{}]", targetServiceUrl);
            //封装Message
            RpcMessage rpcMessage = RpcMessage.builder().data(rpcRequest)
                    .codec(SerializerTypeEnum.getCode(serializer))
                    .compress(CompressTypeEnum.getCode(compress))
                    .requestId(REQUEST_ID.getAndIncrement())
                    .messageType(RpcConstants.REQUEST_TYPE).build();
            // Netty向服务端发送请求
            RpcResponse<Object> rpcResponse = null;
            if (faultTolerantInvoker instanceof RetryInvoker) {
                RetryInvoker.DEFAULT_RETRY_TIMES = retryTime;
            }
            rpcResponse = faultTolerantInvoker.doinvoke(nettyClient, rpcMessage, targetServiceUrl);
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

    public List<String> getServiceList(String serviceName) {
        List<String> serviceUrlList;
        synchronized (serviceName) {
            if (ServerDiscoveryCache.isEmpty(serviceName)) {
                serviceUrlList = register.lookupService(serviceName);
                ServerDiscoveryCache.put(serviceName, serviceUrlList);
            } else {
                serviceUrlList = ServerDiscoveryCache.get(serviceName);
            }
        }
        return serviceUrlList;
    }
}
