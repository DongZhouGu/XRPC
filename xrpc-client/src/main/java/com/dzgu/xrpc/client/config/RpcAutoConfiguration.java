package com.dzgu.xrpc.client.config;

import com.dzgu.xrpc.annotation.RpcAutowired;
import com.dzgu.xrpc.client.core.NettyClient;
import com.dzgu.xrpc.client.discover.ServiceDiscovery;
import com.dzgu.xrpc.client.faultTolerantInvoker.FaultTolerantInvoker;
import com.dzgu.xrpc.client.loadbalance.LoadBalance;
import com.dzgu.xrpc.client.proxy.ProxyFactory;
import com.dzgu.xrpc.client.proxy.ProxyInjectProcessor;
import com.dzgu.xrpc.extension.ExtensionLoader;
import com.dzgu.xrpc.properties.RpcConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import java.lang.reflect.Field;


/**
 * @description: Spring 自动配置类
 * @Author： dzgu
 * @Date： 2022/5/1 10:26
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(RpcConfig.class)
@ConditionalOnProperty(prefix = "xrpc", name = "enable", havingValue = "true", matchIfMissing = true)
public class RpcAutoConfiguration implements DisposableBean {

    private ServiceDiscovery serviceDiscovery;
    private NettyClient nettyClient;
    private ProxyFactory proxyFactory;


    @Bean
    public ServiceDiscovery serviceDiscovery(@Autowired RpcConfig rpcConfig) {
        //serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getAdaptiveExtension();
        serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension("zk");
        serviceDiscovery.setRegisterAddress(rpcConfig.getRegisterAddress());
        return serviceDiscovery;
    }

    @Bean
    public NettyClient nettyClient() {
        nettyClient = new NettyClient();
        return nettyClient;
    }

    @Bean
    public ProxyFactory proxyFactory(@Autowired RpcConfig rpcConfig) {
        LoadBalance loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(rpcConfig.getLoadBalance());
        FaultTolerantInvoker tolerantInvoker = ExtensionLoader.getExtensionLoader(FaultTolerantInvoker.class).getExtension(rpcConfig.getFaultTolerant());

        proxyFactory = new ProxyFactory();
        proxyFactory.setNettyClient(nettyClient)
                .setLoadBalance(loadBalance)
                .setServiceDiscovery(serviceDiscovery)
                .setFaultTolerantInvoker(tolerantInvoker)
                .setRetryTime(rpcConfig.getRetryTimes())
                .setCompress(rpcConfig.getCompress())
                .setSerializer(rpcConfig.getSerializer());
        return proxyFactory;
    }

    @Bean
    public ProxyInjectProcessor injectProcessor(@Autowired RpcConfig rpcConfig) {
        ProxyInjectProcessor proxyInjectProcessor = new ProxyInjectProcessor();
        proxyInjectProcessor.setProxyFactory(proxyFactory);
        return proxyInjectProcessor;
    }


    @Override
    public void destroy() {
        serviceDiscovery.stop();
        nettyClient.stop();
    }

}
