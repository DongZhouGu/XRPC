package com.dzgu.xrpc.client.async;

import com.dzgu.xrpc.consts.enums.RpcErrorMessageEnum;
import com.dzgu.xrpc.consts.enums.RpcResponseCodeEnum;
import com.dzgu.xrpc.dto.RpcResponse;
import com.dzgu.xrpc.exception.RpcException;
import lombok.extern.slf4j.Slf4j;

/**
 * @description: 客户端异步请求回调
 * @Author： dzgu
 * @Date： 2022/5/3 23:41
 */
@Slf4j
public abstract class ResponseCallback {

    public void success(RpcResponse<Object> response) {
        AsyncCallBackExecutor.execute(() -> {
            log.debug("AsyncReceiveHandler success context:{} response:{}", response);
            if (response.getCode() == RpcResponseCodeEnum.SUCCESS.getCode()) {
                try {
                    callBack(response);
                } catch (Exception e) {
                    onException(response, e);
                }
            } else {
                onException(response, new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE));
            }
        });
    }

    /**
     * 重写此方法，添加异步接收到结果之后的业务逻辑
     *
     * @param result
     */
    public abstract void callBack(RpcResponse<Object> result);

    /**
     * 重写此方法，可以在callBack中自行处理业务处理异常，也可以重写此方法兜底处理
     *
     * @param result
     * @param e
     */
    public abstract void onException(RpcResponse<Object> result, Exception e);
}
