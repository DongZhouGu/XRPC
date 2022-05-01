package com.dzgu.xrpc.extension;

/**
 * @description: 持有者。用于单个变量既可以做锁又可以做值
 * @Author： dzgu
 * @Date： 2022/4/22 19:03
 */
public class Holder<T> {

    private volatile T value;

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }
}
