package com.dzgu.xrpc.config.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/4/25 21:56
 */
@AllArgsConstructor
@Getter
public enum RpcConfigEnum {
    RPC_CONFIG_PATH("xrpc.properties"),
    ZK_ADDRESS("xrpc.zookeeper.address"),
    NETTY_PORT("xrpc.netty.port");

    private final String propertyValue;
}
