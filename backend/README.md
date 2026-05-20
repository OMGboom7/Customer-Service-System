# 客服系统后端

Spring Boot 3 + H2 数据库 + JWT 认证，为 AI 客服系统提供后端 API。

## 构建运行

```bash
# 确保 JDK 17+ 可用
export JAVA_HOME=/path/to/jdk-17

# 构建（跳过测试）
./mvnw package -DskipTests

# 运行
java -jar target/openclaw-spring-test-1.0.0.jar --server.port=8089
```
