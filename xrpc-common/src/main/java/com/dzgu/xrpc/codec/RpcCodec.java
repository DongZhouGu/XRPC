package com.dzgu.xrpc.codec;

import com.dzgu.xrpc.compress.Compress;
import com.dzgu.xrpc.consts.RpcConstants;
import com.dzgu.xrpc.consts.enums.CompressTypeEnum;
import com.dzgu.xrpc.consts.enums.SerializerTypeEnum;
import com.dzgu.xrpc.dto.RpcMessage;
import com.dzgu.xrpc.dto.RpcRequest;
import com.dzgu.xrpc.dto.RpcResponse;
import com.dzgu.xrpc.extension.ExtensionLoader;
import com.dzgu.xrpc.serializer.Serializer;

import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import static com.dzgu.xrpc.consts.RpcConstants.*;

/**
 * 自定义协议
 * 0     1     2     3     4        5     6     7     8    9          10      11      12     13    14   15  16
 * +-----+-----+-----+-----+--------+----+----+----+------+-----------+-------+----- --+-----+-----+----+---+
 * |   magic   code        |version |      full length    | messageType| codec|compress|    RequestId       |
 * +-----------------------+--------+---------------------+-----------+-----------+-----------+------------+
 * |                                                                                                       |
 * |                                         body                                                          |
 * |                                                                                                       |
 * |                                        ... ...                                                        |
 * +-------------------------------------------------------------------------------------------------------+
 * 4B  magic code（魔法数）   1B version（版本）   4B full length（消息长度）    1B messageType（消息类型）
 * 1B compress（压缩类型） 1B codec（序列化类型）    4B  requestId（请求的Id）
 * body（object类型数据）
 *
 * @description: 编解码具体逻辑
 * @Author： dzgu
 * @Date： 2022/4/22 16:48
 */
@Slf4j
public class RpcCodec {
    public static final RpcCodec INSTANCE = new RpcCodec();
    private final Map<Byte, Class<?>> messageTypeMap;

    public RpcCodec() {
        messageTypeMap = new HashMap<>();
        messageTypeMap.put(REQUEST_TYPE, RpcRequest.class);
        messageTypeMap.put(RESPONSE_TYPE, RpcResponse.class);
    }

    /**
     * ByteBuf 解码为RpcMessage
     */
    public Object decode(ByteBuf in) {
        int fullLength = in.readInt();
        byte messageType = in.readByte();
        byte codecType = in.readByte();
        byte compressType = in.readByte();
        int requestId = in.readInt();
        RpcMessage rpcMessage = RpcMessage.builder()
                .codec(codecType)
                .requestId(requestId)
                .compress(compressType)
                .messageType(messageType).build();
        //心跳类型的请求、body 长度 0，不需要decode数据体
        if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
            rpcMessage.setData(RpcConstants.PING);
            return rpcMessage;
        }
        if (messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
            rpcMessage.setData(RpcConstants.PONG);
            return rpcMessage;
        }
        // 获取数据体body的长度
        int bodyLength = fullLength - RpcConstants.HEAD_LENGTH;
        byte[] bs = new byte[bodyLength];
        in.readBytes(bs);
        // 反压缩
        String compressName = CompressTypeEnum.getName(compressType);
        Compress compress = ExtensionLoader.getExtensionLoader(Compress.class)
                .getExtension(compressName);
        bs = compress.decompress(bs);
        // 反序列化
        String codecName = SerializerTypeEnum.getName(rpcMessage.getCodec());
        Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class)
                .getExtension(codecName);
        // 设置decode后的消息体
        Object object = serializer.deserialize(messageTypeMap.get(messageType), bs);
        rpcMessage.setData(object);
        return rpcMessage;
    }

    public ByteBuf encode(RpcMessage rpcMessage, ByteBuf out) {
        try {
            // 4B magic code（魔数）
            out.writeBytes(RpcConstants.MAGIC_NUMBER);
            // 1B version（版本）
            out.writeByte(RpcConstants.VERSION);
            // 4B full length（消息长度）. 先空着，后面填。
            out.writerIndex(out.writerIndex() + 4);
            // 1B messageType（消息类型）
            byte messageType = rpcMessage.getMessageType();
            out.writeByte(messageType);
            // 1B codec（序列化类型）
            out.writeByte(rpcMessage.getCodec());
            // 1B compress（压缩类型）
            out.writeByte(rpcMessage.getCompress());
            // 4B requestId（请求的Id）
            out.writeInt(rpcMessage.getRequestId());
            // 写body，并获取数据长度
            byte[] bodyBytes = null;
            int fullLength = RpcConstants.HEAD_LENGTH;
            if (messageType != RpcConstants.HEARTBEAT_REQUEST_TYPE
                    && messageType != RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
                // 序列化
                String codecName = SerializerTypeEnum.getName(rpcMessage.getCodec());
                Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class)
                        .getExtension(codecName);
                bodyBytes = serializer.serialize(rpcMessage.getData());
                // 压缩
                String compressName = CompressTypeEnum.getName(rpcMessage.getCompress());
                Compress compress = ExtensionLoader.getExtensionLoader(Compress.class)
                        .getExtension(compressName);
                bodyBytes = compress.compress(bodyBytes);
                // 总长度=消息头长度+body
                fullLength += bodyBytes.length;
            }
            if (bodyBytes != null) {
                out.writeBytes(bodyBytes);
            }
            // 记录当前写指针
            int writeIndex = out.writerIndex();
            // 写空出的4B full length（消息长度）
            out.writerIndex(MAGIC_LENGTH + VERSION_LENGTH);
            out.writeInt(fullLength);
            // 写指针复原
            out.writerIndex(writeIndex);
        } catch (Exception e) {
            log.error("Encode request error!", e);
        }
        return out;
    }

}
