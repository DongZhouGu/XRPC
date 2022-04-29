package com.dzgu.xrpc.client.core;

import cn.hutool.core.util.StrUtil;
import com.dzgu.xrpc.client.discover.ServiceDiscovery;
import com.dzgu.xrpc.codec.RpcDecoder;
import com.dzgu.xrpc.codec.RpcEncoder;
import com.dzgu.xrpc.codec.Spliter;
import com.dzgu.xrpc.config.RpcConstants;
import com.dzgu.xrpc.config.enums.CompressTypeEnum;
import com.dzgu.xrpc.config.enums.SerializerTypeEnum;
import com.dzgu.xrpc.dto.RpcMessage;
import com.dzgu.xrpc.dto.RpcRequest;
import com.dzgu.xrpc.dto.RpcResponse;
import com.dzgu.xrpc.exception.RpcException;
import com.dzgu.xrpc.extension.ExtensionLoader;
import com.dzgu.xrpc.util.SingletonFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.codec.Decoder;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.dzgu.xrpc.config.RpcConstants.REQUEST_ID;

/**
 * @description: Netty 客户端
 * @Author： dzgu
 * @Date： 2022/4/25 13:58
 */
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

    /**
     * 获取和指定地址连接的 channel，Channel复用，不用每次请求都重新连接
     * 如果获取不到，则新建连接（重连）
     *
     * @param inetSocketAddress 待连接scoket地址
     * @return: {@link Channel} 获取到的连接
     */
    public Channel getChannel(InetSocketAddress inetSocketAddress) {
        Channel channel = channelProvider.get(inetSocketAddress);
        if (channel == null) {
            channel = doConnect(inetSocketAddress);
            channelProvider.set(inetSocketAddress, channel);
        }
        return channel;
    }


    /**
     * 与服务端建立连接
     */
    @SneakyThrows
    public Channel doConnect(InetSocketAddress inetSocketAddress) {
        try {
            CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
            bootstrap.connect(inetSocketAddress).addListener(future -> {
                if (future.isSuccess()) {
                    log.info("The client has connected [{}] successful!", inetSocketAddress.toString());
                    completableFuture.complete(((ChannelFuture) future).channel());
                } else {
                    log.error("connect fail. address:", inetSocketAddress);
                }
            });
            // 阻塞等待，获取连接成功的channel, 10s为获取到，报错
            return completableFuture.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RpcException(inetSocketAddress + " connect fail.", e);
        }
    }

    public void stop() {
        eventLoopGroup.shutdownGracefully();
    }

}
