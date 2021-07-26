# 手撸RPC

## 基本结构

![RPC项目架构](../ziliao/img/rpc.jpg)

RPC 框架包含三个最重要的组件，分别是客户端、服务端和注册中心。在一次 RPC 调用流程中，这三个组件是这样交互的：

- 服务端在启动后，会将它提供的服务列表发布到注册中心，客户端向注册中心订阅服务地址；
- 客户端会通过本地代理模块 Proxy 调用服务端，Proxy 模块收到负责将方法、参数等数据转化成网络字节流；
- 客户端从服务列表中选取其中一个的服务地址，并将数据通过网络发送给服务端；
- 服务端接收到数据后进行解码，得到请求信息；
- 服务端根据解码后的请求信息调用对应的服务，然后将调用结果返回给客户端。


## 模块依赖

使用maven聚合工程

- rpc           父工程 
- consumer，服务消费者，是rpc的子工程，依赖于rpc-client-spring-boot-starter。
- provider，服务提供者，是rpc的子工程，依赖于rpc-server-spring-boot-starter。
- provider-api，服务提供者暴露的服务API，是rpc的子工程。
- rpc-client-spring-boot-starter，rpc客户端starter，封装客户端发起的请求过程（动态代理、网络通信）。
- rpc-core，RPC核心依赖，负载均衡策略、消息协议、协议编解码、序列化、请求响应实体、服务注册发现。
- rpc-server-spring-boot-starter，rpc服务端starter，负责发布 RPC 服务，接收和处理 RPC 请求，反射调用服务端。

## 如何使用？
由上面的模块依赖可以知道RPC框架主要是就是以rpc开头的这几个模块，在使用的时候
1.消费者（consumer）需要依赖rpc-client-spring-boot-starter，
2.服务提供者需要依赖rpc-server-spring-boot-starter。
这样基本就可以了，因为使用了spring boot自动配置，所以消费者和提供者启动的时候都会去加载starter里的spring.factories文件，会自动将需要的bean自动装配到IOC容器中。
3.注册中心使用ZK
4.消费者和服务提供者需要配置注册中心的地址（默认127.0.0.1:2181）以及服务启动端口，服务提供者还需要配置RPC监听端口。

## 发布服务和消费服务
- 对于发布的服务需要使用 @RpcService 注解标识，复合注解，基于 @Service
```
@RpcService(interfaceType = HelloWordService.class, version = "1.0")
public class HelloWordServiceImpl implements HelloWordService {

    @Override
    public String sayHello(String name) {
        return String.format("您好：%s, rpc 调用成功", name);
    }

}
```

- 消费服务需要使用 @RpcAutowired 注解标识，复合注解，基于 @Autowired
```
 @RpcAutowired(version = "1.1")
  private HelloWordService helloWordService;
```

## 本项目实现哪些组件
1.动态代理，基于jdk接口的动态代理，客户端不能切换（rpc-client-spring-boot-starter模块 proxy 包）
  - 原理是服务消费者启动的时候有个 `RpcClientProcessor` bean 的后置处理器，会扫描ioc容器中的bean,如果这个bean有属性被@RpcAutowired修饰，就给属性动态赋代理对象。
 
2.服务注册发现，本项目使用ZK做的，实现在 rpc-core 模块，
