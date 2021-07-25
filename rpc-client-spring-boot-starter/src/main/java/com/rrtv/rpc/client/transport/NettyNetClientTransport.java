package com.rrtv.rpc.client.transport;

import com.rrtv.rpc.client.cache.LocalRpcResponseCache;
import com.rrtv.rpc.client.handler.RpcResponseHandler;
import com.rrtv.rpc.core.codec.RpcDecoder;
import com.rrtv.rpc.core.codec.RpcEncoder;
import com.rrtv.rpc.core.common.RpcRequest;
import com.rrtv.rpc.core.common.RpcResponse;
import com.rrtv.rpc.core.common.ServiceInfo;
import com.rrtv.rpc.core.protocol.MessageProtocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * @Classname NettyNetClientTransport
 * @Description
 * @Date 2021/7/7 14:19
 * @Created by wangchangjiu
 */
@Slf4j
public class NettyNetClientTransport implements NetClientTransport {

    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;
    private final RpcResponseHandler handler;


    public NettyNetClientTransport() {
        bootstrap = new Bootstrap();
        eventLoopGroup = new NioEventLoopGroup(4);
        handler = new RpcResponseHandler();
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline()
                                .addLast(new RpcEncoder<>())
                                .addLast(new RpcDecoder())
                                .addLast(handler);
                    }
                });
    }

    @Override
    public MessageProtocol<RpcResponse> sendRequest(MessageProtocol<RpcRequest> protocol, ServiceInfo serviceInfo) throws Exception {
        RpcFuture<MessageProtocol<RpcResponse>> future = new RpcFuture<>();
        LocalRpcResponseCache.add(protocol.getHeader().getRequestId(), future);

        // TCP 连接
        ChannelFuture channelFuture = bootstrap.connect(serviceInfo.getAddress(), serviceInfo.getPort()).sync();
        channelFuture.addListener((ChannelFutureListener) arg0 -> {
            if (channelFuture.isSuccess()) {
                log.info("connect rpc server {} on port {} success.", serviceInfo.getAddress(), serviceInfo.getPort());
            } else {
                log.error("connect rpc server {} on port {} failed.", serviceInfo.getAddress(), serviceInfo.getPort());
                channelFuture.cause().printStackTrace();
                eventLoopGroup.shutdownGracefully();
            }
        });
        // 写入数据
        channelFuture.channel().writeAndFlush(protocol);
        return future.get();
    }
}
