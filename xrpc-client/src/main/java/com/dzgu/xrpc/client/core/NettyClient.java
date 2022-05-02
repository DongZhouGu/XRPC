package com.dzgu.xrpc.client.core;

import com.dzgu.xrpc.codec.RpcDecoder;
import com.dzgu.xrpc.codec.RpcEncoder;
import com.dzgu.xrpc.codec.Spliter;
import com.dzgu.xrpc.dto.RpcMessage;
import com.dzgu.xrpc.dto.RpcRequest;
import com.dzgu.xrpc.dto.RpcResponse;
import com.dzgu.xrpc.exception.RpcException;
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

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.dzgu.xrpc.consts.RpcConstants.MAX_RETRY;

/**
 * @description: Netty 客户端
 * @Author： dzgu
 * @Date： 2022/4/25 13:58
 */
@Slf4j
public class NettyClient {
    private final ChannelProvider channelProvider;
    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;
    private final PendingRpcRequests pendingRpcRequests;

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
        this.pendingRpcRequests = SingletonFactory.getInstance(PendingRpcRequests.class);
    }


    public RpcResponse<Object> sendRequest(RpcMessage rpcMessage, String targetServiceUrl) {
        String[] socketAddressArray = targetServiceUrl.split(":");
        String host = socketAddressArray[0];
        int port = Integer.parseInt(socketAddressArray[1]);
        InetSocketAddress remoteaddress = new InetSocketAddress(host, port);
        // 构造返回Future
        CompletableFuture<RpcResponse<Object>> resultFuture = new CompletableFuture<>();
        // Channel复用，获取之前连接过的或者断线重连得Netty Channel
        Channel channel = getChannel(remoteaddress);
        RpcResponse<Object> rpcResponse = null;
        try {
            // 将请求放入未完成请求的Map缓存中,key为请求的唯一ID, value存放异步回调Future
            pendingRpcRequests.put(((RpcRequest)rpcMessage.getData()).getRequestId(), resultFuture);
            // 发送请求
            channel.writeAndFlush(rpcMessage).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    if (future.isSuccess()) {
                        log.info("client send message: [{}]", rpcMessage);
                    } else {
                        future.channel().close();
                        resultFuture.completeExceptionally(future.cause());
                        log.error("Send failed:", future.cause());
                    }
                }
            });
            // 阻塞等待调用请求的结果，当 Netty Client 收到对应请求的回复时，future.complete（response）,完成相应
            // TODO 异步调用
            rpcResponse = resultFuture.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("send request error: " + e.getMessage());
            throw new RpcException("send request error:", e);
        } finally {
            channelProvider.remove(remoteaddress);
        }
        return rpcResponse;

    }

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
            channel = doConnect(completableFuture, inetSocketAddress, MAX_RETRY).get();
            channelProvider.set(inetSocketAddress, channel);
        }
        return channel;
    }


    /**
     * 与服务端建立连接
     */
    @SneakyThrows
    public CompletableFuture<Channel> doConnect(CompletableFuture<Channel> completableFuture, InetSocketAddress inetSocketAddress, int retry) {
        bootstrap.connect(inetSocketAddress).addListener(future -> {
            if (future.isSuccess()) {
                log.info("The client has connected [{}] successful!", inetSocketAddress.toString());
                completableFuture.complete(((ChannelFuture) future).channel());
            } else if (retry == 0) {
                log.error("the number of retries expired, connect fail. address:", inetSocketAddress.toString());
            } else {
                // 当前是第几次重连
                int now = MAX_RETRY - retry + 1;
                // 本次重连的时间间隔
                int delay = 1 << now;
                log.warn("connect fail, attempt to reconnect. retry:" + now);
                bootstrap.config().group().schedule(() ->
                        doConnect(completableFuture, inetSocketAddress, retry - 1), delay, TimeUnit.SECONDS);
            }
        });
        return completableFuture;
    }

    public void stop() {
        eventLoopGroup.shutdownGracefully();
    }

}
