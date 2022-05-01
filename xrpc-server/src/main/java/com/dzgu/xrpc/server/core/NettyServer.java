package com.dzgu.xrpc.server.core;

import com.dzgu.xrpc.codec.RpcDecoder;
import com.dzgu.xrpc.codec.RpcEncoder;
import com.dzgu.xrpc.codec.Spliter;
import com.dzgu.xrpc.consts.enums.RpcConfigEnum;
import com.dzgu.xrpc.extension.ExtensionLoader;
import com.dzgu.xrpc.server.registry.ServiceRegistry;
import com.dzgu.xrpc.util.PropertiesFileUtil;
import com.dzgu.xrpc.util.RuntimeUtil;
import com.dzgu.xrpc.util.SingletonFactory;
import com.dzgu.xrpc.util.threadpool.ThreadPoolFactoryUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.protostuff.Rpc;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @description: Netty服务器实现
 * @Author： dzgu
 * @Date： 2022/4/22 0:06
 */
@Slf4j
public class NettyServer {
    private Thread thread;
    private final ServiceRegistry serviceRegistry;
    protected final ServiceProvider serviceProvider;
    private static final int DEFAULT_NETTY_PORT = 18866;
    private static final String DEFAULT_LOCAL_HOST = "127.0.0.1";
    private InetSocketAddress serverAddress = getServerAddress();

    public NettyServer() {
        this.serviceProvider = SingletonFactory.getInstance(ServiceProvider.class);
        this.serviceRegistry = ExtensionLoader.getExtensionLoader(ServiceRegistry.class).getExtension("zk");
        this.serviceRegistry.setRegisterAddress("106.14.145.23:2181");
    }

    public static InetSocketAddress getServerAddress() {
        // check if user has set netty port
        Properties properties = PropertiesFileUtil.readPropertiesFile(RpcConfigEnum.RPC_CONFIG_PATH.getPropertyValue());
        int nettyPort = properties != null && properties.getProperty(RpcConfigEnum.NETTY_PORT.getPropertyValue()) != null ? Integer.parseInt(properties.getProperty(RpcConfigEnum.NETTY_PORT.getPropertyValue())) : DEFAULT_NETTY_PORT;
        String host = null;
        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.error("occur exception when getHostAddress", e);
        }
        return new InetSocketAddress(host == null ? DEFAULT_LOCAL_HOST : host, nettyPort);

    }


    public void start() {
        thread = new Thread(new Runnable() {
            DefaultEventExecutorGroup serviceHandlerGroup = new DefaultEventExecutorGroup(
                    RuntimeUtil.cpus() * 2,
                    ThreadPoolFactoryUtil.createThreadFactory("com.dzgu.xprc.service-handler-group", false));

            @Override
            public void run() {
                // 负责服务器通道新连接的IO事件的监听
                NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
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
                            .handler(new LoggingHandler(LogLevel.DEBUG))
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
                    if (serviceRegistry != null) {
                        serviceRegistry.registerServiceMap(serverAddress, serviceProvider.getserviceMap());
                    } else {
                        log.warn("ServiceRegistry cannot be found and started");
                    }
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
                        // 关闭EventLoopGroup
                        // 释放掉所有资源，包括创建的反应器线程
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
