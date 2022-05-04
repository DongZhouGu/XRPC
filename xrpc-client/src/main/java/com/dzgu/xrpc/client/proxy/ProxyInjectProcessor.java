package com.dzgu.xrpc.client.proxy;

import com.dzgu.xrpc.annotation.RpcAutowired;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.lang.reflect.Field;
import java.util.Objects;

/**
 * @description: Spring 启动完毕再替换bean为其代理方法
 * @Author： dzgu
 * @Date： 2022/5/1 23:17
 */
@Slf4j
@Setter
public class ProxyInjectProcessor implements ApplicationListener<ContextRefreshedEvent> {
    private ProxyFactory proxyFactory;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        // Spring 启动完毕会受到Event
        if (Objects.isNull(contextRefreshedEvent.getApplicationContext().getParent())) {
            ApplicationContext context = contextRefreshedEvent.getApplicationContext();
            String[] names = context.getBeanDefinitionNames();
            for (String name : names) {
                Object bean = context.getBean(name);
                Field[] fields = bean.getClass().getDeclaredFields();
                for (Field field : fields) {
                    RpcAutowired rpcAutowired = field.getAnnotation(RpcAutowired.class);
                    if (rpcAutowired != null) {
                        String version = rpcAutowired.version();
                        boolean isAsync = rpcAutowired.isAsync();
                        field.setAccessible(true);
                        try {
                            field.set(bean, proxyFactory.getProxy(field.getType(), version,isAsync));
                        } catch (IllegalAccessException e) {
                            log.error("field.set error. bean={}, field={}", bean.getClass(), field.getName(), e);
                        }
                    }
                }
            }
        }
    }
}
