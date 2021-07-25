package com.rrtv.rpc.client.config;

import com.rrtv.rpc.core.balancer.BalanceConstant;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

/**
 * @Classname BalanceImportSelector
 * @Description 负载均衡策略选择器
 * @Date 2021/7/23 17:08
 * @Created by wangchangjiu
 */
public class LoadBalancerImportSelector implements ImportSelector, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        String loadBalanceStrategy = BalanceConstant.DEFAULT_STRATEGY;
        RpcClientProperties properties = applicationContext.getBean(RpcClientProperties.class);
        String balance = properties.getBalance();
        if (!StringUtils.isEmpty(balance) && applicationContext.getBean(balance) != null) {
            // 配置了负载均衡策略
            loadBalanceStrategy = balance;
        }
        return new String[]{loadBalanceStrategy};
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
