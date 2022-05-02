package com.dzgu.xrpc.server.core;
import com.dzgu.xrpc.consts.enums.RpcErrorMessageEnum;
import com.dzgu.xrpc.exception.RpcException;
import com.dzgu.xrpc.util.ServiceUtil;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description: 注册中心服务端缓存
 * @Author： dzgu
 * @Date： 2022/4/22 23:05
 */
@Slf4j
public class ServiceRegisterCache {
    /**
     *  服务缓存
     */
    private final Map<String, Object> serviceMap;

    public ServiceRegisterCache() {
        serviceMap = new ConcurrentHashMap<>();
    }

    public void addService(String interfaceName, String version, Object serviceBean) {
        String serviceKey = ServiceUtil.makeServiceKey(interfaceName, version);
        serviceMap.put(serviceKey, serviceBean);
        log.info("Adding com.dzgu.xprc.service, interface: {}, version: {}, bean：{}", interfaceName, version, serviceBean);
    }

    public Object getService(String rpcServiceName) {
        Object service = serviceMap.get(rpcServiceName);
        if (null == service) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND);
        }
        return service;
    }

    public Map<String, Object> getserviceMap() {
        return serviceMap;
    }
}
