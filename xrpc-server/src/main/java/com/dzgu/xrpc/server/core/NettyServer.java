package com.dzgu.xrpc.server.core;

import com.dzgu.xrpc.codec.RpcDecoder;
import com.dzgu.xrpc.codec.RpcEncoder;
import com.dzgu.xrpc.codec.Spliter;
import com.dzgu.xrpc.register.Register;
import com.dzgu.xrpc.server.invoke.Invoker;
import com.dzgu.xrpc.util.RuntimeUtil;
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
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * @description: Netty服务器实现
 * @Author： dzgu
 * @Date： 2022/4/22 0:06
 */
@Slf4j
@Setter
public class NettyServer {
    private Thread thread;
    private Register register;
    private Invoker invoker;
    protected ServiceRegisterCache serviceRegisterCache;
    private InetSocketAddress serverAddress;

    public NettyServer() {

    }


    public void start() {
        thread = new Thread(new Runnable() {
            DefaultEventExecutorGroup serviceHandlerGroup = new DefaultEventExecutorGroup(
                    RuntimeUtil.cpus() * 2,
                    ThreadPoolFactoryUtil.createThreadFactory("com.dzgu.xrpc.service-handler-group", false));

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
                                    // 处理粘包包
                                    ch.pipeline().addLast(new Spliter());
                                    ch.pipeline().addLast(new RpcDecoder());
                                    ch.pipeline().addLast(new RpcEncoder());
                                    ch.pipeline().addLast(serviceHandlerGroup, new NettyServerHandler(invoker, serviceRegisterCache));

                                }
                            });
                    // 绑定端口，同步等待绑定成功
                    //bind操作(对应初始化)是异步的，通过sync改为同步等待初始化的完成，否则立即操作对象(未初始完全)可能会报错
                    ChannelFuture f = bootstrap.bind(serverAddress).sync();
                    if (register != null) {
                        register.registerServiceMap(serviceRegisterCache.getserviceMap(), serverAddress);
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
                        register.unregisterAllMyService(serverAddress);
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
