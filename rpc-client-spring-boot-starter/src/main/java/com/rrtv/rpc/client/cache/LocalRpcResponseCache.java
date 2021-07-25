package com.rrtv.rpc.client.cache;

import com.rrtv.rpc.client.transport.RpcFuture;
import com.rrtv.rpc.core.common.RpcResponse;
import com.rrtv.rpc.core.protocol.MessageProtocol;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: changjiu.wang
 * @Date: 2021/7/25 15:17
 */
public class LocalRpcResponseCache {

    private static Map<String, RpcFuture<MessageProtocol<RpcResponse>>> requestResponseCache = new ConcurrentHashMap<>();

    public static void add(String reqId, RpcFuture<MessageProtocol<RpcResponse>> future){
        requestResponseCache.put(reqId, future);
    }

    public static void fillResponse(String reqId, MessageProtocol<RpcResponse> messageProtocol){
        // 获取缓存中的 future
        RpcFuture<MessageProtocol<RpcResponse>> future = requestResponseCache.get(reqId);
        // 设置数据
        future.setResponse(messageProtocol);
    }

    public static RpcFuture<MessageProtocol<RpcResponse>> getAndRemove(String reqId){
        RpcFuture<MessageProtocol<RpcResponse>> future = requestResponseCache.get(reqId);
        return future;
    }

}
