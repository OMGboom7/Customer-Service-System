# Claw 客服工作台

基于 OpenClaw Gateway 的 AI 智能客服系统，支持实时对话、文件解析、流式响应、用户管理等功能。**前后端统一仓库管理**。

## 系统架构

```
用户 → nginx (8080) → 前端 SPA (React, frontend/dist/)
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
│   │   ├── components/       组件（auth/chat/common/info/layout/sidebar）
│   │   ├── hooks/            自定义 Hooks
│   │   ├── pages/            页面组件
│   │   ├── store/            Zustand 状态管理
│   │   ├── styles/           全局样式
│   │   └── types/            TypeScript 类型定义
│   ├── index.html
│   ├── vite.config.ts
│   ├── package.json
│   └── .env.example
│
├── backend/                  ← 后端 (Spring Boot)
│   ├── pom.xml
│   └── src/main/
│       ├── java/.../config/      JWT、安全、Swagger 配置
│       ├── java/.../controller/  REST 控制器
│       ├── java/.../dto/         数据传输对象
│       ├── java/.../entity/      JPA 实体
│       ├── java/.../repository/  数据访问层
│       ├── java/.../service/     业务逻辑
│       ├── java/.../util/        文件解析、编码检测
│       └── resources/
│           ├── application.example.yml  ← 配置样例
│           └── application.yml          ← 真实配置（已 gitignore）
│
├── .gitignore
└── README.md
```

## 快速开始

### 前置依赖

- Node.js 20+
- JDK 17+（如 OpenJDK 17）
- OpenClaw Gateway（运行中，端口 18789）
- nginx（生产部署用）

### 1. 构建后端

```bash
cd backend
# 从样例创建配置文件
cp src/main/resources/application.example.yml src/main/resources/application.yml
# 编辑 application.yml，填入你的 Gateway Token 和地址
# 构建（需要 Maven，可使用项目内 mvnw）
./mvnw package -DskipTests
cd ..
```

### 2. 启动后端

```bash
JAVA_BIN=/path/to/jdk-17/bin/java
$JAVA_BIN -jar backend/target/openclaw-spring-test-1.0.0.jar --server.port=8089
```

启动后访问：
- Swagger 文档: `http://localhost:8089/swagger-ui.html`
- H2 控制台: `http://localhost:8089/h2-console`

### 3. 安装前端依赖 & 配置

```bash
cd frontend
npm install
cp .env.example .env
# 编辑 .env 配置 OpenClaw Gateway 地址和 Token
cd ..
```

### 4. 启动前端开发服务器

```bash
cd frontend
npm run dev
# → http://localhost:5173
```

### 5. 生产构建

```bash
cd frontend
npm run build          # 构建 → frontend/dist/
# 将 dist/ 复制到 nginx 托管目录
cp -r dist/* /path/to/nginx/html/
```

### 6. 登录

| 用户名 | 密码 | 角色 |
|:------|:----|:----|
| `admin` | `admin123` | 管理员 |
| `user` | `user123` | 普通用户 |

可在 `backend/.../config/DataSeeder.java` 中修改默认账号。

## 后端 API

| 端点 | 方法 | 说明 |
|:-----|:----|:------|
| `/api/auth/login` | POST | 登录，返回 JWT Token |
| `/api/auth/register` | POST | 注册新用户 |
| `/api/auth/users` | GET | 用户列表 |
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
| 数据库 | H2 (文件存储) | 零配置无需安装 |
| ORM | Spring Data JPA + Hibernate | 数据持久化 |
| 安全 | Spring Security + JWT | 认证授权 |
| 文档 | SpringDoc OpenAPI | Swagger UI |
| AI 网关 | OpenClaw Gateway | HTTP + SSE |
| 文件解析 | Apache POI + PDFBox | Office/PDF |
| 编码检测 | 自动检测 | GBK/GB2312/UTF-8 |
| 缓存 | 内存缓存 | 幂等性保护 |

## 配置说明

### 前端 (`frontend/.env`)

```env
VITE_GATEWAY_URL=http://127.0.0.1:18789
VITE_GATEWAY_TOKEN=your-gateway-token
```

### 后端 (`backend/src/main/resources/application.yml`)

从 `application.example.yml` 复制并按需修改：

```yaml
openclaw:
  gateway:
    base-url: http://127.0.0.1:18789    # Gateway 地址
    token: your-gateway-token             # Gateway 认证 Token
    max-tokens: 128000
  nginx:
    base-url: http://127.0.0.1:8080
    public-base-url: https://your-domain.cn
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

```
git fetch origin → 有更新？→ git pull
                          → cd frontend && npm install --include=dev && vite build
                          → cp dist/* → nginx html/
                          → 重启后端 jar（如已编译）
                          → 分析变更写入 skill
```
