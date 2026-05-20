# Claw 客服工作台

基于 OpenClaw Gateway 的 AI 智能客服系统，支持实时对话、文件解析、流式响应、用户管理等功能。**前后端统一仓库管理**。

## 系统架构

```
用户 → nginx (8080) → 前端 SPA (React, dist/)
                    → 后端 API (Spring Boot :8089) → OpenClaw Gateway (:18789)
                                                      ↓
                                               DeepSeek / Qwen AI
```

| 层 | 技术 | 端口 | 职责 |
|:--|:-----|:---:|:-----|
| 🖥️ **前端** | React 18 + TypeScript + Vite + SWC | 5173(dev) / 8080(prod) | 用户界面、聊天交互 |
| 🔄 **反向代理** | nginx | 8080 | 路由 /api/ 到后端、托管前端 SPA |
| ⚙️ **后端** | Spring Boot 3 + H2 + JPA + Spring Security | **8089** | 认证、对话管理、文件处理、OpenClaw 代理 |
| 🌐 **AI 网关** | OpenClaw Gateway | 18789 | 调用 AI 模型处理对话 |
| 🧠 **AI 模型** | DeepSeek V4 Flash + Qwen3.5-Plus | — | 文本对话 + 图片识别 |
| 💾 **数据库** | H2 (文件存储) | — | 用户、对话记录、消息历史 |

## 项目结构

```
├── frontend/                  ← 前端 (React + Vite)
│   ├── src/
│   │   ├── api/              Gateway RPC 封装
│   │   ├── components/
│   │   │   ├── auth/         登录表单
│   │   │   ├── chat/         聊天面板、消息气泡、输入框
│   │   │   ├── common/       通用组件
│   │   │   ├── info/         客户信息面板
│   │   │   ├── layout/       布局组件
│   │   │   └── sidebar/      会话列表
│   │   ├── hooks/            自定义 Hooks
│   │   ├── pages/            页面组件
│   │   ├── store/            Zustand 状态管理
│   │   ├── styles/           全局样式
│   │   └── types/            TypeScript 类型定义
│   ├── index.html
│   ├── vite.config.ts
│   └── package.json
│
├── backend/                  ← 后端 (Spring Boot)
│   ├── pom.xml
│   └── src/main/java/com/openclaw/test/
│       ├── config/           JWT、安全、Swagger 配置
│       ├── controller/       REST 控制器
│       ├── dto/              数据传输对象
│       ├── entity/           JPA 实体
│       ├── repository/       数据访问层
│       ├── service/          业务逻辑
│       └── util/             文件解析、编码检测
│
├── .gitignore
└── README.md
```

## 快速开始

### 前置依赖

- Node.js 20+
- JDK 17+（如 OpenJDK 17）
- OpenClaw Gateway（运行中）
- nginx（生产部署）

### 1. 安装前端依赖

```bash
npm install
cp .env.example .env
# 编辑 .env 配置 OpenClaw Gateway 地址和 Token
```

### 2. 构建后端

```bash
cd backend
# 配置 application.yml（从已有项目或模板复制）
cp /path/to/existing/application.yml src/main/resources/
# 构建
./mvnw package -DskipTests
cd ..
```

### 3. 启动后端

```bash
JAVA_BIN=/path/to/jdk-17/bin/java
$JAVA_BIN -jar backend/target/openclaw-spring-test-1.0.0.jar --server.port=8089
```

启动后访问：
- Swagger 文档: `http://localhost:8089/swagger-ui.html`
- H2 控制台: `http://localhost:8089/h2-console`

### 4. 启动前端开发服务器

```bash
npm run dev
# → http://localhost:5173
```

### 5. 生产构建 & 部署

```bash
npm run build          # 构建前端 → dist/
# 配置 nginx（参考下方说明）
# /api/ → proxy_pass 127.0.0.1:8089
# /     → root /path/to/dist/
```

### 6. 登录

| 用户名 | 密码 | 角色 |
|:------|:----|:----|
| `admin` | `admin123` | 管理员 |
| `user` | `user123` | 普通用户 |

（可在 `backend/DataSeeder.java` 中修改默认账号）

## 后端 API

