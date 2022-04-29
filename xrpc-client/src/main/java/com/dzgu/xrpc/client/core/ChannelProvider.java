package com.dzgu.xrpc.client.core;

import com.sun.org.apache.bcel.internal.generic.RETURN;
import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description: channel复用
 * @Author： dzgu
 * @Date： 2022/4/25 9:03
 */
public class ChannelProvider {
    private final Map<String, Channel> channelMap;

    public ChannelProvider() {
        channelMap = new ConcurrentHashMap<>();
    }

    public Channel get(InetSocketAddress inetSocketAddress) {
        String key = inetSocketAddress.toString();
        if (channelMap.containsKey(key)) {
            Channel channel = channelMap.get(key);
            if (channel != null && channel.isActive()) {
                return channel;
            } else {
                channelMap.remove(key);
            }
        }
        return null;
    }

    public void set(InetSocketAddress inetSocketAddress, Channel channel) {
        channelMap.put(inetSocketAddress.toString(), channel);
    }

    public void remove(String address){
        channelMap.remove(address);
    }
}
