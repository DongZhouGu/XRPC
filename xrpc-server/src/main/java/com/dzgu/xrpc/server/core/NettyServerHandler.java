package com.dzgu.xrpc.server.core;

import com.dzgu.xrpc.config.RpcConstants;
import com.dzgu.xrpc.config.enums.RpcResponseCodeEnum;
import com.dzgu.xrpc.dto.RpcMessage;
import com.dzgu.xrpc.dto.RpcRequest;
import com.dzgu.xrpc.dto.RpcResponse;
import com.dzgu.xrpc.exception.RpcException;
import com.dzgu.xrpc.util.ServiceUtil;
import com.dzgu.xrpc.util.SingletonFactory;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.reflect.FastClass;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @description: Netty 服务端业务逻辑
 * @Author： dzgu
 * @Date： 2022/4/22 19:59
 */
@Slf4j
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

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("server catch exception");
        cause.printStackTrace();
        ctx.close();
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

    private Object invokeJDK(RpcRequest rpcRequest, Object service) {
        Object result;
        try {
            Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParameterTypes());
            method.setAccessible(true);
            result = method.invoke(service, rpcRequest.getParameters());
            log.info("service:[{}] successful invoke method:[{}]", rpcRequest.getClassName(), rpcRequest.getMethodName());
        } catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
            throw new RpcException(e.getMessage(), e);
        }
        return result;
    }

    private Object invokeCglib(RpcRequest rpcRequest, Object service) {
        Object result;
        try {
            Class<?> serviceClass = service.getClass();
            FastClass fastClass = FastClass.create(serviceClass);
            int methodIndex = fastClass.getIndex(rpcRequest.getMethodName(), rpcRequest.getParameterTypes());
            result = fastClass.invoke(methodIndex, service, rpcRequest.getParameters());
        } catch (Exception e) {
            throw new RpcException(e.getMessage(), e);
        }
        return result;

    }
}
