package com.dzgu.xrpc.serializer.protostuff;


import com.dzgu.xrpc.config.enums.SerializerTypeEnum;
import com.dzgu.xrpc.serializer.Serializer;
import com.sun.xml.internal.ws.encoding.soap.SerializationException;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.io.ByteArrayInputStream;


/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/4/22 17:23
 */
public class ProtostuffSerializer implements Serializer {
    /**
     * Avoid re applying buffer space every time serialization
     */
    private static final LinkedBuffer BUFFER = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);

    @Override
    public SerializerTypeEnum getSerializerAlgorithm() {
        return SerializerTypeEnum.PROTOSTUFF;
    }

    @Override
    public byte[] serialize(Object obj) {
        Class<?> clazz = obj.getClass();
        Schema schema = RuntimeSchema.getSchema(clazz);
        byte[] bytes;
        try {
            bytes = ProtostuffIOUtil.toByteArray(obj, schema, BUFFER);
        } finally {
            BUFFER.clear();
        }

        return bytes;
    }

    @Override
    public <T> T deserialize(Class<T> clazz, byte[] bytes) {
        Schema<T> schema = RuntimeSchema.getSchema(clazz);
        T obj = schema.newMessage();
        ProtostuffIOUtil.mergeFrom(bytes, obj, schema);
        return obj;
    }
}
