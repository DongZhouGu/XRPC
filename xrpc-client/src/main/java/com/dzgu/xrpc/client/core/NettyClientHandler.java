package com.dzgu.xrpc.client.core;

import com.dzgu.xrpc.config.RpcConstants;
import com.dzgu.xrpc.config.enums.CompressTypeEnum;
import com.dzgu.xrpc.config.enums.SerializerTypeEnum;
import com.dzgu.xrpc.dto.RpcMessage;
import com.dzgu.xrpc.dto.RpcResponse;
import com.dzgu.xrpc.util.SingletonFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/4/25 17:25
 */
@Slf4j
public class NettyClientHandler extends SimpleChannelInboundHandler<RpcMessage> {
    private final PendingRpcRequests pendingRpcRequests;
    private final NettyClient nettyClient;

    public NettyClientHandler() {
        this.pendingRpcRequests = SingletonFactory.getInstance(PendingRpcRequests.class);
        this.nettyClient = SingletonFactory.getInstance(NettyClient.class);
    }

    /**
     * Read the message transmitted by the server
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcMessage rpcMessage) throws Exception {
        log.info("client receive msg: [{}]", rpcMessage);
        byte messageType = rpcMessage.getMessageType();
        if (messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
            log.info("heart receive[{}]", rpcMessage.getData());
        } else if (messageType == RpcConstants.RESPONSE_TYPE) {
            RpcResponse<Object> rpcResponse = (RpcResponse<Object>) rpcMessage.getData();
            // TODO 异步调用
            pendingRpcRequests.complete(rpcResponse);
        }
    }

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

    /**
     * Called when an exception occurs in processing a client message
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("client catch exception：", cause);
        cause.printStackTrace();
        ctx.close();
    }
}
