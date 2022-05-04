package com.dzgu.xrpc.client.loadbalance.loadbalancer;

import com.dzgu.xrpc.client.loadbalance.LoadBalance;
import com.dzgu.xrpc.dto.RpcRequest;

import java.util.List;

/**
 * @description: 轮询
 */
public class FullRoundBalance implements LoadBalance {
    private int index;
    @Override
    public String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest) {
        if(serviceAddresses.size()==index){
            index=0;
        }
        return serviceAddresses.get(index++);
    }
}
