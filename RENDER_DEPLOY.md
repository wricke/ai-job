# Render 部署说明

本项目已经提供 `render.yaml`，可以用 Render Blueprint 创建 Java Web Service。

## 部署步骤

1. 推送代码到 GitHub：

```bash
git push origin master
```

2. 打开 Render Dashboard，选择 `New` -> `Blueprint`。

3. 选择仓库 `wricke/ai-job`。

4. Render 会读取仓库根目录的 `render.yaml`，创建服务 `ai-job-java`。

5. 首次部署时填写环境变量：

```text
DEEPSEEK_API_KEY=你的 DeepSeek API Key
```

6. 部署成功后访问 Render 分配的域名：

```text
https://ai-job-java.onrender.com/
```

## Render 当前配置

```text
Build Command:
bash ./mvnw -DskipTests package

Start Command:
java -jar target/ai-job-agent-0.0.1-SNAPSHOT.jar --spring.profiles.active=render --server.port=$PORT

Health Check:
/actuator/health
```

## 数据库说明

Render profile 使用 H2 文件数据库，适合快速部署测试。

如果要接真实 MySQL，可以在 Render Environment 中配置：

```text
RENDER_DATABASE_URL=jdbc:mysql://主机:端口/数据库名?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
RENDER_DATABASE_DRIVER=com.mysql.cj.jdbc.Driver
RENDER_DATABASE_USERNAME=数据库用户名
RENDER_DATABASE_PASSWORD=数据库密码
```
