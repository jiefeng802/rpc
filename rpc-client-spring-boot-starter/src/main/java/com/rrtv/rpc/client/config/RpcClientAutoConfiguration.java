package com.rrtv.rpc.client.config;

import com.rrtv.rpc.client.proxy.ClientStubProxyFactory;
import com.rrtv.rpc.client.processor.RpcClientProcessor;
import com.rrtv.rpc.core.balancer.FullRoundBalance;
import com.rrtv.rpc.core.balancer.LoadBalance;
import com.rrtv.rpc.core.balancer.RandomBalance;
import com.rrtv.rpc.core.discovery.DiscoveryService;
import com.rrtv.rpc.core.discovery.ZookeeperDiscoveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/**
 * @Classname RpcClientAutoConfiguration
 * @Description
 * @Date 2021/7/6 20:48
 * @Created by wangchangjiu
 */
@Configuration
public class RpcClientAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "rpc.client")
    public RpcClientProperties ppcClientProperties() {
        return new RpcClientProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public ClientStubProxyFactory clientStubProxyFactory() {
        return new ClientStubProxyFactory();
    }


    @Primary
    @Bean(name = "loadBalance")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.client", name = "balance", havingValue = "randomBalance", matchIfMissing = true)
    public LoadBalance randomBalance() {
        return new RandomBalance();
    }

    @Bean(name = "loadBalance")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.client", name = "balance", havingValue = "fullRoundBalance")
    public LoadBalance loadBalance() {
        return new FullRoundBalance();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({RpcClientProperties.class, LoadBalance.class})
    public DiscoveryService discoveryService(@Autowired RpcClientProperties properties,
                                             @Autowired LoadBalance loadBalance) {
        return new ZookeeperDiscoveryService(properties.getDiscoveryAddr(), loadBalance);
    }

    @Bean
    @ConditionalOnMissingBean
    public RpcClientProcessor rpcClientProcessor(@Autowired ClientStubProxyFactory clientStubProxyFactory,
                                                 @Autowired DiscoveryService discoveryService,
                                                 @Autowired RpcClientProperties properties) {
        return new RpcClientProcessor(clientStubProxyFactory, discoveryService, properties);
    }

}
