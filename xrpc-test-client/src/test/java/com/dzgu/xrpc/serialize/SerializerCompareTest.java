package com.dzgu.xrpc.serialize;

import com.dzgu.xrpc.dto.RpcMessage;
import com.dzgu.xrpc.dto.RpcResponse;
import com.dzgu.xrpc.serializer.hessian.HessianSerializer;
import com.dzgu.xrpc.serializer.kryo.KryoSerializer;
import com.dzgu.xrpc.serializer.protostuff.ProtostuffSerializer;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.UUID;

import static com.dzgu.xrpc.consts.RpcConstants.REQUEST_TYPE;
import static com.dzgu.xrpc.consts.enums.CompressTypeEnum.GZIP;
import static com.dzgu.xrpc.consts.enums.RpcResponseCodeEnum.SUCCESS;
import static com.dzgu.xrpc.consts.enums.SerializerTypeEnum.KRYO;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Fork(1)
@Warmup(iterations = 5)
//测量次数,每次测量的持续时间
@Measurement(iterations = 5)
@BenchmarkMode(Mode.Throughput)
public class SerializerCompareTest {
    private static RpcMessage buildMessage() {
        RpcResponse<Object> rpcResponse = RpcResponse.builder()
                .requestId(UUID.randomUUID().toString())
                .message(SUCCESS.getMessage())
                .code(SUCCESS.getCode())
                .data(new String("我是结果，我是结果，我是结果")).build();

        RpcMessage rpcMessage = RpcMessage.builder()
                .requestId(1)
                .compress(GZIP.getCode())
                .messageType(REQUEST_TYPE)
                .codec(KRYO.getCode())
                .data(rpcResponse).build();
        return rpcMessage;

    }

    @Benchmark
    public static void kryoSerializeSizeTest() {
        RpcMessage data = buildMessage();
        KryoSerializer kryoSerializer = new KryoSerializer();
        byte[] serialize = kryoSerializer.serialize(data);
        //System.out.println("kryo's size is " + serialize.length);
        RpcMessage out = kryoSerializer.deserialize(RpcMessage.class, serialize);
        assertEquals(out, data);
    }

    @Benchmark
    public static void hessianSerializeSizeTest() {
        RpcMessage data = buildMessage();
        HessianSerializer hessianSerializer = new HessianSerializer();
        byte[] serialize = hessianSerializer.serialize(data);
        //System.out.println("hessian's size is " + serialize.length);
        RpcMessage out = hessianSerializer.deserialize(RpcMessage.class, serialize);
        assertEquals(out, data);
    }

    @Benchmark
    public static void protostuffSerializeSizeTest() {
        RpcMessage data = buildMessage();
        ProtostuffSerializer protostuffSerializer = new ProtostuffSerializer();
        byte[] serialize = protostuffSerializer.serialize(data);
        //System.out.println("protostuff's size is " + serialize.length);
        RpcMessage out = protostuffSerializer.deserialize(RpcMessage.class, serialize);
        assertEquals(out, data);
    }

    @Test
    public void sizeTest() {
        kryoSerializeSizeTest();
        hessianSerializeSizeTest();
        protostuffSerializeSizeTest();
    }

    @Test
    public void speedTest() throws RunnerException {
        Options options = new OptionsBuilder().include(SerializerCompareTest.class.getName()+".*").build();
        new Runner(options).run();

    }


}
