package com.dzgu.xrpc.server.core;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

import static com.dzgu.xrpc.config.RpcConstants.BEAT_INTERVAL;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/4/22 15:25
 */
@Slf4j
public class IMIdleStateHandler extends IdleStateHandler {
    public IMIdleStateHandler(){
        super(0,0, BEAT_INTERVAL,TimeUnit.SECONDS);
    }
    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        super.channelIdle(ctx,evt);
        log.warn(BEAT_INTERVAL + "秒内未读到心跳数据");
    }
}
