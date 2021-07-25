package com.rrtv.rpc.client.handler;

import com.rrtv.rpc.client.cache.LocalRpcResponseCache;
import com.rrtv.rpc.core.common.RpcResponse;
import com.rrtv.rpc.core.protocol.MessageProtocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 *  数据响应处理器
 * @Author: changjiu.wang
 * @Date: 2021/7/25 15:09
 */
public class RpcResponseHandler extends SimpleChannelInboundHandler<MessageProtocol<RpcResponse>> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, MessageProtocol<RpcResponse> rpcResponseMessageProtocol) throws Exception {
        String requestId = rpcResponseMessageProtocol.getHeader().getRequestId();
        LocalRpcResponseCache.fillResponse(requestId, rpcResponseMessageProtocol);
    }
}
