package service;

import com.dzgu.xrpc.annotation.RpcService;
import entity.Hello;
import lombok.extern.slf4j.Slf4j;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/4/27 0:18
 */
@Slf4j
@RpcService(value = HelloService.class, version = "1.0")
public class HelloServiceImp1 implements HelloService{
    static {
        System.out.println("HelloServiceImpl1被创建");
    }

    @Override
    public String hello(Hello hello) {
        log.info("HelloServiceImpl收到: {}.", hello.getMessage());
        String result = "Hello description is " + hello.getDescription();
        log.info("HelloServiceImpl返回: {}.", result);
        return result;
    }
}
