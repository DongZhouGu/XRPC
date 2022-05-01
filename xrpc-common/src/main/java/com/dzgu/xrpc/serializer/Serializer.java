package com.dzgu.xrpc.serializer;

import com.dzgu.xrpc.consts.enums.SerializerTypeEnum;
import com.dzgu.xrpc.extension.SPI;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/4/22 17:01
 */
@SPI
public interface Serializer {

    SerializerTypeEnum getSerializerAlgorithm();

    byte[] serialize(Object object);

    <T> T deserialize(Class<T> clazz, byte[] bytes);
}
