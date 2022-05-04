> 从零实现一个轻量级RPC框架-系列文章
> Github: [https://github.com/DongZhouGu/XRpc](https://github.com/DongZhouGu/XRpc) 



## 什么是RPC?

RPC，即 Remote Procedure Call（远程过程调用）, 在计算机科学中已经存在了超过四十年时间，由于微服务风潮带来的热度，RPC技术如今依旧被开发人员关注。RPC出现的最初目的，就是为了让计算机能够**跟调用本地方法一样去调用远程方法。**RPC 可基于 HTTP 或 TCP 协议，Web Service 就是基于 HTTP 协议的 RPC，它具有良好的跨平台性，但其性能却不如基于 TCP 协议的 RPC。



## 为什么需要RPC?

- 单一应用下，逻辑简单，用户较少，流量不大，所有的服务都在单体下，这种情况下并不需要RPC。
- 当我们的系统访问量增大、业务增多时，我们会发现一台单机运行此系统已经无法承受。此时，我们可以将业务拆分成几个互不关联的应用，分别部署在各自机器上，以划清逻辑并减小压力。此时，我们也可以不需要RPC，因为应用之间是互不关联的。
- 发现一些公共的业务逻辑需要抽离出来，组成独立的service应用部署在一些机器上，其他的服务都与service应用交互，这时，就需要高效的应用间的通讯手段来完成远程服务调用。



## 构建一个RPC需要什么？

三方面会直接影响 RPC 的性能，**一是传输方式，二是序列化，三是IO。**

- TCP 是传输层协议，HTTP 是应用层协议，而传输层较应用层更加底层，在数据传输方面，越底层越快，HTTP还封装了冗余的头部信息，因此，在一般情况下，TCP 一定比 HTTP 快。
- 就序列化而言，Java 提供了默认的序列化方式，但在高并发的情况下，这种方式将会带来一些性能上的瓶颈，于是市面上出现了一系列优秀的序列化框架，比如：Protobuf、Kryo、Hessian、Jackson 等，它们可以取代 Java 默认的序列化，从而提供更高效的性能。
- 为了支持高并发，传统的阻塞式 IO 显然不太合适，因此我们需要异步的 IO，即 NIO。Java提供了NIO的解决方案，但实现繁琐，相比之下，Netty作为一个高性能、可拓展的异步事件驱动的通信框架，大大简化了网络编程。

同时，我们在调用远程服务时，如何知道远程服务到底在分布式下的哪一台机器上呢？因此，我们还需要**服务注册与发现功能**，让客户端来自动发现当前可用的服务，并调用这些服务。这需要一种服务注册表（Service Registry）的组件，让它来注册分布式环境下所有的服务地址（包括：主机名与端口号）。

![img](https://cdn.nlark.com/yuque/0/2022/png/1164521/1651118558445-f03c704c-04ca-4017-975c-0e09b046ad70.png)

## RPC的调用流程

在一次 RPC 调用过程中

- 客户端远程调用服务时，使用动态代理，对调用过程增强‘

- 客户端首先会将调用的类名、方法名、参数名、参数值等信息，序列化成二进制流；
- 然后客户端将二进制流，通过网络（注册中心拿到服务端地址）发送给服务端；
- 服务端接收到二进制流之后，将它反序列化，得到需要调用的类名、方法名、参数名和参数值，再通过反射方式，调用对应的方法得到返回值；
- 服务端将返回值序列化，再通过网络发送给客户端；
- 客户端对结果反序列化之后，就可以得到调用的结果了。

![img](https://cdn.nlark.com/yuque/0/2022/png/1164521/1651121917231-3717ea5f-be6a-4122-bd7f-b8803f2e15d1.png)

## 常用的的RPC框架

目前常用的RPC框架如下：

- **Thrift：**thrift是一个软件框架，用来进行可扩展且跨语言的服务的开发。它结合了功能强大的软件堆栈和代码生成引擎，以构建在 C++, Java, Python, PHP, Ruby, Erlang, Perl, Haskell, C#, Cocoa, JavaScript, Node.js, Smalltalk, and OCaml 这些编程语言间无缝结合的、高效的服务。

- **gRPC:**  一开始由 google 开发，是一款语言中立、平台中立、开源的远程过程调用(RPC)系统，采用HTTP2协议和ProtoBuf。
- **Dubbo：**Dubbo是一个分布式服务框架，以及SOA治理方案。其功能主要包括：高性能NIO通讯及多协议集成，服务动态寻址与路由，软负载均衡与容错，依赖分析与降级等。 Dubbo是阿里巴巴内部的SOA服务化治理方案的核心框架，Dubbo自2011年开源后，已被许多非阿里系公司使用。
- **Spring Cloud：** 基于 Spring Boot，基于HTTP协议的REST接口调用，为微服务体系开发中的架构问题，提供了一整套的解决方案——服务注册与发现，服务消费，服务保护与熔断，网关，分布式调用追踪，分布式配置管理等。

## Dubbo使用案例

官网：https://dubbo.apache.org/zh/docs/quick-start/

![img](https://cdn.nlark.com/yuque/0/2022/png/1164521/1651151777712-9460d64a-8d34-432b-a807-54680604efd7.png)

- 服务提供者（Provider) : 暴露服务的服务提供方，服务提供者在启动时，向注册中心注册自己提供的服务。
- 服务消费者(Consumer) : 调用远程服务的服务消费方，服务消费者在启动时，向注册中心订阅自己所需的服务，服务消费者，从提供者地址列表中，基于软负载均衡算法，选一台提供者进行调用，如果调用失败，再选另一台调用。
- 注册中心(Registry) : 注册中心返回服务提供者地址列表给消费者，如果有变更，注册中心将基于长连接推送变更数据给消费者
- 监控中心(Monitor) : 服务消费者和提供者，在内存中累计调用次数和调用时间，定时每分钟发送一次统计数据到监控中心

### 定义服务接口

DemoService.java

```java
package org.apache.dubbo.samples.basic.api;
 
 public interface DemoService {
     String sayHello(String name);
 }
 
```

### 在服务提供方实现接口

DemoServiceImpl.java

```java
public class DemoServiceImpl implements DemoService {
     @Override
     public String sayHello(String name) {
         System.out.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] Hello " + name +
                 ", request from consumer: " + RpcContext.getContext().getRemoteAddress());
         return "Hello " + name + ", response from provider: " + RpcContext.getContext().getLocalAddress();
     }
 }
 
```

## 服务消费者

### 引用远程服务

consumer.xml：

```java
<dubbo:reference id="demoService" check="true" interface="org.apache.dubbo.samples.basic.api.DemoService"/>
```

### 加载Spring配置，并调用远程服务 

Consumer.java

```java
public static void main(String[] args) {
     ...
     DemoService demoService = (DemoService) context.getBean("demoService");
     String hello = demoService.sayHello("world");
     System.out.println(hello);
 }
 
```



可以发现，我们需要抽取公共的service-api，在Provider也就是RPC Server提供方具体实现，在Consumer也就是RPC Client中直接调用，此时的demoService已经是被代理过的对象，当调用demoService.sayHello("world")时，会去注册中心拿到RPC Server的IP+Port，然后通过网络通信去RPC Server拿到方法调用的结果。



## 所以我们该如何实现RPC?



根据上面的介绍，我们已经大概了解RPC是什么以及RPC中涉及到的点。下面我们通过一些问题来考虑我们从零实现一个RPC框架到底需要做些什么。

#### 1. 如何获取可用的远程服务器

换句话说，也就是服务注册与发现，可以使用Zookeeper 作为注册中心， ZooKeeper 将数据保存在内存中，性能很高。 在读多写少的场景中尤其适用，因为写操作会导致所有的服务器间同步状态。服务注册与发现是典型的读多写少的协调服务场景。 Zookeeper 是一个典型的CP系统，在服务选举或者集群半数机器宕机时是不可用状态，相对于服务发现中主流的AP系统来说，可用性稍低。除此之外，还可以使用Nacos、Consul、Eureka、Redis等，需要提供切换及用户自定义注册中心的功能。

#### 2. 如何表示数据

也就是序列化、反序列化。在网络中，所有的数据都将会被转化为字节进行传送，所以为了能够使参数对象在网络中进行传输，需要对这些参数进行序列化和反序列化操作。

序列化：把对象转换为字节序列的过程称为对象的序列化，也就是编码的过程。 

 反序列化：把字节序列恢复为对象的过程称为对象的反序列化，也就是解码的过程。 

目前比较高效的开源序列化框架：如Kryo、Hessian、FastJson和Protobuf等，需要提供切换及用户自定义序列化算法的功能。

#### 3. 如何传递数据

出于并发性能的考虑，传统的阻塞式 IO 显然不太合适，因此我们需要异步的 IO，即 NIO。 Java 提供了 NIO 的解决方案，Java 7 也提供了更优秀的 NIO.2 支持。 可以选择Netty或者MINA来解决NIO数据传输的问题。

#### 4. 服务端如何确定并调用目标方法

代理，用于客户端代理，客户端调用服务接口，实际上是一个网络请求的过程,屏蔽程方法调用的底层细节。可以使用JDK提供的原生的动态代理机制，也可以使用开源的：CGLib代理，Javassist字节码生成技术。 



### 实现要点

- 使用基于Netty框架的NIO，自定义通信协议，解决TCP粘包/拆包，实现Channel复用、心跳保活

（对应系列文章：一、如何用Netty实现异步网络通信？ ）

- 支持Kyro、Hessian、Protostuff多种序列化协议和gzip消息压缩方法，引入SPI扩展功能，Spring自定义配置（对应系列文章：二、网络传输高效序列化协议与实现 ）

- 使用 Zookeeper 管理相关服务地址信息，实现服务注册与发现（对应系列文章：三、Zookeeper实现服务注册与发现）

- 支持可配置的服务端代理模式，可选反射调用、字节码增强。（对应系列文章：四、采用动态代理去无感调用远程服务）
- 支持客户端异步回调、负载均衡、集群容错等高级特性，集成Spring并制作starter发布 （对应系列文章：扩展：。。。）







### 实现流程











![img](https://cdn.nlark.com/yuque/0/2022/png/1164521/1651156250260-edee5e6b-6f61-474f-8db3-e9d2c7122a59.png)















![img](https://cdn.nlark.com/yuque/0/2022/png/1164521/1651147019248-72fa42e7-25d2-4500-aceb-273b0b9cbcf0.png)

![img](https://cdn.nlark.com/yuque/0/2022/png/1164521/1651134156830-1cce2028-e93a-47d5-899c-51c846b84ca6.png)

![img](https://cdn.nlark.com/yuque/0/2022/png/1164521/1651122574216-ac24d764-36ba-4abd-9b6a-0184f54deb8a.png)