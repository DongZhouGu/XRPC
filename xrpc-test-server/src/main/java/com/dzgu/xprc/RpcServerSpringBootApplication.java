package com.dzgu.xprc;

import com.dzgu.xrpc.server.RpcServer;
import com.dzgu.xrpc.server.core.NettyServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/5/2 16:15
 */
@SpringBootApplication
public class RpcServerSpringBootApplication {
    public static void main(String[] args) {
        SpringApplication.run(RpcServerSpringBootApplication.class);
    }
}
