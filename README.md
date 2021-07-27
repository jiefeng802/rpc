# 手撸RPC

## 基本结构

![RPC项目架构](ziliao/img/rpc.jpg)

RPC 框架包含三个最重要的组件，分别是客户端、服务端和注册中心。在一次 RPC 调用流程中，这三个组件是这样交互的：

- 服务端在启动后，会将它提供的服务列表发布到注册中心，客户端向注册中心订阅服务地址；
- 客户端会通过本地代理模块 Proxy 调用服务端，Proxy 模块收到负责将方法、参数等数据转化成网络字节流；
- 客户端从服务列表中选取其中一个的服务地址，并将数据通过网络发送给服务端；
- 服务端接收到数据后进行解码，得到请求信息；
- 服务端根据解码后的请求信息调用对应的服务，然后将调用结果返回给客户端。


## 模块依赖

### 使用maven聚合工程

- rpc           父工程 
- consumer，服务消费者，是rpc的子工程，依赖于rpc-client-spring-boot-starter。
- provider，服务提供者，是rpc的子工程，依赖于rpc-server-spring-boot-starter。
- provider-api，服务提供者暴露的服务API，是rpc的子工程。
- rpc-client-spring-boot-starter，rpc客户端starter，封装客户端发起的请求过程（动态代理、网络通信）。
- rpc-core，RPC核心依赖，负载均衡策略、消息协议、协议编解码、序列化、请求响应实体、服务注册发现。
- rpc-server-spring-boot-starter，rpc服务端starter，负责发布 RPC 服务，接收和处理 RPC 请求，反射调用服务端。

### 依赖图
![模块依赖](ziliao/img/module_dependency.png)

## 如何使用？
由上面的模块依赖可以知道RPC框架主要是就是以rpc开头的这几个模块，在使用的时候
- 1.消费者（consumer）需要依赖 `rpc-client-spring-boot-starter`。
- 2.服务提供者需要依赖 `rpc-server-spring-boot-starter`。
这样基本就可以了，因为使用了spring boot自动配置，所以消费者和提供者启动的时候都会去加载starter里的spring.factories文件，会自动将需要的bean自动装配到IOC容器中。
- 3.注册中心使用ZK
- 4.消费者和服务提供者需要配置注册中心的地址（默认127.0.0.1:2181）以及服务启动端口，服务提供者还需要配置RPC监听端口。

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
### 1.动态代理
  基于jdk接口的动态代理，客户端不能切换（`rpc-client-spring-boot-starter`模块 proxy 包）  
  原理是服务消费者启动的时候有个 `RpcClientProcessor` bean 的后置处理器，会扫描ioc容器中的bean,如果这个bean有属性被@RpcAutowired修饰，就给属性动态赋代理对象。
 
### 2.服务注册发现
本项目使用ZK做的，实现在 `rpc-core` 模块，`com.rrtv.rpc.core.discovery` 包下面是服务发现，`com.rrtv.rpc.core.register` 包下面是服务注册。  
服务提供者启动后，`RpcServerProvider` 会获取到被 @RpcService 修饰的bean，将服务元数据注册到zk上。  

### 3.负载均衡策略
负载均衡定义在`rpc-core`中，目前支持轮询（FullRoundBalance）和随机（RandomBalance），默认使用随机策略。由`rpc-client-spring-boot-starter`指定。
```
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
```
可以在消费者中配置 `rpc.client.balance=fullRoundBalance` 替换，也可以自己定义，通过实现接口 `LoadBalance`，并将创建的类加入IOC容器即可。
```
@Slf4j
@Component
public class FirstLoadBalance implements LoadBalance {

    @Override
    public ServiceInfo chooseOne(List<ServiceInfo> services) {
        log.info("---------FirstLoadBalance-----------------");
        return services.get(0);
    }
}
```

### 4.自定义消息协议、编解码。  
所谓协议，就是通信双方事先商量好规则，服务端知道发送过来的数据将如何解析。  

#### 4.1自定义消息协议  
+---------------------------------------------------------------+  
| 魔数 2byte | 协议版本号 1byte | 序列化算法 1byte | 报文类型 1byte|  
+---------------------------------------------------------------+  
| 状态 1byte |        消息 ID 8byte     |      数据长度 4byte     |  
+---------------------------------------------------------------+  
|                   数据内容 （长度不定）                         |  
+---------------------------------------------------------------+  

 - 魔数：魔数是通信双方协商的一个暗号，通常采用固定的几个字节表示。魔数的作用是防止任何人随便向服务器的端口上发送数据。
        例如 java Class 文件开头就存储了魔数 0xCAFEBABE，在加载 Class 文件时首先会验证魔数的正确性

 - 协议版本号：随着业务需求的变化，协议可能需要对结构或字段进行改动，不同版本的协议对应的解析方法也是不同的。
 
 - 序列化算法：序列化算法字段表示数据发送方应该采用何种方法将请求的对象转化为二进制，以及如何再将二进制转化为对象，如 JSON、Hessian、Java 自带序列化等。
 
 - 报文类型： 在不同的业务场景中，报文可能存在不同的类型。RPC 框架中有请求、响应、心跳等类型的报文。
 
 - 状态： 状态字段用于标识请求是否正常（SUCCESS、FAIL）。
 
 - 消息ID： 请求唯一ID，通过这个请求ID将响应关联起来。
 
 - 数据长度： 标明数据的长度，用于判断是否是一个完整的数据包
 
 - 数据内容： 请求体内容
 
#### 4.2 编解码
编解码实现在 `rpc-core` 模块，在包 `com.rrtv.rpc.core.codec`下。

4.2.1 如何实现编解码？  
编码利用 netty 的 MessageToByteEncoder 类实现。实现 encode 方法，MessageToByteEncoder 继承 ChannelOutboundHandlerAdapter 。  
编码就是将请求数据写入到 ByteBuf 中。

解码是利用 netty 的 ByteToMessageDecoder 类实现。 实现 decode 方法，ByteToMessageDecoder 继承 ChannelInboundHandlerAdapter。  
解码就是将 ByteBuf 中数据解析出请求的数据。  
解码要注意 TCP 粘包和拆包问题

5.序列化和反序列化  
序列化和反序列化在 `rpc-core` 模块 `com.rrtv.rpc.core.serialization` 包下，提供了 `HessianSerialization` 和 `JsonSerialization` 序列化。  
默认使用 `HessianSerialization` 序列化。
```
  public static SerializationTypeEnum parseByName(String typeName) {
        for (SerializationTypeEnum typeEnum : SerializationTypeEnum.values()) {
            if (typeEnum.name().equalsIgnoreCase(typeName)) {
                return typeEnum;
            }
        }
        return HESSIAN;
    }

    public static SerializationTypeEnum parseByType(byte type) {
        for (SerializationTypeEnum typeEnum : SerializationTypeEnum.values()) {
            if (typeEnum.getType() == type) {
                return typeEnum;
            }
        }
        return HESSIAN;
    }
```

6.网络传输，使用netty


## 环境搭建

- 操作系统：Windows
- 集成开发工具：IntelliJ IDEA
- 项目技术栈：SpringBoot 2.5.2 + JDK 1.8 + Netty 4.1.42.Final
- 项目依赖管理工具：Maven 4.0.0
- 注册中心：Zookeeeper 3.7.0

## 项目测试

- 启动 Zookeeper 服务器：bin/zkServer.cmd
- 启动 provider 模块 ProviderApplication
- 启动 consumer 模块 ConsumerApplication
- 测试：浏览器输入 http://localhost:9090/hello/world?name=hello，成功返回 您好：hello, rpc 调用成功