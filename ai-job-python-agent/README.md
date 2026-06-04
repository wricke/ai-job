# 职途雷达 · AI 岗位匹配助手（Python 版）

这是一个独立的 Python/FastAPI 版本，用于展示更标准的 AI Agent 后端项目架构。

## 技术栈

- FastAPI：REST API
- Pydantic：请求与响应模型校验
- SQLAlchemy：ORM 数据访问
- MySQL：业务数据持久化
- Redis：分析结果缓存，可选
- JWT：登录鉴权
- PyMuPDF / python-docx：PDF、DOCX 简历解析
- DeepSeek Chat Completions：岗位推荐与分析内容生成
- LangGraph：多阶段 Agent 工作流编排，可选；未安装时回退顺序编排

## 项目结构

```text
app/
  api/            路由与依赖
  agents/         Agent 节点与工作流编排
  ai/             DeepSeek 客户端
  core/           配置与安全工具
  db/             数据库连接与初始化
  models/         SQLAlchemy ORM 模型
  schemas/        Pydantic Schema
  services/       业务服务
  utils/          文件解析工具
```

## 启动

```powershell
cd D:\pj1\ai-job-python-agent
pip install -r requirements.txt
$env:DEEPSEEK_API_KEY="你的 DeepSeek Key"
$env:DATABASE_URL="mysql+pymysql://root:密码@localhost:3306/ai_job_agent_py?charset=utf8mb4"
uvicorn app.main:app --reload --port 8000
```

不配置 `DATABASE_URL` 时会使用 SQLite，便于本地演示。

访问：

```text
http://localhost:8000/docs
```

默认体验账号：

```text
用户名：demo
密码：demo123
```

首次启动会自动创建 demo 账号，并写入一份样例简历、三条样例推荐岗位和一条已完成的 Agent 分析报告。新用户注册后也会自动生成同样的入门样例，方便直接查看完整流程。
