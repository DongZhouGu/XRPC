package com.dzgu.xrpc.extension;

/**
 * @description:
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
