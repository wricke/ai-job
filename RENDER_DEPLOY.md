# Render 部署说明

本项目使用 Render Blueprint + Docker 部署 Java/Spring Boot 版本。

## 部署步骤

1. 推送代码到 GitHub：

```bash
git push origin master
```

2. 打开 Render Dashboard，选择 `New` -> `Blueprint`。

3. 选择仓库 `wricke/ai-job`，分支选择 `master`。

4. Blueprint Path 保持默认：

```text
render.yaml
```

5. Render 会读取仓库根目录的 `render.yaml`，创建服务 `ai-job-java`。

6. 首次部署时填写环境变量：

```text
DEEPSEEK_API_KEY=你的 DeepSeek API Key
```

7. 部署成功后访问 Render 分配的域名：

```text
https://ai-job-java.onrender.com/
```

## 当前 Render 配置

Java runtime 不能直接写成 `runtime: java`。Render Blueprint 不支持该 runtime，Java 项目需要使用 Docker。

当前配置：

```yaml
services:
  - type: web
    name: ai-job-java
    runtime: docker
    plan: free
    healthCheckPath: /actuator/health
```

构建和启动逻辑写在 `Dockerfile` 里：

```text
./mvnw -DskipTests package
java -jar /app/app.jar --spring.profiles.active=render --server.port=$PORT
```

## 数据库说明

Render 容器内启用 `render` profile。该 profile 使用 H2 文件数据库，适合快速部署测试。

如果要接真实 MySQL，可以在 Render Environment 中配置：

```text
RENDER_DATABASE_URL=jdbc:mysql://主机:端口/数据库名?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
RENDER_DATABASE_DRIVER=com.mysql.cj.jdbc.Driver
RENDER_DATABASE_USERNAME=数据库用户名
RENDER_DATABASE_PASSWORD=数据库密码
```
