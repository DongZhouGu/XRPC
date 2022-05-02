package com.dzgu.xrpc.server;

import com.dzgu.xrpc.annotation.RpcService;
import com.dzgu.xrpc.server.core.NettyServer;
import com.dzgu.xrpc.server.core.ServiceRegisterCache;
import lombok.Setter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Map;
import java.util.Objects;

/**
 * @description: Spring 启动完毕 启动netty 并注册服务
 * @Author： dzgu
 * @Date： 2022/5/2 17:36
 */
@Setter
public class ServiceInjectProcessor implements ApplicationListener<ContextRefreshedEvent> {
    private NettyServer nettyServer;
    private ServiceRegisterCache serviceRegisterCache;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        // Spring 启动完毕会受到Event
        if (Objects.isNull(contextRefreshedEvent.getApplicationContext().getParent())) {
            ApplicationContext context = contextRefreshedEvent.getApplicationContext();
            Map<String, Object> serviceBeanMap = context.getBeansWithAnnotation(RpcService.class);
            if (serviceBeanMap != null && !serviceBeanMap.isEmpty()) {
                for (Object serviceBean : serviceBeanMap.values()) {
                    RpcService rpcService = serviceBean.getClass().getAnnotation(RpcService.class);
                    String interfaceName = rpcService.value().getName();
                    String version = rpcService.version();
                    serviceRegisterCache.addService(interfaceName, version, serviceBean);

                }
            }
            nettyServer.setServiceRegisterCache(serviceRegisterCache);
            nettyServer.start();
        }
    }
}
