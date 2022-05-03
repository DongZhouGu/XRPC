package com.dzgu.xrpc.register.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.pojo.Instance;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description: Nacos 工具类
 */
@Slf4j
public class NacosUtils {
    private static final Map<String, List<String>> SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>();
    private static final Set<String> REGISTERED_PATH_SET = ConcurrentHashMap.newKeySet();
    private static NamingService namingService;

    public static NamingService getNacosClient(String address) {
        try {
            namingService = NamingFactory.createNamingService(address);
        } catch (NacosException e) {
            log.error("connect to nacos [{}] fail", address);
        }
        return namingService;
    }

    /**
     * 根据服务名称和地址注册服务
     *
     * @param rpcServiceName 服务名称
     * @param address        服务地址
     * @throws NacosException
     */
    public static void registerInstance(NamingService namingService, String rpcServiceName, InetSocketAddress address) throws NacosException {
        namingService.registerInstance(rpcServiceName, address.getHostName(), address.getPort());
        REGISTERED_PATH_SET.add(rpcServiceName);
    }

    /**
     * 根绝服务名称获取服务的所有实例
     *
     * @param serviceName 服务名称
     * @return 服务实例集合
     * @throws NacosException
     */
    public static List<String> getAllInstance(NamingService namingService, String serviceName) throws NacosException {
        if (SERVICE_ADDRESS_MAP.containsKey(serviceName)) {
            return SERVICE_ADDRESS_MAP.get(serviceName);
        }
        List<Instance> allInstances = namingService.getAllInstances(serviceName);
        List<String> addressList = new ArrayList<>();
        for (Instance instance : allInstances) {
            addressList.add(instance.getIp() + ":" + instance.getPort());
        }
        SERVICE_ADDRESS_MAP.put(serviceName, addressList);
        registerWatcher(namingService, serviceName);
        return addressList;
    }

    /**
     * 根据服务地址清理 Nacos
     *
     * @param address 服务地址
     */
    public static void clearRegistry(NamingService namingService, InetSocketAddress address) {
        String host = address.getHostName();
        int port = address.getPort();
        REGISTERED_PATH_SET.stream().parallel().forEach(serviceName -> {
            try {
                namingService.deregisterInstance(serviceName, host, port);
            } catch (NacosException e) {
                log.error("clear registry for service [{}] fail", serviceName, e);
            }
        });
        log.info("All registered services on the server are cleared:[{}]", REGISTERED_PATH_SET.toString());

    }

    /**
     * 监听服务，更改时刷缓存
     */
    @SneakyThrows
    private static void registerWatcher(NamingService namingService, String serviceName) {
        namingService.subscribe(serviceName, new EventListener() {
            @SneakyThrows
            @Override
            public void onEvent(Event event) {
                List<Instance> allInstances = namingService.getAllInstances(serviceName);
                List<String> addressList = new ArrayList<>();
                for (Instance instance : allInstances) {
                    addressList.add(instance.getIp() + ":" + instance.getPort());
                }
                SERVICE_ADDRESS_MAP.put(serviceName, addressList);
            }
        });

    }
}
