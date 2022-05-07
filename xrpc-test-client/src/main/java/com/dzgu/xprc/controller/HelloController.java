package com.dzgu.xprc.controller;

import com.dzgu.xprc.entity.Hello;
import com.dzgu.xprc.service.HelloService;
import com.dzgu.xrpc.annotation.RpcAutowired;
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

    @RpcAutowired(version = "1.0", isAsync = true)
    private HelloService helloServiceAsync;

    @GetMapping("/hello")
    public String sayHello() {
        String res = helloService.hello(new Hello("111", "222"));
        return res;
    }

    public void test() throws InterruptedException {
        for (int i = 0; i < 1000; i++) {
            System.out.println(i + "----sync:" + helloService.hello(new Hello("hello", "hello sync")));
            Thread.sleep(1000);
        }
    }

    public void testAsync() throws InterruptedException {
        //如需使用 assert 断言，需要在 VM options 添加参数：-ea
        for (int i = 0; i < 1000; i++) {
            helloServiceAsync.hello(new Hello("hello", "hello async"));
            //RpcContext.setCallback(new ResponseCallback() {
            //    @Override
            //    public void callBack(RpcResponse<Object> result) {
            //        System.out.println("----Async--requetId:" + result.getRequestId() + "--data:" + result.getData());
            //    }
            //
            //    @Override
            //    public void onException(RpcResponse<Object> result, Exception e) {
            //
            //    }
            //});
            //Thread.sleep(1000);
        }
    }

    public void testSyncBenchMark() throws InterruptedException {
        helloService.hello(new Hello("hello", "hello sync"));
    }

    public void testAsyncBenchMark() throws InterruptedException {
        helloServiceAsync.hello(new Hello("hello", "hello async"));
    }
}




