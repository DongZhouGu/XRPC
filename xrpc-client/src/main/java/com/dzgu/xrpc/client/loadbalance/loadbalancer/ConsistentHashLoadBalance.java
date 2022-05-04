package com.dzgu.xrpc.client.loadbalance.loadbalancer;

import com.dzgu.xrpc.client.loadbalance.LoadBalance;
import com.dzgu.xrpc.dto.RpcRequest;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * refer to dubbo consistent hash load balance: https://github.com/apache/dubbo/blob/2d9583adf26a2d8bd6fb646243a9fe80a77e65d5/dubbo-cluster/src/main/java/org/apache/dubbo/rpc/cluster/loadbalance/ConsistentHashLoadBalance.java
 */
@Slf4j
public class ConsistentHashLoadBalance implements LoadBalance {
    private final ConcurrentHashMap<String, ConsistentHashSelector> selectors = new ConcurrentHashMap<>();

    @Override
    public String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest) {
        // 用来识别Invoker列表是否发生变更的Hash码
        int identityHashCode = System.identityHashCode(serviceAddresses);
        String rpcServiceName = rpcRequest.getMethodName();
        ConsistentHashSelector selector = selectors.get(rpcServiceName);
        // 若不存在"接口.方法名"对应的选择器，或是Invoker列表已经发生了变更，则初始化一个选择器
        if (selector == null || selector.identityHashCode != identityHashCode) {
            selectors.put(rpcServiceName, new ConsistentHashSelector(serviceAddresses, 160, identityHashCode));
            selector = selectors.get(rpcServiceName);
        }
        return selector.select(rpcServiceName + Arrays.stream(rpcRequest.getParameters()));
    }


    static class ConsistentHashSelector {
        // 存储Hash值与节点映射关系的TreeMap
        private final TreeMap<Long, String> virtualInvokers;
        // 用来识别Invoker列表是否发生变更的Hash码
        private final int identityHashCode;

        ConsistentHashSelector(List<String> invokers, int replicaNumber, int identityHashCode) {
            this.virtualInvokers = new TreeMap<>();
            this.identityHashCode = identityHashCode;
            // 对每个invoker生成replicaNumber个虚拟结点，并存放于TreeMap中
            for (String invoker : invokers) {
                for (int i = 0; i < replicaNumber / 4; i++) {
                    // 根据md5算法为每4个结点生成一个消息摘要，摘要长为16字节128位。
                    byte[] digest = md5(invoker + i);
                    // 随后将128位分为4部分，0-31,32-63,64-95,95-128，并生成4个32位数，存于long中，long的高32位都为0
                    // 并作为虚拟结点的key。
                    for (int h = 0; h < 4; h++) {
                        long m = hash(digest, h);
                        virtualInvokers.put(m, invoker);
                    }
                }
            }
        }

        static byte[] md5(String key) {
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
                byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
                md.update(bytes);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }

            return md.digest();
        }

        // hash算法这里实际上是MD5hash算法的变种，能够最大限度的让所有位都参与运算，
        // 这样hash值才更加均匀；除了MD5hash算法之外，还有FNV1hash
        // number = 0 ，digest数组倒置，右起
        // 数组下标3，先&掩码FF，左移24位（高位字节1）
        // 数组下标2，先&掩码FF，左移16位（高位字节2）
        // 数组下标1，先&掩码FF，左移8位（高位字节3）
        // 数组下标0，先&掩码FF，无需移位，最后一个字节字，
        // 然后这4个字节做或运算，其实就是直接拼起来
        // 最后再跟掩码做与运算
        static long hash(byte[] digest, int idx) {
            return ((long) (digest[3 + idx * 4] & 255) << 24 | (long) (digest[2 + idx * 4] & 255) << 16 | (long) (digest[1 + idx * 4] & 255) << 8 | (long) (digest[idx * 4] & 255)) & 4294967295L;
        }

        /**
         *  选择节点
         */
        public String select(String rpcServiceKey) {
            //生成消息摘要
            byte[] digest = md5(rpcServiceKey);
            //调用hash(digest, 0)，将消息摘要转换为hashCode，这里仅取0-31位来生成HashCode
            return selectForKey(hash(digest, 0));
        }

        /**
         *  根据hashCode选择结点
         */
        public String selectForKey(long hashCode) {
            // 1、先找当前key对应的entity，若不存在，走2
            // 2、找环上hash比key大，且最近的 entry
            // 3、若无则返回null
            Map.Entry<Long, String> entry = virtualInvokers.ceilingEntry(hashCode);
            if (entry == null) {
                // 若找不到，则直接返回环上第一个entry
                entry = virtualInvokers.firstEntry();
            }
            // 返回具体invoker
            return entry.getValue();
        }
    }
}
