package com.dzgu.xrpc.serializer.hessian;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import com.dzgu.xrpc.consts.enums.SerializerTypeEnum;
import com.dzgu.xrpc.exception.SerializeException;
import com.dzgu.xrpc.serializer.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * @description: Hessian 序列化
 */
public class HessianSerializer implements Serializer {
    @Override
    public SerializerTypeEnum getSerializerAlgorithm() {
        return SerializerTypeEnum.HESSIAN;
    }

    @Override
    public byte[] serialize(Object object) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            HessianOutput hessianOutput = new HessianOutput(byteArrayOutputStream);
            hessianOutput.writeObject(object);
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new SerializeException("Hessian Serialization failed:", e.getMessage());
        }

    }

    @Override
    public <T> T deserialize(Class<T> clazz, byte[] bytes) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes)) {
            HessianInput hessianInput = new HessianInput(byteArrayInputStream);
            Object o = hessianInput.readObject();
            return clazz.cast(o);
        } catch (Exception e) {
            throw new SerializeException("Hessian Deserialization failed:", e.getMessage());
        }
    }
}
