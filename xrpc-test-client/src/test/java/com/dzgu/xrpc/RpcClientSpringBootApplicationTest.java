package com.dzgu.xrpc;

import com.dzgu.xprc.RpcClientSpringBootApplication;
import com.dzgu.xprc.controller.HelloController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest(classes = RpcClientSpringBootApplication.class)
public class RpcClientSpringBootApplicationTest {
    @Autowired
    HelloController helloController;

    @Test
    public void test() throws InterruptedException {
        helloController.test();
    }
    @Test
    public void testAsync() throws InterruptedException {
        helloController.testAsync();
    }

}
