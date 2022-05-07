package com.dzgu.xrpc.invoke;

import com.dzgu.xprc.RpcClientSpringBootApplication;
import com.dzgu.xprc.controller.HelloController;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.*;

/**
 * @description: 压力测试
 * @Author： dzgu
 * @Date： 2022/5/7 19:22
 */
@Slf4j
@SpringBootTest(classes = RpcClientSpringBootApplication.class)
public class InvokeCompareTest {
    @Autowired
    HelloController helloController;

    private static ExecutorService executor = new ThreadPoolExecutor(512, 512, 5, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1000), new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("xrpc-test-netty-work");
            return t;
        }
    });

    @Test
    public void test() throws InterruptedException {
        int epoch = 20;
        int size = 10000;
        float[] timeEpoch = new float[epoch];
        float allTime = 0;
        // 运行20次
        for (int i = 1; i <= epoch; ++i) {
            final CountDownLatch latch = new CountDownLatch(size);
            final Semaphore semaphore = new Semaphore(600, false);
            long startTime = System.currentTimeMillis();
            // 每次调用size次
            for (int j = 1; j <= size; j++) {
                semaphore.acquire();
                executor.submit(() -> {
                    try {
                        helloController.testSyncBenchMark();
                    } catch (InterruptedException e) {
                    } finally {
                        semaphore.release();
                        latch.countDown();
                    }
                });
            }
            log.info("第" + i + "次运行-->提交任务完成");
            // 阻塞等待调用size次任务完成
            latch.await();
            float epochTime = System.currentTimeMillis() - startTime;
            allTime += epochTime;
            timeEpoch[i-1]=epochTime;
            if (i == 10) {
                Thread.sleep(10000);
            } else {
                Thread.sleep(100);
            }
        }
        float num = (float) epoch * size;
        for (int i = 0; i < timeEpoch.length; i++) {
            log.info("第" + i + "次运行-->耗时：[{}] ms", timeEpoch[i]);
        }
        log.info("平均每次调用-->耗时：[{}] ms", allTime / num);
        //new CountDownLatch(1).await();
    }
}
