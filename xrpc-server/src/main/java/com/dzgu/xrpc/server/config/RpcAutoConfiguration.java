package com.dzgu.xrpc.server.config;

import com.dzgu.xrpc.extension.ExtensionLoader;
import com.dzgu.xrpc.properties.RpcConfig;
import com.dzgu.xrpc.register.Register;
import com.dzgu.xrpc.register.RegisterFactory;
import com.dzgu.xrpc.server.ServiceInjectProcessor;
import com.dzgu.xrpc.server.core.NettyServer;
import com.dzgu.xrpc.server.core.ServiceRegisterCache;
import com.dzgu.xrpc.server.invoke.Invoker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * @description: 自动配置类
 * @Author： dzgu
 * @Date： 2022/5/2 11:24
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(RpcConfig.class)
@ConditionalOnProperty(prefix = "xrpc", name = "enable", havingValue = "true", matchIfMissing = true)
public class RpcAutoConfiguration implements DisposableBean {

    private NettyServer nettyServer;
    private ServiceRegisterCache serviceRegisterCache;
    private Invoker invoker;


    @Bean
    public ServiceRegisterCache serviceProvider() {
        serviceRegisterCache = new ServiceRegisterCache();
        return serviceRegisterCache;
    }

    @Bean
    public Invoker invoker(@Autowired RpcConfig rpcConfig) {
        invoker = ExtensionLoader.getExtensionLoader(Invoker.class).getExtension(rpcConfig.getProxyType());
        return invoker;
    }


    @Bean
    public NettyServer nettyServer(@Autowired RpcConfig rpcConfig) {
        RegisterFactory registerFactory = ExtensionLoader.getExtensionLoader(RegisterFactory.class).getExtension(rpcConfig.getRegister());
        Register register = registerFactory.getRegister(rpcConfig.getRegisterAddress());
        String host = null;
        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.error("occur exception when getHostAddress", e);
        }
        InetSocketAddress inetSocketAddress = new InetSocketAddress(host == null ? "127.0.0.1" : host, rpcConfig.getServerPort());
        nettyServer = new NettyServer();
        nettyServer.setRegister(register);
        nettyServer.setInvoker(invoker);
        nettyServer.setServerAddress(inetSocketAddress);
        return nettyServer;
    }

    @Bean
    public ServiceInjectProcessor injectProcessor() {
        ServiceInjectProcessor serviceInjectProcessor = new ServiceInjectProcessor();
        serviceInjectProcessor.setNettyServer(nettyServer);
        serviceInjectProcessor.setServiceRegisterCache(serviceRegisterCache);
        return serviceInjectProcessor;
    }


    @Override
    public void destroy() {
        nettyServer.stop();
    }
}
