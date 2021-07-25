package com.rrtv.rpc.client.proxy;

import com.rrtv.rpc.client.config.RpcClientProperties;
import com.rrtv.rpc.client.transport.NetClientTransportFactory;
import com.rrtv.rpc.core.common.RpcRequest;
import com.rrtv.rpc.core.common.RpcResponse;
import com.rrtv.rpc.core.common.ServiceInfo;
import com.rrtv.rpc.core.common.ServiceUtil;
import com.rrtv.rpc.core.discovery.DiscoveryService;
import com.rrtv.rpc.core.protocol.MessageHeader;
import com.rrtv.rpc.core.protocol.MessageProtocol;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @Author: changjiu.wang
 * @Date: 2021/7/24 22:52
 */
public class ClientStubInvocationHandler implements InvocationHandler, ApplicationContextAware {

    private ApplicationContext applicationContext;

    private Class<?> calzz;

    private String version;

    public ClientStubInvocationHandler(Class<?> calzz, String version) {
        super();
        this.calzz = calzz;
        this.version = version;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        // 1、获得服务信息
        DiscoveryService discoveryService = applicationContext.getBean(DiscoveryService.class);
        ServiceInfo serviceInfo = discoveryService.discovery(ServiceUtil.serviceKey(this.calzz.getName(), this.version));

        RpcClientProperties properties = applicationContext.getBean(RpcClientProperties.class);

        MessageProtocol<RpcRequest> messageProtocol = new MessageProtocol<>();
        // 设置请求头
        messageProtocol.setHeader(MessageHeader.build(properties.getSerialization()));
        // 设置请求体
        RpcRequest request = new RpcRequest();
        request.setServiceName(ServiceUtil.serviceKey(this.calzz.getName(), this.version));
        request.setMethod(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameters(args);
        messageProtocol.setBody(request);

        // 发送网络请求 拿到结果
        MessageProtocol<RpcResponse> responseMessageProtocol = NetClientTransportFactory.getNetClientTransport().sendRequest(messageProtocol, serviceInfo);

        return "你好,爱是不分";
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
