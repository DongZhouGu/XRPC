package com.dzgu.xrpc.compress;

import com.dzgu.xrpc.config.enums.CompressTypeEnum;
import com.dzgu.xrpc.extension.SPI;

/**
 * @description:
 * @Author： dzgu
 * @Date： 2022/4/22 19:06
 */
@SPI
public interface Compress {
    byte[] compress(byte[] bytes);


    byte[] decompress(byte[] bytes);

    CompressTypeEnum getCompressAlgorithm();

}
