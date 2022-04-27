package com.dzgu.xrpc.server;

import com.dzgu.xrpc.annotation.RpcService;
import com.dzgu.xrpc.server.core.NettyServer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @description: 基于Netty实现的RPC服务器
 * @Author： dzgu
 * @Date： 2022/4/22 0:00
 */
@Component
public class RpcServer extends NettyServer implements ApplicationContextAware, InitializingBean, DisposableBean {
    public RpcServer() {
    }

    @Override
    public void destroy() throws Exception {
        super.stop();
    }

    // ApplicationContextAware->Constructor（构造器） > @PostConstruct > # InitializingBean # > init-method
    @Override
    public void afterPropertiesSet() throws Exception {
        super.start();
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        Map<String, Object> serviceBeanMap = ctx.getBeansWithAnnotation(RpcService.class);
        if (serviceBeanMap != null && !serviceBeanMap.isEmpty()) {
            for (Object serviceBean : serviceBeanMap.values()) {
                RpcService xRpcService = serviceBean.getClass().getAnnotation(RpcService.class);
                String interfaceName = xRpcService.value().getName();
                String version = xRpcService.version();
                super.serviceProvider.addService(interfaceName, version, serviceBean);
            }
        }
    }
}
