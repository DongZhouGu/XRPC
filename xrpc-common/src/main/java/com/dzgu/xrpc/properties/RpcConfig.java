package com.dzgu.xrpc.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @description: Rpc配置类
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
    private String registerAddress = "127.0.0.1:2181";

    /**
     * 注册中心
     */
    private String register = "zookeeper";


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
     * 服务代理类型 reflect：
     */
    private String proxyType = "cglib";
}