| 端点 | 方法 | 说明 |
|:-----|:----|:------|
| `/api/auth/login` | POST | 登录，返回 JWT Token |
| `/api/auth/register` | POST | 注册新用户 |
| `/api/auth/users` | GET | 获取用户列表 |
| `/api/auth/users/{id}` | DELETE | 删除用户 |
| `/api/conversations` | GET | 对话列表 |
| `/api/conversations` | POST | 创建新对话 |
| `/api/conversations/{id}/messages` | GET | 消息历史 |
| `/api/conversations/{id}/stream` | POST | 流式发送消息 (SSE) |
| `/api/conversations/{id}/context-usage` | GET | 上下文用量 |
| `/api/conversations/{id}/steer` | POST | 转向（中断+重新发送） |
| `/api/conversations/{id}/queue-status` | GET | 排队状态 |
| `/api/file/upload` | POST | 上传文件 |
| `/api/file/upload-base64` | POST | Base64 上传 |
| `/api/file/download/{id}` | GET | 下载文件 |
| `/api/file/parse/{id}` | GET | 解析文件 |
| `/api/chat/admin/all` | GET | 管理员查看所有聊天 |

## 前端功能

- 🔐 JWT 登录认证
- 💬 实时聊天（WebSocket + SSE 流式响应）
- 📜 对话历史加载与管理
- ⚡ 流式响应显示（打字机效果）
- 📝 Markdown 渲染（代码块、表格、图片）
- 📎 文件上传（拖拽上传）
- 🖼️ 图片上传预览
- 📄 Office/PDF 文件解析
- 🔄 转向/排队机制
- 📊 上下文用量指示器
- 💅 Markdown 快捷工具栏
- 👑 管理面板（用户管理、聊天审计）
- 🎨 主题切换（亮色/暗色）
- ⏹️ 停止响应按钮
- 📋 代码块复制按钮
- ✏️ 会话自动恢复

## 后端技术栈

| 组件 | 技术 | 用途 |
|:----|:-----|:-----|
| 框架 | Spring Boot 3.2.5 | Web 服务 |
| 数据库 | H2 (文件存储) | 零配置，无需安装 |
| ORM | Spring Data JPA + Hibernate | 数据持久化 |
| 安全 | Spring Security + JWT | 认证授权 |
| 文档 | SpringDoc OpenAPI | Swagger UI |
| AI 网关 | OpenClaw Gateway | HTTP + SSE |
| 文件解析 | Apache POI, PDFBox | Office/PDF |
| 编码检测 | 自动检测 | GBK/GB2312/UTF-8 |
| 缓存 | 内存缓存 | 幂等性保护 |

## 配置说明

### 前端 (`.env`)

```env
VITE_GATEWAY_URL=http://127.0.0.1:18789
VITE_GATEWAY_TOKEN=your-gateway-token
```

### 后端 (不包含在仓库中，需自行创建 `backend/src/main/resources/application.yml`)

```yaml
server:
  port: 8089
  forward-headers-strategy: framework

spring:
  datasource:
    url: jdbc:h2:file:./data/csdb
    driver-class-name: org.h2.Driver
    username: sa
  jpa:
    hibernate:
      ddl-auto: update
  h2:
    console:
      enabled: true
      path: /h2-console
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html

openclaw:
  gateway:
    base-url: http://127.0.0.1:18789
    token: your-gateway-token
    max-tokens: 128000
  nginx:
    output-dir: /path/to/nginx/html
```

## OpenClaw 协议

前端通过 WebSocket 直接连接 OpenClaw Gateway，使用标准 RPC 协议：

| 方法 | 用途 |
|------|------|
| `connect` | 网关握手认证 |
| `sessions.list` | 获取会话列表 |
| `sessions.send` | 发送消息 |
| `chat.history` | 获取历史消息 |
| `agents.list` | 获取智能体列表 |

后端通过 HTTP + SSE 与 OpenClaw Gateway 通信，实现流式响应、文件生成等功能。

## 自动部署

本仓库配置了定时任务（每日 01:00 CST），自动检查 GitHub 更新：
- ✅ 有更新 → `git pull` → 构建前端 (`vite build`) + 部署到 nginx
- ✅ 后端需要手动构建 jar 后放到 `backend/target/`
- ✅ 更新分析结果写入 `skills/customer-service-analysis/SKILL.md`
