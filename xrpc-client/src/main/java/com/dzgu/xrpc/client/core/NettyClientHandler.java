package com.dzgu.xrpc.client.core;

import com.dzgu.xrpc.consts.RpcConstants;
import com.dzgu.xrpc.consts.enums.CompressTypeEnum;
import com.dzgu.xrpc.consts.enums.SerializerTypeEnum;
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

/**
 * @description: Netty客户端业务逻辑
 * @Author： dzgu
 * @Date： 2022/4/25 17:25
 */
@Slf4j
public class NettyClientHandler extends SimpleChannelInboundHandler<RpcMessage> {
    private final PendingRpcRequests pendingRpcRequests;
    protected volatile Channel channel;

    public NettyClientHandler() {
        this.pendingRpcRequests = SingletonFactory.getInstance(PendingRpcRequests.class);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("Connect to server successfully:{}", ctx);
        this.channel = ctx.channel();
    }

    /**
     * 从服务端读到消息时的业务逻辑
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcMessage rpcMessage) throws Exception {
        log.info("client receive msg: [{}]", rpcMessage);
        byte messageType = rpcMessage.getMessageType();
        if (messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
            log.debug("heart receive[{}]", rpcMessage.getData());
        } else if (messageType == RpcConstants.RESPONSE_TYPE) {
            RpcResponse<Object> rpcResponse = (RpcResponse<Object>) rpcMessage.getData();
            // 调用结果响应 绑定到对应的请求
            pendingRpcRequests.complete(rpcResponse);
        }
    }

    /**
     * 当5秒内没有主动远程调用，也就是没有写事件发生时候，触发userEventTriggered主动写并发送心跳数据包
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.WRITER_IDLE) {
                log.debug("write idle happen [{}]", ctx.channel().remoteAddress());
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
     * 客户端异常捕获，并关闭连接, 从channel Map中删除channel, 下次调用自动重连server端
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("client catch exception：", cause);
        cause.printStackTrace();
        ctx.close();
    }
}
