package com.dzgu.xrpc.exception;

import com.dzgu.xrpc.config.enums.RpcErrorMessageEnum;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/4/22 23:14
 */
public class RpcException extends RuntimeException{
    public RpcException(RpcErrorMessageEnum rpcErrorMessageEnum, String detail) {
        super(rpcErrorMessageEnum.getMessage() + ":" + detail);
    }

    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }

    public RpcException(RpcErrorMessageEnum rpcErrorMessageEnum) {
        super(rpcErrorMessageEnum.getMessage());
    }
}
