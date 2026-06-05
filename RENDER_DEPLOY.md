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
6. 在 Environment Variables 中填写：

```text
DEEPSEEK_API_KEY=你的 DeepSeek API Key
MYSQL_URL=jdbc:mysql://数据库主机:3306/ai_job_agent?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
MYSQL_USERNAME=数据库用户名
MYSQL_PASSWORD=数据库密码
```

7. 部署成功后访问 Render 分配的域名：

```text
https://ai-job-java.onrender.com/
```

## 当前 Render 配置

Render Blueprint 使用 Docker 部署：

```yaml
services:
  - type: web
    name: ai-job-java
    runtime: docker
    plan: free
    healthCheckPath: /actuator/health
```

构建和启动逻辑写在 `Dockerfile` 中：

```text
./mvnw -DskipTests package
java -jar /app/app.jar --spring.profiles.active=render --server.port=$PORT
```

## 数据持久化

线上 `render` profile 已改为必须连接外部 MySQL。账号、简历、岗位、分析任务、AI 分析结果都会写入 MySQL 表中，不再写入 Render 容器内的临时 H2 文件数据库。

如果没有配置 `MYSQL_URL`、`MYSQL_USERNAME`、`MYSQL_PASSWORD`，服务会启动失败。这是为了避免数据被写入临时文件后在 Render 重启或重新部署时丢失。

如果你的数据库服务商不允许应用自动创建数据库，请先在 MySQL 控制台中手动创建 `ai_job_agent` 数据库。
