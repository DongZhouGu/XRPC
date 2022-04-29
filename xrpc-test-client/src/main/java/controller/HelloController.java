package controller;

import com.dzgu.xrpc.annotation.RpcAutowired;
import entity.Hello;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import service.HelloService;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/4/27 0:31
 */
@Component
public class HelloController {
    @RpcAutowired(version = "1.0")
    private HelloService helloService;

    public void test() throws InterruptedException {
        String hello = helloService.hello(new Hello("111", "222"));
        //如需使用 assert 断言，需要在 VM options 添加参数：-ea
        for (int i = 0; i < 100; i++) {
            System.out.println(helloService.hello(new Hello("111", "222")));
            Thread.sleep(1000);
        }
    }

}
