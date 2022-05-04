package com.dzgu.xrpc.client.async;

/**
 * @description: RpcFuture的上下文
 * @Author： dzgu
 * @Date： 2022/5/4 11:59
 */
public class RpcContext {
    private static ThreadLocal<ResponseCallback> localCallback = new ThreadLocal<>();

    public static void setCallback(ResponseCallback callback) {
        localCallback.set(callback);
    }

    public static ResponseCallback getCallback() {
        return localCallback.get();
    }
}
