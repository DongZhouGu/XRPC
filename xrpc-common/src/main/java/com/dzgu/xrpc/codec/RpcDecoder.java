package com.dzgu.xrpc.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/4/22 16:42
 */
@Slf4j
public class RpcDecoder extends ByteToMessageDecoder {
    public RpcDecoder() {
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception {
        out.add(RpcCodec.INSTANCE.decode(byteBuf));
    }

}
