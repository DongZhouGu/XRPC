package com.dzgu.xrpc.dto;

import com.dzgu.xrpc.util.JsonUtil;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Objects;

/**
 * @description: 注册服务信息封装
 * @Author： dzgu
 * @Date： 2022/4/21 22:51
 */
@Data
@ToString
@Accessors(chain = true)
public class RpcServiceInfo implements Serializable {
    private String serviceName;
    private String version;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RpcServiceInfo that = (RpcServiceInfo) o;
        return Objects.equals(serviceName, that.serviceName) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceName, version);
    }


}
