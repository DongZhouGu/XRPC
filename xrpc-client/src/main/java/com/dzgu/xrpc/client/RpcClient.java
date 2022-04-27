package com.dzgu.xrpc.client;

import com.dzgu.xrpc.annotation.RpcAutowired;
import com.dzgu.xrpc.client.core.NettyClient;
import com.dzgu.xrpc.client.discover.ServiceDiscovery;
import com.dzgu.xrpc.client.proxy.ObjectProxy;
import com.dzgu.xrpc.extension.ExtensionLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/4/24 19:17
 */
@Slf4j
@Component
public class RpcClient implements ApplicationContextAware, DisposableBean {
    private final ServiceDiscovery serviceDiscovery;


    public RpcClient() {
        this.serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension("zk");
    }

    public static <T, P> T createService(Class<T> interfaceClass, String version) {
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new ObjectProxy<T>(interfaceClass, version)
        );
    }

    public void stop() {
        serviceDiscovery.stop();
        NettyClient.getInstance().stop();
    }

    @Override
    public void destroy() throws Exception {
        this.stop();
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        String[] beanNames = ctx.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean = ctx.getBean(beanName);
            Field[] fields = bean.getClass().getDeclaredFields();
            try {
                for (Field field : fields) {
                    RpcAutowired rpcAutowired = field.getAnnotation(RpcAutowired.class);
                    if (rpcAutowired != null) {
                        String version = rpcAutowired.version();
                        field.setAccessible(true);
                        field.set(bean, createService(field.getType(), version));
                    }
                }
            } catch (Exception e) {
                log.error(e.toString());
            }
        }

    }
}
