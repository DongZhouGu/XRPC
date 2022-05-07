package com.dzgu.xrpc.dto;

import lombok.*;

import java.io.Serializable;

/**
 * @description: 消息头+消息体
 * @Author： dzgu
 * @Date： 2022/4/22 16:56
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
@Data
public class RpcMessage implements Serializable {
    /**
     * rpc message type
     */
    private byte messageType;
    /**
     * serialization type
     */
    private byte codec;
    /**
     * compress type
     */
    private byte compress;
    /**
     * request id
     */
    private int requestId;
    /**
     * request data
     */
    private Object data;
}
