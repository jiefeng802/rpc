package com.rrtv.rpc.client.config;

import com.rrtv.rpc.client.proxy.ClientStubProxyFactory;
import com.rrtv.rpc.client.processor.RpcClientProcessor;
import com.rrtv.rpc.core.discovery.DiscoveryService;
import com.rrtv.rpc.core.discovery.ZookeeperDiscoveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @Classname RpcClientAutoConfiguration
 * @Description
 * @Date 2021/7/6 20:48
 * @Created by wangchangjiu
 */
@Configuration
@EnableConfigurationProperties(RpcClientProperties.class)
@Import(value = LoadBalancerImportSelector.class)
public class RpcClientAutoConfiguration {

    @Autowired
    private RpcClientProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public ClientStubProxyFactory clientStubProxyFactory(){
        return new ClientStubProxyFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(value = ClientStubProxyFactory.class)
    public RpcClientProcessor rpcClientProcessor(@Autowired ClientStubProxyFactory clientStubProxyFactory){
        return new RpcClientProcessor(clientStubProxyFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public DiscoveryService discoveryService(){
        return new ZookeeperDiscoveryService(properties.getDiscoveryAddr());
    }

}
