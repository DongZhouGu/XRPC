package com.dzgu.xrpc.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @description: Rpc配置
 * @Author： dzgu
 * @Date： 2022/5/1 12:03
 */
@Data
@ConfigurationProperties(prefix = "xrpc")
public class RpcConfig {
    /**
     * 是否启用rpc 默认启用
     */
    private boolean enable = true;

    /**
     * 注册中心地址
     */
    private String registerAddress = "zk://10.20.153.10:6379?backup=10.20.153.11:6379,10.20.153.12:6379";

    /**
     * 服务暴露端口
     */
    private Integer serverPort = 9999;

    /**
     * 序列化类型
     */
    private String serializer = "kryo";

    /**
     * 压缩算法
     */
    private String compress = "gzip";

    /**
     * 负载均衡算法
     */
    private String loadBalance = "random";

    /**
     * 容错策略
     */
    private String faultTolerant = "retry";

    /**
     * 重试次数，只有容错策略是 'retry' 的时候才有效
     */
    private Integer retryTimes = 3;


    /**
     * 服务代理类型 reflect： 反射调用 javassist： 字节码生成代理类调用
     */
    private String proxyType = "javassist";
}
