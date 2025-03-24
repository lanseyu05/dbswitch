# DBSwitch 轻量级数据库双写切换框架

DBSwitch 是一个轻量级的数据库双写切换框架，支持多种写入模式的动态切换，特别适用于数据迁移和读写分离场景。

## 功能特性

- **多种写入模式支持**：
  - 仅主库（MASTER_ONLY）：所有操作都在主库执行
  - 仅从库（SLAVE_ONLY）：所有操作都在从库执行
  - 主从双写（MASTER_SLAVE）：先写主库，后写从库
  - 从主双写（SLAVE_MASTER）：先写从库，后写主库

- **动态切换**：通过Redis存储写入模式，支持运行时动态切换，无需重启应用

- **自动检测操作类型**：自动识别SELECT/INSERT/UPDATE/DELETE操作

- **消息队列支持**：
  - RocketMQ
  - RabbitMQ

- **数据一致性**：
  - 消息重试机制
  - 幂等性处理
  - 可靠消息传递

- **自动依赖检测**：根据依赖自动选择消息队列类型

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>online.yueyun</groupId>
    <artifactId>dbswitch</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 选择一种消息队列 -->
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>2.2.3</version>
</dependency>
<!-- 或者 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

### 2. 配置数据源

```yaml
# 数据库双写切换配置
dbswitch:
  enabled: true
  # 消息队列配置
  rocketmq:
    topic: dbswitch-topic
    consumer-group: dbswitch-consumer-group
  # 或者
  rabbitmq:
    exchange: dbswitch-exchange
    queue: dbswitch-queue
    routing-key: dbswitch.operation
  # 幂等性配置
  idempotent:
    key-prefix: dbswitch:idempotent:
    expire-hours: 24
  # 写入模式配置
  write-mode:
    redis-key: dbswitch:write-mode
    default: MASTER_ONLY  # 默认写入模式

# Spring配置
spring:
  # 数据源配置
  datasource:
    # 主数据源
    master:
      driver-class-name: com.mysql.cj.jdbc.Driver
      jdbc-url: jdbc:mysql://localhost:3306/db_master
      username: root
      password: root
    # 从数据源
    slave:
      driver-class-name: com.mysql.cj.jdbc.Driver
      jdbc-url: jdbc:mysql://localhost:3306/db_slave
      username: root
      password: root
  # Redis配置
  redis:
    host: localhost
    port: 6379
```

### 3. 创建实体和Mapper

```java
@Data
public class User {
    private Long id;
    private String username;
    private String email;
}

@Mapper
public interface UserMapper {
    void insert(User user);
    User findById(Long id);
    void update(User user);
    void delete(Long id);
}
```

### 4. 使用Service层

```java
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    public void createUser(User user) {
        userMapper.insert(user);  // 根据当前写入模式自动决定使用哪个数据源
    }
    
    public User getUserById(Long id) {
        return userMapper.findById(id);  // 查询操作会根据写入模式选择数据源
    }
    
    public void updateUser(User user) {
        userMapper.update(user);  // 根据写入模式可能会进行双写
    }
    
    public void deleteUser(Long id) {
        userMapper.delete(id);  // 根据写入模式可能会进行双写
    }
}
```

### 5. 动态切换写入模式

```java
@RestController
public class WriteModeController {

    @Autowired
    private WriteModeService writeModeService;

    @PutMapping("/write-mode/{mode}")
    public String updateWriteMode(@PathVariable String mode) {
        WriteMode writeMode = WriteMode.valueOf(mode);
        writeModeService.updateWriteMode(writeMode);
        return "写入模式已更新为: " + mode;
    }
    
    @GetMapping("/write-mode")
    public String getWriteMode() {
        return writeModeService.getCurrentWriteMode().name();
    }
}
```

## 架构设计

### 核心组件

1. **DynamicDataSource**：动态数据源，支持主从切换
2. **DynamicDataSourceSelector**：数据源选择器，根据操作类型和写入模式决定使用哪个数据源
3. **DBSwitchInterceptor**：MyBatis拦截器，拦截SQL操作并路由到正确的数据源
4. **MQProducer**：消息生产者，支持RocketMQ和RabbitMQ
5. **AbstractMQConsumer**：消息消费者基类，提供公共消费逻辑和重试机制

### 工作流程

1. **单库操作模式**（MASTER_ONLY 或 SLAVE_ONLY）：
   - 拦截数据库操作
   - 根据写入模式切换到对应的数据源
   - 执行操作
   - 清除数据源上下文

2. **双写模式**（MASTER_SLAVE 或 SLAVE_MASTER）：
   - 拦截数据库操作
   - 切换到第一个数据源（根据写入模式决定）
   - 执行操作
   - 构建消息并发送到消息队列
   - 消费者接收消息并在第二个数据源执行相同操作

## 注意事项

1. 确保主从数据库的表结构一致
2. 建议使用 MyBatis 作为 ORM 框架
3. 需要配置 Redis 用于幂等性控制和写入模式存储
4. 根据需要选择并配置 RocketMQ 或 RabbitMQ
5. 日志框架使用说明：
   - 框架内部使用 SLF4J + Logback 进行日志记录
   - 通过 Maven Shade 插件重定位到内部包，避免与使用方日志冲突
   - 如果遇到日志问题，可以设置 `logging.level.online.yueyun.dbswitch=DEBUG` 查看详情

## 日志使用示例

如果你的项目使用的是不同的日志框架实现（比如Log4j2），只需要按照Spring Boot的标准方式配置即可，DBSwitch框架不会与之冲突：

```yaml
# 使用Log4j2的配置示例
logging:
  config: classpath:log4j2.xml
```

框架内部的日志配置不会影响应用系统的日志实现。

## 许可证

本项目基于 MIT 许可证开源 