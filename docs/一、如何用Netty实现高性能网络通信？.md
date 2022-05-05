# 一、如何用Netty实现高性能网络通信？

> 从零实现一个轻量级RPC框架-系列文章
> Github: [https://github.com/DongZhouGu/XRpc](https://github.com/DongZhouGu/XRpc) 

## 前言
既然要调用远程方法，必然需要网络通信，通过网络来传递要调用的目标类信息及相关方法参数，和返回的调用结果。
网络传输具体实现可以使用 Socket 、NIO、Netty：

- Socket：Java 中最原始、最基础的网络通信方式。但是Socket 是阻塞 IO、性能低并且功能单一
- NIO：同步非阻塞的 I/O 模型，Java原生实现，但是用它来进行网络编程太过繁琐
- Netty：基于 NIO 的 client-server(客户端服务器)框架，设计了一套优秀的Reactor反应器模式使用它可以快速简单地开发网络应用程序。极大地简化并简化了 TCP 和 UDP 套接字服务器等网络编程, 并且性能以及安全性等很多方面甚至都要更好。支持多种协议如 FTP，SMTP，HTTP 以及各种二进制和基于文本的传统协议。
## Reactor反应器模式
Reactor 就是基于NIO中实现多路复用的一种模式，
### 单Reactor单线程模型
![image.png](https://cdn.nlark.com/yuque/0/2022/png/1164521/1651239494041-429b359b-3ebf-4043-88f4-d4f043c98ac9.png#clientId=u094a13e3-2a73-4&crop=0&crop=0&crop=1&crop=1&from=paste&id=uf1c9ab7e&margin=%5Bobject%20Object%5D&name=image.png&originHeight=479&originWidth=743&originalType=url&ratio=1&rotation=0&showTitle=false&size=32668&status=done&style=none&taskId=u8086378d-4da0-45b8-946d-333c8f1a9b0&title=)

- 服务端的Reactor是一个线程，该线程会启动事件循环，并使用Selector实现IO多路复用，通过acceptor来获取并注册新的连接
- 客户端发起连接请求，Reactor监听到了这个时间，并分发给对应的acceptor去处理，acceptor负责建立到这个客户端的SocketChannel，Reactor使用Selector系统调用进行事件监听
- 当客户端有IO读写事件时，则分发给对应的handler进行处理

单线程Reactor模式中，不仅I/O操作在该Reactor线程上，连非I/O的业务操作也在该线程上进行处理了，这可能会大大延迟I/O请求的响应。所以我们应该将非I/O的业务逻辑操作从Reactor线程上卸载，以此来加速Reactor线程对I/O请求的响应。
### 单Reactor多线程模型
![image.png](https://cdn.nlark.com/yuque/0/2022/png/1164521/1651239500254-92694874-1d18-43a8-9501-a4f49aede460.png#clientId=u094a13e3-2a73-4&crop=0&crop=0&crop=1&crop=1&from=paste&id=uc14ceb57&margin=%5Bobject%20Object%5D&name=image.png&originHeight=459&originWidth=687&originalType=url&ratio=1&rotation=0&showTitle=false&size=33868&status=done&style=none&taskId=u4f1f3f5b-4efc-4916-80f5-289f3db5d2f&title=)
与单线程Reactor模式不同的是，添加了一个工作者线程池，并将非I/O操作从Reactor线程中移出转交给工作者线程池来执行。这样能够提高Reactor线程的I/O响应，不至于因为一些耗时的业务逻辑而延迟对后面I/O请求的处理。
### 多Reactor多线程模型

![image.png](https://cdn.nlark.com/yuque/0/2022/png/1164521/1651239513042-abc8057a-d22b-41d8-83c7-0cea76d96cb2.png#clientId=u094a13e3-2a73-4&crop=0&crop=0&crop=1&crop=1&from=paste&id=uc09103b5&margin=%5Bobject%20Object%5D&name=image.png&originHeight=479&originWidth=827&originalType=url&ratio=1&rotation=0&showTitle=false&size=43560&status=done&style=none&taskId=ueff1da0b-f2bc-4a60-a8a3-874a57218a7&title=)
多Reactor线程模式将“接受客户端的连接请求”和“与该客户端的通信”分在了两个Reactor线程来完成。mainReactor完成接收客户端连接请求的操作，它不负责与客户端的通信，而是将建立好的连接转交给subReactor线程来完成与客户端的通信，这样一来就不会因为read()数据量太大而导致后面的客户端连接请求得不到即时处理的情况。并且多Reactor线程模式在海量的客户端并发请求的情况下，还可以通过实现subReactor线程池来将海量的连接分发给多个subReactor线程，在多核的操作系统中这能大大提升应用的负载和吞吐量。

## 使用Netty来实现XPRC服务端	
```java
@Slf4j
public class NettyServer {
    private Thread thread;
    private InetSocketAddress serverAddress = getServerAddress();

    public void start() {
        thread = new Thread(new Runnable() {
            DefaultEventExecutorGroup serviceHandlerGroup = new DefaultEventExecutorGroup(
                    RuntimeUtil.cpus() * 2,
                    ThreadPoolFactoryUtil.createThreadFactory("service-handler-group", false));

            @Override
            public void run() {
                // 负责服务器通道新连接的IO事件的监听
                NioEventLoopGroup bossGroup = new NioEventLoopGroup();
                // 负责传输通道的IO事件的处理, 无参数的构造函数默认最大可用的CPU处理器数量 的2倍
                NioEventLoopGroup workerGroup = new NioEventLoopGroup();
                try {
                    ServerBootstrap bootstrap = new ServerBootstrap();
                    bootstrap.group(bossGroup, workerGroup)
                            .channel(NioServerSocketChannel.class)
                            // TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。
                            .childOption(ChannelOption.TCP_NODELAY, true)
                            // 是否开启 TCP 底层心跳机制
                            .childOption(ChannelOption.SO_KEEPALIVE, true)
                            //表示系统用于临时存放已完成三次握手的请求的队列的最大长度,如果连接建立频繁，服务器处理创建新连接较慢，可以适当调大这个参数
                            .option(ChannelOption.SO_BACKLOG, 128)
                            .childHandler(new ChannelInitializer<NioSocketChannel>() {
                                @Override
                                protected void initChannel(NioSocketChannel ch) throws Exception {
                                    // 心跳,空闲检测
                                    ch.pipeline().addLast(new IdleStateHandler(15, 0, 0, TimeUnit.SECONDS));
                                    // 处理粘包半包
                                    ch.pipeline().addLast(new Spliter());
                                    ch.pipeline().addLast(new RpcDecoder());
                                    ch.pipeline().addLast(new RpcEncoder());
                                    ch.pipeline().addLast(serviceHandlerGroup, new NettyServerHandler());
                                }
                            });
                    // 绑定端口，同步等待绑定成功
                    //bind操作(对应初始化)是异步的，通过sync改为同步等待初始化的完成，否则立即操作对象(未初始完全)可能会报错
                    ChannelFuture f = bootstrap.bind(serverAddress.getAddress(), serverAddress.getPort()).sync();
                    log.info("Netty Server started on address {}", serverAddress);
                    // 不会立即执行 finally，而阻塞在这里，等待服务端监听端口关闭
                    f.channel().closeFuture().sync();
                } catch (Exception e) {
                    if (e instanceof InterruptedException) {
                        log.info("Rpc server remoting server stop");
                    } else {
                        log.error("Rpc server remoting server error", e);
                    }
                } finally {
                    try {
                        serviceRegistry.unregisterService(serverAddress);
                        workerGroup.shutdownGracefully();
                        bossGroup.shutdownGracefully();
                    } catch (Exception ex) {
                        log.error(ex.getMessage(), ex);
                    }
                }
            }
        });
        thread.start();
    }

    public void stop() {
        // destroy server thread
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }
}

```
### NioEventLoop
Netty的反应器类为：NioEventLoop，首先看到，我们创建了两个NioEventLoopGroup，第一个通 常被称为“包工头”，负责服务器通道新连接的IO事件的监听。第二个 通常被称为“工人”，主要负责传输通道的IO事件的处理。具体来说，一种类型的reactor线程是boss线程组，专门用来接受新的连接，然后封装成channel对象扔给worker线程组；还有一种类型的reactor线程是worker线程组，专门用来处理连接的读写。不管是boos线程还是worker线程，所做的事情均分为以下三个步骤

1. 轮询注册在selector上的IO事件
1. 处理IO事件
1. 执行异步task

对于boos线程来说，第一步轮询出来的基本都是 accept 事件，表示有新的连接，而worker线程轮询出来的基本都是read/write事件，表示网络的读写事件。
![](https://cdn.nlark.com/yuque/0/2022/webp/1164521/1651202492097-1e6d73cc-91fb-4dbe-b509-80543853cd2e.webp#clientId=u38b5cafb-8dd3-4&crop=0&crop=0&crop=1&crop=1&from=paste&id=u2ca7f28d&margin=%5Bobject%20Object%5D&originHeight=714&originWidth=1304&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=ub8c17781-9fb6-43c3-b227-316b201a444&title=)
### ServerBootstrap服务启动类
Bootstrap类是Netty提供的一个便利的工厂类，可以通过它来完成 Netty的客户端或服务器端的Netty组件的组装，以及Netty程序的初始 化。当然，Netty的官方解释是，完全可以不用这个Bootstrap启动器。 但是，一点点去手动创建通道、完成各种设置和启动、并且注册到 EventLoop，这个过程会非常麻烦。通常情况下，还是使用这个便利的 Bootstrap工具类会效率更高。

1. **创建反应器线程组，并赋值给ServerBootstrap启动器实例**

在服务器端，建议设置成两个线程组的工作模式。

2. **设置通道的IO类型**

Netty不止支持Java NIO，也支持阻塞式的OIO（也叫BIO，BlockIO，即阻塞式IO）由于NIO的优势巨大，通 常不会在Netty中使用BIO。
在Netty中，将有接收关系的NioServerSocketChannel和 NioSocketChannel，叫作父子通道。其中，NioServerSocketChannel负 责服务器连接监听和接收，也叫父通道（Parent Channel）。对应于每 一个接收到的NioSocketChannel传输类通道，也叫子通道（Child Channel）。服务端使用b.channel(NioServerSocketChannel.class)来监听

3. **设置传输通道的配置选项**

调用了Bootstrap的option()选项设置方法。对于服务器的Bootstrap而言，这个方法的作用是：给父通道（Parent Channel）设置一些与传输协议相关的选项。如果要给子通道（Child Channel）设置一些通道选项，则需要调用childOption()设置方法。具体的channelOption(): [https://juejin.cn/post/6982470261811445791](https://juejin.cn/post/6982470261811445791)

4. **装配子通道的Pipeline流水线**

每一个通道的子通道，都用一条ChannelPipeline 流水线。它的内部有一个双向的链表。装配流水线的方式是：将业务 处理器ChannelHandler实例加入双向链表中。 装配子通道的Handler流水线调用childHandler()方法，传递一个 ChannelInitializer通道初始化类的实例。
在父通道成功接收一个连接， 并创建成功一个子通道后，就会初始化子通道，这里配置的 ChannelInitializer实例就会被调用。 在ChannelInitializer通道初始化类的实例中，有一个initChannel初 始化方法，在子通道创建后会被执行到，向子通道流水线增加业务处理器。
```java
.childHandler(new ChannelInitializer<NioSocketChannel>() {
    @Override
    protected void initChannel(NioSocketChannel ch) throws Exception {
        // 心跳,空闲检测
        ch.pipeline().addLast(new IdleStateHandler(15, 0, 0, TimeUnit.SECONDS));
        // 处理粘包半包
        ch.pipeline().addLast(new Spliter());
        ch.pipeline().addLast(new RpcDecoder());
        ch.pipeline().addLast(new RpcEncoder());
        ch.pipeline().addLast(serviceHandlerGroup, new NettyServerHandler());
    }
});
```
为什么仅装配子通道的流水线，而不需要装配父通道的流水线 呢？原因是：父通道也就是NioServerSocketChannel连接接受通道，它 的内部业务处理是固定的：接受新连接后，创建子通道，然后初始化 子通道，所以不需要特别的配置。

5. **开始绑定服务器新连接的监听端口**
```java
// 绑定端口，同步等待绑定成功
//bind操作(对应初始化)是异步的，通过sync改为同步等待初始化的完成，否则立即操作对象(未初始完全)可能会报错
ChannelFuture f = bootstrap.bind(serverAddress.getAddress(), serverAddress.getPort()).sync();
log.info("Netty Server started on address {}", serverAddress);
```
bootstrap.bind()方法的功能：返回一个端口绑定Netty的异 步任务channelFuture。在这里，并没有给channelFuture异步任务增加回 调监听器，而是阻塞channelFuture异步任务，直到端口绑定任务执行 完成。

6. **自我阻塞，直到通道关闭**
```java
// 不会立即执行 finally，而阻塞在这里，等待服务端监听端口关闭
f.channel().closeFuture().sync();
```

7. **关闭EventLoopGroup**

关闭Reactor反应器线程组，同时会关闭内部的subReactor子反应 器线程，也会关闭内部的Selector选择器、内部的轮询线程以及负责查 询的所有的子通道。在子通道关闭后，会释放掉底层的资源，如TCP Socket文件描述符等。

至此，Netty服务端已搭建完成，其中，最为重要的是装配到**Pipeline流水线中handler,下面我们具体介绍。**

### Pipline流水线
每条通道内部都有一条流水线pipline来讲Handler装配起来来处理业务。Netty的业务处理器流水线ChannelPipeline是基于**责任链设计模式 **来设计的，内部是一个双向链表结构，能够 支持动态地添加和删除Handler业务处理器。
Handler涉及的环节有：数据包解码、业务处理、目标数据编码、数据包写入通道这几个部分，那么他们在pipline中的添加顺序是怎样的呢？
首先，Handler有入站和出 站两种类型操作

- 入站处理，触发的方向为：自底向上，Netty的内部（如通道）到 ChannelInboundHandler入站处理器。
- 出站处理，触发的方向为：自顶向下，从ChannelOutboundHandler 出站处理器到Netty的内部（如通道）。

 按照这种方向来分，前面数据包解码、业务处理两个环节——属 于入站处理器的工作；后面目标数据编码、把数据包写到通道中两个 环节——属于出站处理器的工作。
入站处理器的流动次序是：从前到后。加在前面的， 执行也在前面；出站流水处理次序为从后向前，最后加入的出 站处理器，反而执行在最前面。这一点和Inbound入站处理次序是相反的。
**需要注意的点**
```java
ch.pipeline().addLast(new InBoundHandlerA());
ch.pipeline().addLast(new OutboundHandlerA());
ch.pipeline().addLast(new InBoundHandlerB());
ch.pipeline().addLast(new OutboundHandlerB());
ch.pipeline().addLast(new InBoundHandlerC());
ch.pipeline().addLast(new OutboundHandlerC());
```
![image.png](https://cdn.nlark.com/yuque/0/2022/png/1164521/1651216115227-f9defdc5-886d-44a1-a50e-61e0c61a7966.png#clientId=u094a13e3-2a73-4&crop=0&crop=0&crop=1&crop=1&from=paste&id=u614b5f98&margin=%5Bobject%20Object%5D&name=image.png&originHeight=724&originWidth=3828&originalType=url&ratio=1&rotation=0&showTitle=false&size=191372&status=done&style=none&taskId=ue0472cfa-7581-4cbc-bbd7-2f6ec5502b7&title=)
针对InBoundHandlerC，处理完消息发送时，

- 当调用ctx.writeAndFlush(new Object())时代表Object从当前的handler流向head节点
- 当调用ctx.channel().writeAndFlush(new Object())时代表Object从tail节点流向head节点。

![image.png](https://cdn.nlark.com/yuque/0/2022/png/1164521/1651216412456-197b6ad3-8008-4654-b305-6d511d1a51f4.png#clientId=u094a13e3-2a73-4&crop=0&crop=0&crop=1&crop=1&from=paste&id=u6c21b4c0&margin=%5Bobject%20Object%5D&name=image.png&originHeight=1444&originWidth=3112&originalType=url&ratio=1&rotation=0&showTitle=false&size=317106&status=done&style=none&taskId=u222e7442-78c1-4ec5-b014-ce3aaefbff5&title=)
针对RPC框架的Netty Server的pipline来说，执行顺序为
![image.png](https://cdn.nlark.com/yuque/0/2022/png/1164521/1651226155922-921d33a5-63a7-43cc-955b-5161497538e5.png#clientId=u765db17b-2c3b-4&crop=0&crop=0&crop=1&crop=1&from=paste&id=u2502c4fb&margin=%5Bobject%20Object%5D&name=image.png&originHeight=844&originWidth=3168&originalType=url&ratio=1&rotation=0&showTitle=false&size=247573&status=done&style=none&taskId=u0076bb5d-9e65-4ff7-b3fa-d4f3fff651e&title=)
### 心跳-空闲检测
![](https://cdn.nlark.com/yuque/0/2022/webp/1164521/1651214594531-e9023b15-12cd-4279-9d69-b223ebbf9ee7.webp#clientId=u094a13e3-2a73-4&crop=0&crop=0&crop=1&crop=1&from=paste&id=ua0ee798b&margin=%5Bobject%20Object%5D&originHeight=1218&originWidth=1240&originalType=url&ratio=1&rotation=0&showTitle=false&status=done&style=none&taskId=u08fc06b3-877c-4492-a611-6c560c34852&title=)
网络应用程序普遍会出现连接假死，连接假死的现象是：在某一端（服务端或者客户端）看来，底层的 TCP 连接已经断开了，但是应用程序并没有捕获到，因此会认为这条连接仍然是存在的，从 TCP 层面来说，只有收到四次握手数据包或者一个 RST 数据包，连接的状态才表示已断开。
连接假死会带来以下两大问题

1. 对于服务端来说，因为每条连接都会耗费 cpu 和内存资源，大量假死的连接会逐渐耗光服务器的资源，最终导致性能逐渐下降，程序奔溃。
1. 对于客户端来说，连接假死会造成发送数据超时，影响用户体验。

通常，连接假死由以下几个原因造成的

1. 应用程序出现线程堵塞，无法进行数据的读写。
1. 客户端或者服务端网络相关的设备出现故障，比如网卡，机房故障。
1. 公网丢包。公网环境相对内网而言，非常容易出现丢包，网络抖动等现象，如果在一段时间内用户接入的网络连续出现丢包现象，那么对客户端来说数据一直发送不出去，而服务端也是一直收不到客户端来的数据，连接就一直耗着。

我们分别从客户端和服务端来解决这个问题
**服务端**
利用Netty 自带的 IdleStateHandler实现空闲检测，服务端只需要检测一段时间内，是否收到过客户端发来的数据即可，
```java
public IdleStateHandler(long readerIdleTime, long writerIdleTime, 
                        long allIdleTime,TimeUnit unit){
  
}
```

- 第一个参数是隔多久检查一下读事件是否发生，如果_ channelRead() _方法超过 readerIdleTime 时间未被调用则会触发一个_ READER_IDLE _的 _IdleStateEvent_ 事件；
- 第二个参数是隔多久检查一下写事件是否发生，_writerIdleTime_ 写空闲超时时间设定，如果 write() 方法超过 writerIdleTime 时间未被调用则会_WRITER_IDLE_ 的_ IdleStateEvent_ 事件；
- 第三个参数是全能型参数，隔多久检查读写事件；
- 第四个参数表示当前的时间单位。
```java
@Override
public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
        IdleState state = ((IdleStateEvent) evt).state();
        if (state == IdleState.READER_IDLE) {
            log.info("idle check happen, so close the connection");
            ctx.close();
        }
    } else {
        super.userEventTriggered(ctx, evt);
    }
}
```
服务端当15秒内没有读到数据（客户端发来的心跳），则出发userEventTriggered事件，关闭假死的
channel连接。

**客户端**
客户端同样添加IdleStateHandler
```java
ch.pipeline().addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS));
当5秒内没有主动远程调用，也就是没有写事件发生时候，触发userEventTriggered主动写并发送心跳数据包
 // 心跳发送
@Override
public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
        IdleState state = ((IdleStateEvent) evt).state();
        if (state == IdleState.WRITER_IDLE) {
            log.info("write idle happen [{}]", ctx.channel().remoteAddress());
            Channel channel = nettyClient.getChannel((InetSocketAddress) ctx.channel().remoteAddress());
            RpcMessage rpcMessage = new RpcMessage();
            rpcMessage.setCodec(SerializerTypeEnum.PROTOSTUFF.getCode());
            rpcMessage.setCompress(CompressTypeEnum.GZIP.getCode());
            rpcMessage.setMessageType(RpcConstants.HEARTBEAT_REQUEST_TYPE);
            rpcMessage.setData(RpcConstants.PING);
            channel.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        }
    } else {
        super.userEventTriggered(ctx, evt);
    }
}
```
### 
### 自定义协议
无论是使用 Netty 还是原始的 Socket 编程，基于 TCP 通信的数据包格式均为二进制，协议指的就是客户端与服务端事先商量好的，每一个二进制数据包中每一段字节分别代表什么含义的规则。对于XRPC来说，使用了消息头+消息体 的方式制定私有协议。其格式如下：
```java
 0     1     2     3     4        5     6     7     8    9          10      11     12     13    14   15   16
+-----+-----+-----+-----+--------+----+----+----+------+-----------+-------+----- --+-----+-----+----+---+
|   magic   code        |version |      full length    | messageType| codec|compress|    RequestId       |
+-----------------------+--------+---------------------+-----------+-----------+-----------+------------+
|                                                                                                       |
|                                         body                                                          |
|                                                                                                       |
|                                        ... ...                                                        |
+-------------------------------------------------------------------------------------------------------+
4B  magic code（魔法数）   
1B version（版本）   
4B full length（消息长度）   
1B messageType（消息类型）
1B compress（压缩类型） 
1B codec（序列化类型）    
4B  requestId（请求的Id）
body（object类型数据）
```
![image.png](https://cdn.nlark.com/yuque/0/2022/png/1164521/1651231638890-31866612-2803-4951-a68f-d2ce5d0414c1.png#clientId=u094a13e3-2a73-4&crop=0&crop=0&crop=1&crop=1&from=paste&id=uc778bce2&margin=%5Bobject%20Object%5D&name=image.png&originHeight=744&originWidth=4044&originalType=url&ratio=1&rotation=0&showTitle=false&size=292662&status=done&style=none&taskId=u852bc693-9d4c-410c-a238-3480158f8a9&title=)
**字段解释**
**1. magic（魔数)**
是通信双方协商的一个暗号，4 个字节，定义在 RpcConstants._MAGIC_NUMBER_。
魔数的作用是用于服务端在接收数据时先解析出魔数做正确性对比。如果和协议中的魔数不匹配，则认为是非法数据，可以直接关闭连接或采取其他措施增强系统安全性。
注意：这只是一个简单的校验，如果有安全性方面的需求，需要使用其他手段，例如 SSL/TLS。
魔数的思想在很多场景中都有体现，如 Java Class 文件开头就存储了魔数 OxCAFEBABE，在 JVM 加载 Class 文件时首先就会验证魔数对的正确性。
**2. version（版本)**
为了应对业务需求的变化，可能需要对自定义协议的结构或字段进行改动。不同版本的协议对应的解析方法也是不同的。所以在生产级项目中强烈建议预留协议版本这个字段。
**3. full length（消息长度)**
记录了整个消息的长度，这个字段是报文拆包的关键。
**4. messageType（消息类型)**
```java
/**
 *  消息类型
 */
byte REQUEST_TYPE = 1;
byte RESPONSE_TYPE = 2;
byte HEARTBEAT_REQUEST_TYPE = 3;
byte HEARTBEAT_RESPONSE_TYPE = 4;
```
**5. compress（压缩类型）**
序列化的字节流，还可以进行压缩，使得体积更小，在网络传输更快，但是同时会消耗 CPU 资源。
如果使用压缩效果好的序列化器，可以考虑不使用压缩
```java
/**
 * 伪压缩器，等于不使用压缩
 */
DUMMY((byte) 0, "dummy"),
GZIP((byte) 1, "gzip"),
UNZIP((byte) 2, "unzip");
```
**7. serialize（序列化类型）**
通过这个类型来确定使用哪种序列化方式，将字节流序列化成对应的对象。
序列化类型定义如下：
```java
HESSIAN((byte) 1, "hessian"),
KRYO((byte) 2, "kryo"),
PROTOSTUFF((byte) 3, "protostuff");
```
**8. requestId（请求的Id）**
每个请求分配好请求Id，这样响应数据的时候，才能对的上。使用4 字节的 int 类型
**9. body**
body 里面放具体的数据，通常来说是请求的参数request、响应的结果response，再经过序列化、压缩后的字节数组。
```java
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class RpcMessage {
    /**
     * rpc message type
     */
    private byte messageType;
    /**
     * serialization type
     */
    private byte codec;
    /**
     * compress type
     */
    private byte compress;
    /**
     * request id
     */
    private int requestId;
    /**
     * request data
     */
    private Object data;
}

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@ToString
public class RpcRequest implements Serializable {
    private static final long serialVersionUID = 2176648719840392878L;
    private String requestId;
    private String className;
    private String methodName;
    private Class<?>[] parameterTypes;
    private Object[] parameters;
    private String version;
}

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class RpcResponse<T> implements Serializable {
    private static final long serialVersionUID = 715745410605631233L;
    private String requestId;
    /**
     * response code
     */
    private Integer code;
    /**
     * response message
     */
    private String message;
    /**
     * response body
     */
    private T data;
}

```

### 编解码与粘包拆包
> TCP/IP协议，在用户数据量非常小的情况下，极端情况下，一个字节，该TCP数据包的有效载荷非常低，传递100字节的数据，需要100次TCP传送，100次ACK，在应用及时性要求不高的情况下，将这100个有效数据拼接成一个数据包，那会缩短到一个TCP数据包，以及一个ack，有效载荷提高了，带宽也节省了
> 非极端情况，有可能两个数据包拼接成一个数据包，也有可能一个半的数据包拼接成一个数据包，也有可能两个半的数据包拼接成一个数据包
拆包和粘包是相对的，一端粘了包，另外一端就需要将粘过的包拆开，举个栗子，发送端将三个数据包粘成两个TCP数据包发送到接收端，接收端就需要根据应用协议将两个数据包重新组装成三个数据包，还有一种情况就是用户数据包超过了mss(最大报文长度)，那么这个数据包在发送的时候必须拆分成几个数据包，接收端收到之后需要将这些数据包粘合起来之后，再拆开

#### 编码Encode
编码器相对比较简单，按照协议定义的长度和值进行设置
```java
    public ByteBuf encode(RpcMessage rpcMessage, ByteBuf out) {
        try {
            // 4B magic code（魔数）
            out.writeBytes(RpcConstants.MAGIC_NUMBER);
            // 1B version（版本）
            out.writeByte(RpcConstants.VERSION);
            // 4B full length（消息长度）. 先空着，后面填。
            out.writerIndex(out.writerIndex() + 4);
            // 1B messageType（消息类型）
            byte messageType = rpcMessage.getMessageType();
            out.writeByte(messageType);
            // 1B codec（序列化类型）
            out.writeByte(rpcMessage.getCodec());
            // 1B compress（压缩类型）
            out.writeByte(CompressTypeEnum.GZIP.getCode());
            // 4B requestId（请求的Id）
            out.writeInt(rpcMessage.getRequestId());
            // 写body，并获取数据长度
            byte[] bodyBytes = null;
            int fullLength = RpcConstants.HEAD_LENGTH;
            if (messageType != RpcConstants.HEARTBEAT_REQUEST_TYPE
                    && messageType != RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
                // 序列化
                String codecName = SerializerTypeEnum.getName(rpcMessage.getCodec());
                log.info("encode name: [{}] ", codecName);
                Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class)
                        .getExtension(codecName);
                bodyBytes = serializer.serialize(rpcMessage.getData());
                // 压缩
                String compressName = CompressTypeEnum.getName(rpcMessage.getCompress());
                Compress compress = ExtensionLoader.getExtensionLoader(Compress.class)
                        .getExtension(compressName);
                bodyBytes = compress.compress(bodyBytes);
                // 总长度=消息头长度+body
                fullLength += bodyBytes.length;
            }
            if (bodyBytes != null) {
                out.writeBytes(bodyBytes);
            }
            // 记录当前写指针
            int writeIndex = out.writerIndex();
            // 写空出的4B full length（消息长度）
            out.writerIndex(MAGIC_LENGTH + VERSION_LENGTH);
            out.writeInt(fullLength);
            // 写指针复原
            out.writerIndex(writeIndex);
        } catch (Exception e) {
            log.error("Encode request error!", e);
        }
        return out;
    }
```
#### 解码decode
```java
/**
 * ByteBuf 解码为RpcMessage
 */
public Object decode(ByteBuf in) {
    int fullLength = in.readInt();
    byte messageType = in.readByte();
    byte codecType = in.readByte();
    byte compressType = in.readByte();
    int requestId = in.readInt();
    RpcMessage rpcMessage = RpcMessage.builder()
            .codec(codecType)
            .requestId(requestId)
            .compress(compressType)
            .messageType(messageType).build();
    //心跳类型的请求、body 长度 0，不需要decode数据体
    if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
        rpcMessage.setData(RpcConstants.PING);
        return rpcMessage;
    }
    if (messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
        rpcMessage.setData(RpcConstants.PONG);
        return rpcMessage;
    }
    // 获取数据体body的长度
    int bodyLength = fullLength - RpcConstants.HEAD_LENGTH;
    byte[] bs = new byte[bodyLength];
    in.readBytes(bs);
    // 反压缩
    String compressName = CompressTypeEnum.getName(compressType);
    Compress compress = ExtensionLoader.getExtensionLoader(Compress.class)
            .getExtension(compressName);
    bs = compress.decompress(bs);
    // 反序列化
    String codecName = SerializerTypeEnum.getName(rpcMessage.getCodec());
    Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class)
            .getExtension(codecName);
    // 设置decode后的消息体
    Object object = serializer.deserialize(messageTypeMap.get(messageType), bs);
    rpcMessage.setData(object);
    return rpcMessage;
}
```
#### Netty 拆包器
使用最为常用的**基于长度域拆包器 LengthFieldBasedFrameDecoder**
只要自定义协议中包含长度域字段，均可以使用这个拆包器来实现应用层拆包。
```java
 new LengthFieldBasedFrameDecoder(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
```
- maxFrameLength：指定包的最大长度，如果超过，直接丢弃
- lengthFieldOffset： 描述长度的字段在第几个字节
- lengthFieldLength：length 字段本身的长度(几个字节)
- lengthAdjustment：包的总长度调整，去掉lengthFieldOffset+lengthFieldLength
- initialBytesToStrip： 跳过的字节数，之前的几个参数，已经足够识别出整个数据包了。但是很多时候，调用者只关心包的内容，包的头部完全可以丢弃掉
- initialBytesToStrip 就是用来告诉 Netty，识别出整个数据包之后，截掉 initialBytesToStrip之前的数据
  因此，这里我们的拆包参数为

```java
 new LengthFieldBasedFrameDecoder(RpcConstants.MAX_FRAME_LENGTH, 5, 4, -9, 0);
```
因为我们还需要检测 魔数 和 版本号，所以initialBytesToStrip=0，不能去除，当 魔数 和 版本号不符合规定时，拒绝非本协议连接。
```java
@Slf4j
public class Spliter extends LengthFieldBasedFrameDecoder {
    public Spliter() {
        this(MAX_FRAME_LENGTH, MAGIC_LENGTH + VERSION_LENGTH, FULL_LENGTH_LENGTH,
                -(MAGIC_LENGTH + VERSION_LENGTH + FULL_LENGTH_LENGTH), 0);
    }

    /**
     * @param maxFrameLength      指定包的最大长度，如果超过，直接丢弃
     * @param lengthFieldOffset   描述长度的字段在第几个字节
     * @param lengthFieldLength   length 字段本身的长度(几个字节)
     * @param lengthAdjustment    包的总长度调整，去掉lengthFieldOffset+lengthFieldLength
     * @param initialBytesToStrip 跳过的字节数，识别出整个数据包之后，截掉 initialBytesToStrip之前的数据
     */
    public Spliter(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        Object decoded = super.decode(ctx, in);
        ByteBuf frame = (ByteBuf) decoded;
        if (frame.readableBytes() >= RpcConstants.HEAD_LENGTH) {
            //拒绝非本协议连接
            if(!checkMagicNumberAndVersion(in)){
                ctx.channel().close();
                return null;
            }
        }
        return decoded;
    }

    /**
     * 读取并检查魔数和版本是否符合规定
     */
    private boolean checkMagicNumberAndVersion(ByteBuf in) {
        // 读取魔数
        int len = RpcConstants.MAGIC_NUMBER.length;
        byte[] bytes = new byte[len];
        in.readBytes(bytes);
        // 比较魔数是否符合规定，不符合抛出异常
        for (int i = 0; i < len; i++) {
            if (bytes[i] != RpcConstants.MAGIC_NUMBER[i]) {
                log.error("Unknown magic code: " + Arrays.toString(bytes));
                return false;
            }
        }
        // 读取版本号
        byte version = in.readByte();
        // 比较版本号是否符合规定，不符合抛出异常
        if (version != RpcConstants.VERSION) {
            log.error("version isn't compatible" + version);
            return false;
        }
        return true;
    }
}

```

### 服务端业务Handler
服务器端需要隔离EventLoop（Reactor）线程和业务 线程。所以需要使用独立的、异步的线程任务去执行用户验证 的逻辑；而不在EventLoop线程中去执行用户验证的逻辑。
在默认情况下，Netty的一个EventLoop实例会开启2倍 CPU核数的内部线程。通常情况下，一个Netty服务器端会有几万或者 几十万的连接通道。也就是说，一个EventLoop内部线程会负责处理着 几万个或者上十万个通道连接的IO处理，而耗时的入站/出站处理越 多，就越会拖慢整个线程的其他IO处理，最终导致严重的性能问题。
因此这里我们是用一个独立的异步任务处理队列去处理业务逻辑。
```java
DefaultEventExecutorGroup serviceHandlerGroup = new DefaultEventExecutorGroup(
            RuntimeUtil.getProcessorCount() * 2,
            ThreadUtil.newNamedThreadFactory("service-handler-group", false)
);
ch.pipeline().addLast(serviceHandlerGroup, new NettyServerHandler());
```
**具体的handler业务逻辑**
```java
public class NettyServerHandler extends SimpleChannelInboundHandler<RpcMessage> {
    private final ServiceProvider serviceProvider;

    public NettyServerHandler() {
        serviceProvider = SingletonFactory.getInstance(ServiceProvider.class);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        log.info("server receive msg: [{}] ", rpcMessage);
        byte messageType = rpcMessage.getMessageType();
        // 如果是心跳消息，回复pong
        if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
            rpcMessage.setMessageType(RpcConstants.HEARTBEAT_RESPONSE_TYPE);
            rpcMessage.setData(RpcConstants.PONG);
        } else {
            RpcRequest rpcRequest = (RpcRequest) rpcMessage.getData();
            // 根据请求的参数，找到对应的服务，反射执行方法
            Object result = handle(rpcRequest);
            log.info(String.format("server get result: %s", result.toString()));
            rpcMessage.setMessageType(RpcConstants.RESPONSE_TYPE);
            if (ctx.channel().isActive() && ctx.channel().isWritable()) {
                RpcResponse<Object> rpcResponse = RpcResponse.success(result, rpcRequest.getRequestId());
                rpcMessage.setData(rpcResponse);
            } else {
                RpcResponse<Object> rpcResponse = RpcResponse.fail(RpcResponseCodeEnum.FAIL);
                rpcMessage.setData(rpcResponse);
                log.error("not writable now, message dropped");
            }
        }
        ctx.writeAndFlush(rpcMessage).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    future.channel().close();
                    log.error("Fail!! Send response for request " + rpcMessage.getRequestId());

                } else {
                    log.info("Send response for request " + rpcMessage.getRequestId());
                }
            }
        });

    }
    
}
 private Object handle(RpcRequest request) {
    String className = request.getClassName();
    String version = request.getVersion();
    String serviceKey = ServiceUtil.makeServiceKey(className, version);
    Object serviceBean = serviceProvider.getService(serviceKey);
    if (serviceBean == null) {
        log.error("Can not find service implement with interface name: {} and version: {}", className, version);
    }
    return invokeCglib(request, serviceBean);
}
```
解析request请求服务，在服务端注册服务的时候本地缓存一个服务Map，从Map中找到服务，使用反射调用，并将结果返回，构造response，并写入到channal,最后encode发送

## 使用Netty来实现XPRC客户端
### 整体架构逻辑

![image.png](https://cdn.nlark.com/yuque/0/2022/png/1164521/1651244325781-e8658c69-83a8-4a8c-a4f9-9e57b44820fd.png#clientId=u094a13e3-2a73-4&crop=0&crop=0&crop=1&crop=1&from=paste&id=u56eeab43&margin=%5Bobject%20Object%5D&name=image.png&originHeight=1252&originWidth=4184&originalType=url&ratio=1&rotation=0&showTitle=false&size=548410&status=done&style=none&taskId=ue69f9fea-00be-48a5-8979-5f64a502d04&title=)
当用户要调用一个远程服务时，给该服务添加@RpcAutowired注解，那么该服务将自动被替换为其动态代理类，代理中包含从调用-构造RPC request- 获取连接channel-编码-发送， 收到回复-拆包-解码-与发送的request关联response-返回调用结果

### 动态代理逻辑
```java
@Slf4j
public class ObjectProxy<T> implements InvocationHandler {
    private Class<T> clazz;
    private String version;

    public ObjectProxy(Class<T> clazz, String version) {
        this.clazz = clazz;
        this.version = version;
    }

    /**
     * 获取被调用服务的动态代理类
     */
    public static <T> T createService(Class<T> interfaceClass, String version) {
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new ObjectProxy<T>(interfaceClass, version)
        );
    }
    /**
     *  客户端主要逻辑，包括发送请求，相应结果与请求的绑定
     *
     */
    @SneakyThrows
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        log.info("client invoked method: [{}]", method.getName());
        RpcRequest rpcRequest = RpcRequest.builder()
                .methodName(method.getName())
                .parameters(args)
                .parameterTypes(method.getParameterTypes())
                .className(method.getDeclaringClass().getName())
                .requestId(UUID.randomUUID().toString())
                .version(version)
                .build();
        // 向服务端发送请求
        CompletableFuture<RpcResponse<Object>> completableFuture = (CompletableFuture<RpcResponse<Object>>) NettyClient.getInstance().sendRequest(rpcRequest);
        // 阻塞等待调用请求的结果，当 Netty Client 收到对应请求的回复时，future.complete（response）,完成相应
        RpcResponse<Object> rpcResponse = completableFuture.get();
        this.check(rpcResponse, rpcRequest);
        return rpcResponse.getData();

    }

    private void check(RpcResponse<Object> rpcResponse, RpcRequest rpcRequest) {
        if (rpcResponse == null) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, "interfaceName" + ":" + rpcRequest.getMethodName());
        }

        if (!rpcRequest.getRequestId().equals(rpcResponse.getRequestId())) {
            throw new RpcException(RpcErrorMessageEnum.REQUEST_NOT_MATCH_RESPONSE, "interfaceName" + ":" + rpcRequest.getMethodName());
        }

        if (rpcResponse.getCode() == null || !rpcResponse.getCode().equals(RpcResponseCodeEnum.SUCCESS.getCode())) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, "interfaceName" + ":" + rpcRequest.getMethodName());
        }
    }
}

```
### 发送请求
```java
public Object sendRequest(RpcRequest rpcRequest) {
    // 构造返回Future
    CompletableFuture<RpcResponse<Object>> resultFuture = new CompletableFuture<>();
    // 通过负载均衡获取服务端地址
    InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest);
    // Channel复用，获取之前连接过的或者断线重连得Netty Channel
    Channel channel = getChannel(inetSocketAddress);
    if (channel.isActive()) {
        // 将请求放入未完成请求的Map缓存中,key为请求的唯一ID, value存放异步回调Future
        pendingRpcRequests.put(rpcRequest.getRequestId(), resultFuture);
        RpcMessage rpcMessage = RpcMessage.builder().data(rpcRequest)
                .codec(SerializerTypeEnum.HESSIAN.getCode())
                .compress(CompressTypeEnum.GZIP.getCode())
                .requestId(REQUEST_ID.getAndIncrement())
                .messageType(RpcConstants.REQUEST_TYPE).build();
        // 发送请求
        channel.writeAndFlush(rpcMessage).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    log.info("client send message: [{}]", rpcMessage);
                } else {
                    future.channel().close();
                    resultFuture.completeExceptionally(future.cause());
                    log.error("Send failed:", future.cause());
                }
            }
        });

    } else {
        throw new IllegalStateException();
    }
    return resultFuture;

}

```
### Channel复用与重连
```java

/**
 * 获取和指定地址连接的 channel，Channel复用，不用每次请求都重新连接
 * 如果获取不到，则新建连接（重连）
 *
 * @param inetSocketAddress 待连接scoket地址
 * @return: {@link Channel} 获取到的连接
 */
@SneakyThrows
public Channel getChannel(InetSocketAddress inetSocketAddress) {
    Channel channel = channelProvider.get(inetSocketAddress);
    if (channel == null) {
        // 阻塞等待，获取连接成功的channel
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        channel = doConnect(completableFuture,inetSocketAddress, MAX_RETRY).get();
        channelProvider.set(inetSocketAddress, channel);
    }
    return channel;
}


/**
 * 与服务端建立连接
 */
@SneakyThrows
public CompletableFuture<Channel> doConnect(CompletableFuture<Channel> completableFuture,InetSocketAddress inetSocketAddress, int retry) {
    bootstrap.connect(inetSocketAddress).addListener(future -> {
        if (future.isSuccess()) {
            log.info("The client has connected [{}] successful!", inetSocketAddress.toString());
            completableFuture.complete(((ChannelFuture) future).channel());
        } else if (retry == 0) {
            log.error("the number of retries expired, connect fail. address:", inetSocketAddress);
        } else {
            // 当前是第几次重连
            int now = MAX_RETRY - retry + 1;
            // 本次重连的时间间隔
            int delay = 1 << now;
            log.warn("connect fail, attempt to reconnect. retry:" + now);
            bootstrap.config().group().schedule(() ->
                    doConnect(completableFuture,inetSocketAddress, retry - 1), delay, TimeUnit.SECONDS);
        }
    });
    return completableFuture;
}
```
### Client的Pipline流水线
```java
@Slf4j
public class NettyClient {
    private final ServiceDiscovery serviceDiscovery;
    private final ChannelProvider channelProvider;
    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;
    private final PendingRpcRequests pendingRpcRequests;
    private static volatile NettyClient instance = null;

    /**
     * 线程安全的懒汉单例
     */
    public static NettyClient getInstance() {
        if (instance == null) {
            synchronized (NettyClient.class) {
                if (instance == null) {
                    instance = new NettyClient();
                }
            }
        }
        return instance;
    }

    public NettyClient() {
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                // 连接超时时间
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                // TCP 底层心跳机制
                .option(ChannelOption.SO_KEEPALIVE, true)
                //要求高实时性，有数据发送时就马上发送，就设置为 true 关闭，如果需要减少发送次数减少网络交互，就设置为 false 开启
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS));
                        ch.pipeline().addLast(new RpcEncoder());
                        ch.pipeline().addLast(new Spliter());
                        ch.pipeline().addLast(new RpcDecoder());
                        ch.pipeline().addLast(new NettyClientHandler());
                    }
                });
        this.channelProvider = SingletonFactory.getInstance(ChannelProvider.class);
        this.serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension("zk");
        this.pendingRpcRequests = SingletonFactory.getInstance(PendingRpcRequests.class);
    }
}
```
### Client接受响应的逻辑处理
```java
/**
 * 从服务端读到消息时的业务逻辑
 */
@Override
protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcMessage rpcMessage) throws Exception {
    log.info("client receive msg: [{}]", rpcMessage);
    byte messageType = rpcMessage.getMessageType();
    if (messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
        log.info("heart receive[{}]", rpcMessage.getData());
    } else if (messageType == RpcConstants.RESPONSE_TYPE) {
        RpcResponse<Object> rpcResponse = (RpcResponse<Object>) rpcMessage.getData();
        // 调用结果相应 绑定到对应的请求
        pendingRpcRequests.complete(rpcResponse);
    }
}

/**
 * @description: 未收到回复的请求
 */
public class PendingRpcRequests {
    public static final Map<String, CompletableFuture<RpcResponse<Object>>> PENDING_RESPONSE_FUTURES = new ConcurrentHashMap<>();

    public void put(String requestId, CompletableFuture<RpcResponse<Object>> future) {
        PENDING_RESPONSE_FUTURES.put(requestId, future);
    }

    /**
     * 将请求与调用结果响应绑定
     *
     * @param rpcResponse 收到服务端发来的调用结果
     */
    public void complete(RpcResponse<Object> rpcResponse) {
        CompletableFuture<RpcResponse<Object>> future = PENDING_RESPONSE_FUTURES.remove(rpcResponse.getRequestId());
        if (null != future) {
            future.complete(rpcResponse);
        } else {
            throw new IllegalStateException();
        }
    }
}
```
#### 如何将调用的结果response和请求request绑定呢？
1、 通过channel的Attributekey绑定

2. 通 CompletableFuture 包装返回结，使用request和response统一的ID作为key，服务端收到请求之后，将 RequestId 原封不动写到响应结果中。客户端收到响应结果后，拿出 RequestId 找到对应的 Future 并写入结果。

![image.png](https://cdn.nlark.com/yuque/0/2022/png/1164521/1651247120029-4999acb6-a108-4367-b27b-7e91713e8139.png#clientId=u094a13e3-2a73-4&crop=0&crop=0&crop=1&crop=1&from=paste&id=u3f547007&margin=%5Bobject%20Object%5D&name=image.png&originHeight=1764&originWidth=3168&originalType=url&ratio=1&rotation=0&showTitle=false&size=566555&status=done&style=none&taskId=u2d03d89b-e2a9-479b-bc3f-99dceb92cfe&title=)

## 参考：
[Netty 入门与实战：仿写微信 IM 即时通讯系统](https://juejin.cn/book/6844733738119593991)
Netty、Redis、Zookeeper高并发实战

