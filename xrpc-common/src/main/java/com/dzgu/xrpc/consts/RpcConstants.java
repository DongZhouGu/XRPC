package com.dzgu.xrpc.consts;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description: RPC 常量
 * @Author： dzgu
 * @Date： 2022/4/22 16:20
 */
public interface RpcConstants {
    /**
     * 魔术，验证通信双方的合法性
     */
    byte[] MAGIC_NUMBER = {(byte) 'x', (byte) 'r', (byte) 'p', (byte) 'c'};
    byte VERSION = 1;
    /**
     * 请求 Id
     */
    AtomicInteger REQUEST_ID = new AtomicInteger(0);

    /**
     * 魔法数字长度
     */
    int MAGIC_LENGTH = 4;

    /**
     * 版本长度
     */
    int VERSION_LENGTH = 1;
    /**
     * 总长度字段的长度
     */
    int FULL_LENGTH_LENGTH = 4;


    /**
     * 消息类型
     */
    byte REQUEST_TYPE = 1;
    byte RESPONSE_TYPE = 2;
    byte HEARTBEAT_REQUEST_TYPE = 3;
    byte HEARTBEAT_RESPONSE_TYPE = 4;
    int HEAD_LENGTH = 16;
    String PING = "ping";
    String PONG = "pong";
    int MAX_FRAME_LENGTH = 8 * 1024 * 1024;
    /**
     *  客户端重连次数
     */
    int MAX_RETRY = 5;

}
