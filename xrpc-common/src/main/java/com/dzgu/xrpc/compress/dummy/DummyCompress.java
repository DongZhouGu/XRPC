package com.dzgu.xrpc.compress.dummy;

import com.dzgu.xrpc.compress.Compress;
import com.dzgu.xrpc.config.enums.CompressTypeEnum;

/**
 * @description: 不使用压缩算法
 * @Author： dzgu
 * @Date： 2022/4/29 20:10
 */
public class DummyCompress implements Compress {
    @Override
    public byte[] compress(byte[] bytes) {
        return bytes;
    }

    @Override
    public byte[] decompress(byte[] bytes) {
        return bytes;
    }

    @Override
    public CompressTypeEnum getCompressAlgorithm() {
        return CompressTypeEnum.DUMMY;
    }
}
