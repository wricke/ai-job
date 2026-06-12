# 职途雷达 · AI 岗位匹配助手

这是一个面向后端实习求职的 AI 岗位匹配助手。用户注册登录后上传简历，系统会推荐适合投递的岗位方向并显示适配度，同时给出适配理由、短板、搜索关键词、准备建议和分析报告。

## 主要功能

- 用户系统：支持注册、登录、退出，每个用户只看到自己的简历、推荐岗位和分析历史。
- 上传简历：支持 PDF、DOCX 和常见图片格式。
- 岗位推荐：根据简历推荐岗位方向，并自动放入“推荐岗位”下拉框。
- Agent 分析：选择推荐岗位后，生成匹配度、优势短板、简历优化建议和面试准备问题。
- 历史记录：查看每次分析结果和 Agent 执行轨迹。

## 技术栈

- Java 17
- Spring Boot 3.5
- Spring Security
- MyBatis
- MySQL
- PDFBox
- Apache POI
- Redis 依赖预留
- DeepSeek Chat Completions
- 原生 HTML/CSS/JavaScript

## 快速启动

```powershell
cd D:\pj1\ai-job-agent
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="你的 MySQL 密码"
$env:DEEPSEEK_API_KEY="你的 DeepSeek API Key"
.\mvnw.cmd spring-boot:run
```

访问：

- 控制台：`http://localhost:8080`
- 健康检查：`http://localhost:8080/actuator/health`
- Agent 信息：`http://localhost:8080/api/agent/info`

首次访问会进入登录/注册页。注册后上传简历，生成推荐岗位，再选择推荐岗位启动分析。

## GitHub Pages 体验版

仓库内置了一个纯静态体验页：

```text
docs/index.html
```

启用方式：

1. 推送代码到 GitHub。
2. 打开仓库 `Settings -> Pages`。
3. `Source` 选择 `Deploy from a branch`。
4. `Branch` 选择 `main` 或 `master`，目录选择 `/docs`。
5. 点击 `Save`。

启用后访问：

```text
https://wricke.github.io/ai-job/
```

这个体验版用于展示完整交互流程，不连接真实后端、数据库或大模型接口。真实功能请运行 Spring Boot 服务。

## 配置说明

MySQL 默认配置：

```yaml
spring:
  datasource:
    url: ${MYSQL_URL:jdbc:mysql://localhost:3306/ai_job_agent?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true}
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD:}
```

DeepSeek 配置：

```yaml
agent:
  ai:
    base-url: https://api.deepseek.com
    api-key: ${DEEPSEEK_API_KEY:}
    model: deepseek-v4-flash
```

建表脚本在 `src/main/resources/schema.sql`。项目启动时会自动初始化表结构，并为旧数据库补充 `user_id` 字段。

## 核心接口

认证：

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
POST /api/auth/logout
```

简历：

```text
GET    /api/resumes
POST   /api/resumes/upload
GET    /api/resumes/{id}
DELETE /api/resumes/{id}
```

岗位推荐：

```text
POST /api/resumes/{id}/job-recommendations
```

分析：

```text
GET  /api/analyses
POST /api/analyses
GET  /api/analyses/{id}
POST /api/analyses/{id}/run
```

## 简历项目写法

项目名称：职途雷达 · AI 岗位匹配助手

项目简介：基于 Spring Boot、Spring Security、MyBatis、MySQL 和 DeepSeek 大模型接口实现的 AI 求职分析系统，支持用户登录、简历解析、岗位推荐、匹配度评分、优化建议和面试准备，帮助用户快速判断适合投递的岗位方向。

负责内容：

- 使用 Spring Security + BCrypt 实现注册登录和用户数据隔离，保证简历、推荐岗位和分析历史按用户维度管理。
- 设计用户、简历、岗位、分析任务和分析缓存等核心表结构，并基于 MyBatis 完成数据访问层。
- 接入 DeepSeek `deepseek-v4-flash`，实现岗位推荐、匹配度分析、简历优化建议和面试准备内容生成。
- 将求职分析拆分为简历解析、岗位解析、匹配评分、优化建议、面试准备等 Agent 步骤。

## 验证命令

```powershell
.\mvnw.cmd test
```
