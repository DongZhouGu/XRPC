# 七、集成Spring与SpringBoot

> 从零实现一个轻量级RPC框架-系列文章
> Github: [https://github.com/DongZhouGu/XRpc](https://github.com/DongZhouGu/XRpc) 

## 前言
SpringBoot 最强大的功能就是把我们常用的场景抽取成了一个个starter（场景启动器），我们通过引入springboot 为我提供的这些场景启动器，我们再进行少量的配置就能使用相应的功能。
因此对于实现的XRPC同样需要制作starter，并将相关配置和bean加载交由Spring来管理，最后通过Maven发布

## 自动配置原理

- 首先，SpringBoot 在启动时会去依赖的starter包中寻找 resources/META-INF/spring.factories 文件，然后根据文件中配置的Jar包去扫描项目所依赖的Jar包，这类似于 Java 的 SPI 机制。
- 第二步，根据 spring.factories配置加载AutoConfigure类。
- 最后，根据 @Conditional注解的条件，进行自动配置并将Bean注入Spring Context 上下文当中。

## POM依赖
```java
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.5.4</version>
</parent>
<dependencies>
     <!-- SpringBoot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
    </dependency>
</dependencies>

```
## XRPC-Client集成SpringBoot
### 1. 提供RpcConfig
编写配置类，这样就可以在SpringBoot的配置中配置xrpc的参数
```java
@Data
@ConfigurationProperties(prefix = "xrpc")
public class RpcConfig {
    /**
     * 是否启用rpc 默认启用
     */
    private boolean enable = true;

    /**
     * 注册中心地址
     */
    private String registerAddress = "127.0.0.1:2181";

    /**
     * 注册中心
     */
    private String register = "zookeeper";


    /**
     * 服务暴露端口
     */
    private Integer serverPort = 9999;

    /**
     * 序列化类型
     */
    private String serializer = "kryo";

    /**
     * 压缩算法
     */
    private String compress = "gzip";

    /**
     * 负载均衡算法
     */
    private String loadBalance = "random";

    /**
     * 容错策略
     */
    private String faultTolerant = "retry";

    /**
     * 重试次数，只有容错策略是 'retry' 的时候才有效
     */
    private Integer retryTimes = 3;


    /**
     * 服务代理类型 reflect：
     */
    private String proxyType = "cglib";
}
```
### 
### 2. 编写RpcAutoConfiguration

```java
@Slf4j
@Configuration
@EnableConfigurationProperties(RpcConfig.class)
@ConditionalOnProperty(prefix = "xrpc", name = "enable", havingValue = "true", matchIfMissing = true)
public class RpcAutoConfiguration implements DisposableBean {

    private Register register;
    private NettyClient nettyClient;
    private ProxyFactory proxyFactory;


    @Bean
    public Register serviceDiscovery(@Autowired RpcConfig rpcConfig) {
        RegisterFactory registerFactory = ExtensionLoader.getExtensionLoader(RegisterFactory.class).getExtension(rpcConfig.getRegister());
        register = registerFactory.getRegister(rpcConfig.getRegisterAddress());
        return register;
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
                .setRegister(register)
                .setFaultTolerantInvoker(tolerantInvoker)
                .setRetryTime(rpcConfig.getRetryTimes())
                .setCompress(rpcConfig.getCompress())
                .setSerializer(rpcConfig.getSerializer());
        return proxyFactory;
    }

    @Bean
    public ProxyInjectProcessor injectProcessor() {
        ProxyInjectProcessor proxyInjectProcessor = new ProxyInjectProcessor();
        proxyInjectProcessor.setProxyFactory(proxyFactory);
        return proxyInjectProcessor;
    }


    @Override
    public void destroy() {
        register.stop();
        nettyClient.stop();
    }

}
```
在SpringBoot启动进行自动装载时，根据RpcConfig中的参数进行SPI扩展类的注入，比如负载均衡和容错策略的选择。最后我们需要使用ProxyFactory来获得远程服务调用的代理类，但是ProxyFactory还依赖了其他类，（可能是更复杂的关联），所以当我们去使用这个类做事情时发现包空指针错误，这是因为我们这个类有可能已经初始化完成，但是引用的其他类不一定初始化完成，所以发生了空指针错误。
为了解决这个问题，我们还注入了ProxyInjectProcessor，这个类中的主要作用就是等待Spring装载完成后，将需要远程调用的方法使用动态代理类替换。具体是继承spring的ApplicationListener监听，并监控ContextRefreshedEvent事件
```java
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
```

### 3. 编写spring.factories
最后，编写spring.factories，让Springboot自动装载的时候去加载我们的AutoConfiuration
```java
org.springframework.boot.autoconfigure.EnableAutoConfiguration=com.dzgu.xrpc.client.config.RpcAutoConfiguration
```

## XRPC-Server集成SpringBoot

### 1. 提供RpcConfig
和上一节一样，略过

### 2. 编写RpcAutoConfiguration
```java
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
```
一样的，我们需要在所有bean装载完之后，将服务加载到ServiceRegisterCache缓存中，同时将nettyServer启动，并根绝缓存一次性注册到注册中心

```java
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
```
### 3. 编写spring.factories
最后，编写spring.factories，让Springboot自动装载的时候去加载我们的AutoConfiuration
```java
org.springframework.boot.autoconfigure.EnableAutoConfiguration=com.dzgu.xrpc.server.config.RpcAutoConfiguration
```
## 









