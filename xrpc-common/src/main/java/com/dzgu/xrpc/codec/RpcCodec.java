package com.dzgu.xrpc.codec;

import com.dzgu.xrpc.compress.Compress;
import com.dzgu.xrpc.config.RpcConstants;
import com.dzgu.xrpc.config.enums.CompressTypeEnum;
import com.dzgu.xrpc.config.enums.SerializerTypeEnum;
import com.dzgu.xrpc.dto.RpcMessage;
import com.dzgu.xrpc.dto.RpcRequest;
import com.dzgu.xrpc.dto.RpcResponse;
import com.dzgu.xrpc.extension.ExtensionLoader;
import com.dzgu.xrpc.serializer.Serializer;

import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.dzgu.xrpc.config.RpcConstants.*;

/**
 * custom protocol decoder
 * <pre>
 *   0     1     2     3     4        5     6     7     8         9          10      11     12  13  14   15 16
 *   +-----+-----+-----+-----+--------+----+----+----+------+-----------+-------+----- --+-----+-----+-------+
 *   |   magic   code        |version | full length         | messageType| codec|compress|    RequestId       |
 *   +-----------------------+--------+---------------------+-----------+-----------+-----------+------------+
 *   |                                                                                                       |
 *   |                                         body                                                          |
 *   |                                                                                                       |
 *   |                                        ... ...                                                        |
 *   +-------------------------------------------------------------------------------------------------------+
 * 4B  magic code（魔法数）   1B version（版本）   4B full length（消息长度）    1B messageType（消息类型）
 * 1B compress（压缩类型） 1B codec（序列化类型）    4B  requestId（请求的Id）
 * body（object类型数据）
 *
 * @description:
 * @Author： dzgu
 * @Date： 2022/4/22 16:48
 */
@Slf4j
public class RpcCodec {
    public static final RpcCodec INSTANCE = new RpcCodec();
    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);

    private final Map<Byte, Class<?>> messageTypeMap;

    public RpcCodec() {
        messageTypeMap = new HashMap<>();
        messageTypeMap.put(REQUEST_TYPE, RpcRequest.class);
        messageTypeMap.put(RESPONSE_TYPE, RpcResponse.class);
    }

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

        if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
            rpcMessage.setData(RpcConstants.PING);
            return rpcMessage;
        }
        if (messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
            rpcMessage.setData(RpcConstants.PONG);
            return rpcMessage;
        }
        int bodyLength = fullLength - RpcConstants.HEAD_LENGTH;

        byte[] bs = new byte[bodyLength];
        in.readBytes(bs);
        // decompress the bytes
        String compressName = CompressTypeEnum.getName(compressType);
        Compress compress = ExtensionLoader.getExtensionLoader(Compress.class)
                .getExtension(compressName);
        bs = compress.decompress(bs);
        // deserialize the object
        String codecName = SerializerTypeEnum.getName(rpcMessage.getCodec());
        log.info("decode name: [{}] ", codecName);
        Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class)
                .getExtension(codecName);
        Object tmpValue = serializer.deserialize(messageTypeMap.get(messageType), bs);
        rpcMessage.setData(tmpValue);
        return rpcMessage;
    }

    public ByteBuf encode(RpcMessage rpcMessage, ByteBuf out) {
        try {
            out.writeBytes(RpcConstants.MAGIC_NUMBER);
            out.writeByte(RpcConstants.VERSION);
            // leave a place to write the value of full length
            out.writerIndex(out.writerIndex() + 4);
            byte messageType = rpcMessage.getMessageType();
            out.writeByte(messageType);
            out.writeByte(rpcMessage.getCodec());
            out.writeByte(CompressTypeEnum.GZIP.getCode());
            out.writeInt(ATOMIC_INTEGER.getAndIncrement());
            // build full length
            byte[] bodyBytes = null;
            int fullLength = RpcConstants.HEAD_LENGTH;
            // if messageType is not heartbeat message,fullLength = head length + body length
            if (messageType != RpcConstants.HEARTBEAT_REQUEST_TYPE
                    && messageType != RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
                // serialize the object
                String codecName = SerializerTypeEnum.getName(rpcMessage.getCodec());
                log.info("encode name: [{}] ", codecName);
                Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class)
                        .getExtension(codecName);
                bodyBytes = serializer.serialize(rpcMessage.getData());
                // compress the bytes
                String compressName = CompressTypeEnum.getName(rpcMessage.getCompress());
                Compress compress = ExtensionLoader.getExtensionLoader(Compress.class)
                        .getExtension(compressName);
                bodyBytes = compress.compress(bodyBytes);
                fullLength += bodyBytes.length;
            }

            if (bodyBytes != null) {
                out.writeBytes(bodyBytes);
            }
            int writeIndex = out.writerIndex();
            out.writerIndex(writeIndex - fullLength + RpcConstants.MAGIC_NUMBER.length + 1);
            out.writeInt(fullLength);
            out.writerIndex(writeIndex);
        } catch (Exception e) {
            log.error("Encode request error!", e);
        }
        return out;

    }

}
