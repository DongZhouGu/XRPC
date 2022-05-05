# XRPC-实现轻量级RPC框架

![GitHub repo size](https://img.shields.io/github/repo-size/DongZhouGu/XRPC?style=for-the-badge)
![GitHub stars](https://img.shields.io/github/stars/DongZhouGu/XRPC?style=for-the-badge)
![GitHub forks](https://img.shields.io/github/forks/DongZhouGu/XRPC?style=for-the-badge)
![GitHub commit activity](https://img.shields.io/github/commit-activity/m/DongZhouGu/XRPC?style=for-the-badge)
![Bitbucket  issues](https://img.shields.io/github/issues-closed/DongZhouGu/XRPC?style=for-the-badge)

<p align="left">
<a href="https://github.com/DongZhouGu/XRPC" target="_blank">
	<img src="https://cdn.jsdelivr.net/gh/dongzhougu/XRPC/docs/images/logo.jpg#pic_center" width="600"/>
</a>
</p>


## 介绍

> 为了更深入的学习RPC的原理与实现过程，从零实现了一个简易的可拓展RPC项目。
>
> 技术点包括：网络通信框架Netty、长连接复用、TCP粘包 / 拆包、心跳保活、服务注册与发现（Zookeeper、Nacos）、Java基础（注解、反射、多线程、Future、SPI 、动态代理）、自定义传输协议、多种序列化（ProtoBuf / Kyro / Hessian）、Gzip压缩、多种负载均衡算法（轮询、随机、一致性哈希）、客户端同步 / 异步调用，集成SpringBoot开箱即用
>
> :star:在学习过程中，我也将重点整理为了博客，如果觉得有用，请点个star 吧！感谢！！
>
> :triangular_flag_on_post: 本人能力有限，如有错误和改进欢迎提交PR



## **:books: 文章列表：**

[零、如何实现一个轻量级RPC框架？](https://dongzhougu.github.io/XRPC/#/./零、如何实现一个轻量级RPC框架？?id=零、如何实现一个轻量级rpc框架？)

[一、如何用Netty实现高性能网络通信？](https://dongzhougu.github.io/XRPC/#/./一、如何用Netty实现高性能网络通信？?id=一、如何用netty实现高性能网络通信？)

[二、网络传输高效序列化协议与实现](https://dongzhougu.github.io/XRPC/#/./二、网络传输高效序列化协议与实现?id=二、网络传输高效序列化协议与实现)

[三、服务注册与发现](https://dongzhougu.github.io/XRPC/#/./三、服务注册与发现?id=三、服务注册与发现)

[四、采用动态代理去无感调用远程服务](https://dongzhougu.github.io/XRPC/#/./四、采用动态代理去无感调用远程服务?id=四、采用动态代理去无感调用远程服务)

[五、使用SPI实现可插拔扩展设计](https://dongzhougu.github.io/XRPC/#/./五、使用SPI实现可插拔扩展设计?id=五、使用spi实现可插拔扩展设计)

[六、去调用哪个服务器呢？负载均衡策略](https://dongzhougu.github.io/XRPC/#/./六、去调用哪个服务器呢？负载均衡策略?id=六、去调用哪个服务器呢？负载均衡策略)

[七、集成Spring与SpringBoot](https://dongzhougu.github.io/XRPC/#/./七、集成Spring与SpringBoot?id=七、集成spring与springboot)



## 🔨 实现要点

- [x] 基于NIO的Netty网络通讯，实现Channel复用、心跳保活
- [x] 支持ProtoBuf、Kryo、Hessian2序列化，反序列化，经测试Kryo效率最高，默认Kyro
- [x] 支持Gzip压缩，可在配置文件配置是否启用包压缩，已经压缩算法，减少数据包的大小。
- [x] 支持Zookeeper和Nacos的服务注册发现，启动后将服务信息发布到注册中心，客户端发现并监听服务信息。
- [x] 客户端实现了基于轮询、随机和一致性哈希负载均衡算法，快速失败和重试的容错策略
- [x] 自定义RpcFuture，客户端支持同步和异步调用，设置回调方法，返回调用响应后执行回调。
- [x] 基于SPI的模块化管理，更加方便扩展模块，集成Spring通过注解注册服务，SpringBoot自动装载配置
- [ ] 动态代理使用Javassist 生成代码，直接调用
- [ ] 支持Eureka、Consul等注册中心
- [ ] 调用鉴权、服务监控中心
- [ ] 编写更完整的测试




## 💻 项目目录

````
以下是重要的包的简介：
```
|- docs：博文Markdown源文件以及绘图draw.io文件

|- xrpc-client：RPC客户端核心
  |- async: 实现了RpcFuture,完成同步、异步回调
  |- config: SpringBoot 自动配置类
  |- core: Netty 客户端核心逻辑，Channel复用，心跳保活
  |- faultTolerantInvoker: 容错策略
  |- loadbalance: 负载均衡算法
  |- proxy: 动态代理类， 实现无感调用
  
|- xrpc-common: RPC抽取出来的通用模块
  |- annotation：自定义的注解，例如 @RpcService(服务提供)、@RpcAutowired(服务引用)
  |- codec: Netty编解码、TCP粘包、拆包
  |- compress: 网络传输过程中的压缩算法
  |- dto: 网络传输中的RpcMessage,Request,Response
  |- extension: 增强版JDK SPI
  |- proterties: SpringBoot的配置Config
  |- registry: 注册中心，例如 Zookeeper、Nacos 注册中心
  |- serializer: 序列化算法实现
  
|- xrpc-server: RPC服务端核心
  |- core: Netty 服务端逻辑，注册服务，接受请求
  |- invoke: 反射调用请求的方法，实现了jdk和cglib
  
|- xrpc-test-client: 样例demo-客户端 
|- xrpc-test-server: 样例demo-服务端
```
````



## 🚀 主要特性

下面为使用draw.io绘制的图，源文件位于https://github.com/DongZhouGu/XRPC/blob/master/docs/images/rpc.drawio，可供参考

### RPC调用过程

![image-20220505133524450](E:\Typora图片\image-20220505133524450.png)

### Netty服务端pipline

![img](https://cdn.nlark.com/yuque/0/2022/png/1164521/1651226155922-921d33a5-63a7-43cc-955b-5161497538e5.png)

### 传输协议

![img](https://cdn.nlark.com/yuque/0/2022/png/1164521/1651231638890-31866612-2803-4951-a68f-d2ce5d0414c1.png)

### RPC-Client逻辑

![img](https://cdn.nlark.com/yuque/0/2022/png/1164521/1651244325781-e8658c69-83a8-4a8c-a4f9-9e57b44820fd.png)

### 同步调用逻辑

![img](https://cdn.nlark.com/yuque/0/2022/png/1164521/1651247120029-4999acb6-a108-4367-b27b-7e91713e8139.png)

### 异步调用逻辑

![img](https://cdn.nlark.com/yuque/0/2022/png/1164521/1651643941142-b1ffeaaa-7a5e-4869-b0c8-1d2cfdc01a19.png)



## :art: 使用方式

1. 克隆本项目到本地Maven install。
2. 添加maven依赖到你的`SpringBoot`项目中。

```
<!--  客户端      -->
 <dependency>
     <groupId>com.dzgu.xrpc</groupId>
     <artifactId>xrpc-client</artifactId>
     <version>1.0-SNAPSHOT</version>
 </dependency>
 
 <!--  服务端      -->
 <dependency>
     <groupId>com.dzgu.xrpc</groupId>
     <artifactId>xrpc-server</artifactId>
     <version>1.0-SNAPSHOT</version>
 </dependency>
```

3. 默认配置项在`RpcConfig`类中，可以通过`application.properties`来覆盖需要修改的配置项。

```yml
xrpc:
  # 是否启用rpc，默认启用
  enable: true
  # RPC服务端口
  serverPort: 18866
  # 注册中心，默认zookeeper
  register: zookeeper
  # 注册中心地址
  registerAddress: 127.0.01:8848
  # 序列化算没法，默认kryo
  serializer: kryo
  # 压缩算法，默认gzip
  compress: gzip
  # 负载均衡算法，默认random
  load-balance: random
  # 容错策略，默认retry
  retry: retry
  # 重试次数，只有容错策略是retry时才有效
  retry-times: 3
```

4. 启动注册中心



### 服务端

1. 定义服务接口

```java
public interface HelloService {
    String hello(Hello hello);
}
```

2. 实现服务接口，并通过`@RpcService`注册服务

```java
@RpcService(value = HelloService.class, version = "1.0")
public class HelloServiceImp1 implements HelloService{
    static {
        System.out.println("HelloServiceImpl1被创建");
    }

    @Override
    public String hello(Hello hello) {
        log.info("HelloServiceImpl收到: {}.", hello.getMessage());
        String result = "Hello description is " + hello.getDescription();
        log.info("HelloServiceImpl返回: {}.", result);
        // 模拟耗时操作
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }
}

```



### 客户端

#### 同步调用

1. 使用 `@RpcAutowired` 注解调用远程服务
2. 调用接口方法

```java
public class HelloController {
    @RpcAutowired(version = "1.0")
    private HelloService helloService;

    public void test() throws InterruptedException {
        for (int i = 0; i < 1000; i++) {
            System.out.println(i+"----sync:"+helloService.hello(new Hello("hello", "hello sync")));
            Thread.sleep(1000);
        }
    }

}

```

#### 异步调用

1. 使用 `@RpcAutowired` 注解调用远程服务，并且将注解的 `isAsync` 置为 `ture`
2. 调用接口方法，并立即为`RpcContext` 上下文设置回调函数（集成 `ResponseCallback` 抽象类）

```java
public class HelloController {
    @RpcAutowired(version = "1.0",isAsync = true)
    private HelloService helloServiceAsync;
    
    public void testAsync() throws InterruptedException {
        for (int i = 0; i < 1000; i++) {
            helloServiceAsync.hello(new Hello("hello", "hello async"));
            RpcContext.setCallback(new ResponseCallback() {
                @Override
                public void callBack(RpcResponse<Object> result) {
                    System.out.println("----Async--requetId:"+ result.getRequestId()+"--data:"+result.getData());
                }
                @Override
                public void onException(RpcResponse<Object> result, Exception e) {

                }
            });
            Thread.sleep(1000);
        }
    }
}

```




## ☕  鸣谢

感谢以下项目，我们从中得到了很大的帮助：

- https://my.oschina.net/huangyong/blog/361751
- https://github.com/luxiaoxun/NettyRpc
- https://github.com/Snailclimb/guide-rpc-framework

