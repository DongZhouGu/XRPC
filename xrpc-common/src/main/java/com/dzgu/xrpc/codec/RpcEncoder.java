package com.dzgu.xrpc.codec;

import com.dzgu.xrpc.dto.RpcMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/4/24 14:27
 */
public class RpcEncoder extends MessageToByteEncoder<RpcMessage> {
    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage rpcMessage, ByteBuf byteBuf) throws Exception {
        RpcCodec.INSTANCE.encode(rpcMessage, byteBuf);
    }
}
