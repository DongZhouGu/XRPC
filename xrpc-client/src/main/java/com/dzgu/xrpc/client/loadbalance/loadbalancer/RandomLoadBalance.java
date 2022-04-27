package com.dzgu.xrpc.client.loadbalance.loadbalancer;


import com.dzgu.xrpc.client.loadbalance.LoadBalance;
import com.dzgu.xrpc.dto.RpcRequest;

import java.util.List;
import java.util.Random;


public class RandomLoadBalance implements LoadBalance {
    @Override
    public String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest) {
        Random random = new Random();
        return serviceAddresses.get(random.nextInt(serviceAddresses.size()));
    }
}
