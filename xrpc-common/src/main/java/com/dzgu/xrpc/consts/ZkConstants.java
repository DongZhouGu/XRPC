package com.dzgu.xrpc.consts;

/**
 * @description: 常量
 * @Author： dzgu
 * @Date： 2022/4/21 0:15
 */
public interface ZkConstants {
    int ZK_SESSION_TIMEOUT = 5000;
    int ZK_CONNECTION_TIMEOUT = 5000;

    String ZK_REGISTRY_PATH = "/registry";
    String ZK_DATA_PATH = ZK_REGISTRY_PATH + "/data";

    String ZK_NAMESPACE = "xrpc";
}
