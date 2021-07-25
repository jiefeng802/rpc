package com.rrtv.rpc.client.transport;

/**
 * @Author: changjiu.wang
 * @Date: 2021/7/25 15:12
 */
public class NetClientTransportFactory {

    public static NetClientTransport getNetClientTransport(){
        return new NettyNetClientTransport();
    }


}
