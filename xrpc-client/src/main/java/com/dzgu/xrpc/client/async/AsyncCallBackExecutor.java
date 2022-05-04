package com.dzgu.xrpc.client.async;

import com.dzgu.xrpc.util.threadpool.ThreadPoolFactoryUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @description: 客户端异步回调执行器
 * @Author： dzgu
 * @Date： 2022/5/3 23:43
 */
@Slf4j
public class AsyncCallBackExecutor {
    private static final int worker = 4;

    private static class ThreadPoolExecutorHolder {
        static {
            log.info("call back executor work count is " + worker);
        }

        private final static ThreadPoolExecutor callBackExecutor = new ThreadPoolExecutor(
                worker, worker, 2000L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                ThreadPoolFactoryUtil.createThreadFactory("XRPC-Client-AsyncCallBackExecutor", false));
    }
    public static void execute(Runnable runnable) {
        ThreadPoolExecutorHolder.callBackExecutor.execute(runnable);
    }

}
