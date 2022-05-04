package com.dzgu.xrpc.client.async;

import com.dzgu.xrpc.dto.RpcResponse;

import java.util.concurrent.*;

/**
 * @description: 处理异步回调
 * @Author： dzgu
 * @Date： 2022/5/3 23:39
 */
public class RpcFuture implements Future {
    private RpcResponse<Object> response;

    private ResponseCallback responseCallback;
    private CountDownLatch countDownLatch;

    public RpcFuture() {
        countDownLatch = new CountDownLatch(1);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    /**
     * 阻塞获取结果
     */
    @Override
    public RpcResponse<Object> get() throws InterruptedException, ExecutionException {
        countDownLatch.await();
        return response;
    }

    @Override
    public RpcResponse<Object> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (countDownLatch.await(timeout, unit)) {
            return response;
        }
        return null;
    }

    public void complete(RpcResponse<Object> response) {
        this.response = response;
        countDownLatch.countDown();
        if(responseCallback!=null){
            responseCallback.success(response);
        }
    }

    public void setResponseCallback(ResponseCallback responseCallback) {
        this.responseCallback = responseCallback;
    }
}
