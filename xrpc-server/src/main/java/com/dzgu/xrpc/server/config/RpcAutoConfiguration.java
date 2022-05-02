package com.dzgu.xrpc.server.config;

import com.dzgu.xrpc.annotation.RpcService;
import com.dzgu.xrpc.extension.ExtensionLoader;
import com.dzgu.xrpc.properties.RpcConfig;
import com.dzgu.xrpc.server.ServiceInjectProcessor;
import com.dzgu.xrpc.server.core.NettyServer;
import com.dzgu.xrpc.server.core.ServiceProvider;
import com.dzgu.xrpc.server.invoke.Invoker;
import com.dzgu.xrpc.server.registry.Registry;
import com.dzgu.xrpc.server.registry.RegistryFactory;
import io.protostuff.Rpc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/5/2 11:24
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(RpcConfig.class)
@ConditionalOnProperty(prefix = "xrpc", name = "enable", havingValue = "true", matchIfMissing = true)
public class RpcAutoConfiguration implements DisposableBean {

    private NettyServer nettyServer;
    private ServiceProvider serviceProvider;
    private Invoker invoker;


    @Bean
    public ServiceProvider serviceProvider() {
        serviceProvider = new ServiceProvider();
        return serviceProvider;
    }

    @Bean
    public Invoker invoker(@Autowired RpcConfig rpcConfig) {
        invoker = ExtensionLoader.getExtensionLoader(Invoker.class).getExtension(rpcConfig.getProxyType());
        return invoker;
    }


    @Bean
    public NettyServer nettyServer(@Autowired RpcConfig rpcConfig) {
        RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getExtension(rpcConfig.getRegister());
        Registry registry = registryFactory.getRegistry(rpcConfig.getRegisterAddress());
        String host = null;
        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.error("occur exception when getHostAddress", e);
        }
        InetSocketAddress inetSocketAddress = new InetSocketAddress(host == null ? "127.0.0.1" : host, rpcConfig.getServerPort());
        nettyServer = new NettyServer();
        nettyServer.setRegistry(registry);
        nettyServer.setInvoker(invoker);
        nettyServer.setServerAddress(inetSocketAddress);
        return nettyServer;
    }

    @Bean
    public ServiceInjectProcessor injectProcessor() {
        ServiceInjectProcessor serviceInjectProcessor = new ServiceInjectProcessor();
        serviceInjectProcessor.setNettyServer(nettyServer);
        serviceInjectProcessor.setServiceProvider(serviceProvider);
        return serviceInjectProcessor;
    }


    @Override
    public void destroy() {
        nettyServer.stop();
    }

}
