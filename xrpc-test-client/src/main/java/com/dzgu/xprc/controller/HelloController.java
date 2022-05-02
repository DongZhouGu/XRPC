package com.dzgu.xprc.controller;

import com.dzgu.xrpc.annotation.RpcAutowired;
import com.dzgu.xprc.entity.Hello;
import org.springframework.stereotype.Component;
import com.dzgu.xprc.service.HelloService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/4/27 0:31
 */
@RestController
public class HelloController {
    @RpcAutowired(version = "1.0")
    private HelloService helloService;

    @GetMapping("/hello")
    public String sayHello() {
        String res = helloService.hello(new Hello("111", "222"));
        return res;
    }

    public void test() throws InterruptedException {
        String hello = helloService.hello(new Hello("111", "222"));
        //如需使用 assert 断言，需要在 VM options 添加参数：-ea
        for (int i = 0; i < 1000; i++) {
            System.out.println(helloService.hello(new Hello("111", "222")));
            Thread.sleep(1000);
        }
    }

}
