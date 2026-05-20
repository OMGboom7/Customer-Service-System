# Claw 客服工作台

基于 OpenClaw Gateway 的 AI 智能客服系统，支持实时对话、文件解析、流式响应、用户管理等功能。

## 系统架构

```
用户 → nginx (8080) → 前端 SPA (React)
                    → 后端 API (Spring Boot :8089) → OpenClaw Gateway (:18789)
                                                      ↓
                                               DeepSeek / Qwen AI
```

| 层 | 技术 | 端口 | 职责 |
|:--|:-----|:---:|:-----|
| 🖥️ **前端** | React 18 + TypeScript + Vite | 5173(dev) / 8080(prod) | 用户界面、聊天交互 |
| 🔄 **反向代理** | nginx | 8080 | 路由 /api/ 到后端、托管前端静态资源 |
| ⚙️ **后端** | Spring Boot 3 + H2 + JPA | **8089** | 认证、对话管理、文件处理、OpenClaw 网关代理 |
| 🌐 **AI 网关** | OpenClaw Gateway | 18789 | 调用 AI 模型处理对话 |
| 🧠 **AI 模型** | DeepSeek V4 Flash + Qwen3.5-Plus | — | 文本对话 + 图片识别 |
| 💾 **数据库** | H2 (文件存储) | — | 用户、对话记录、消息历史 |

## 完整部署

### 前置依赖

- Node.js 20+
- Java 17+ (JDK)
- OpenClaw Gateway（运行中）

### 1. 启动后端

```bash
cd backend  # Spring Boot 项目
cp src/main/resources/application.example.yml application.yml
# 编辑 application.yml 配置 OpenClaw Gateway 地址和 Token
./mvnw package -DskipTests
java -jar target/openclaw-spring-test-1.0.0.jar --server.port=8089
```

后端启动后访问：
- Swagger API 文档: `http://localhost:8089/swagger-ui.html`
- H2 数据库控制台: `http://localhost:8089/h2-console`

### 2. 启动前端

```bash
cd frontend
npm install
cp .env.example .env
# 编辑 .env 配置 Gateway 地址
npm run dev        # 开发模式 → http://localhost:5173
npm run build      # 生产构建 → dist/
```

### 3. 生产部署 (nginx)

```bash
# 构建前端
npm run build

# nginx 配置（已内置）
# /api/ → 反向代理到后端 :8089
# /     → 托管前端静态文件
```

### 4. 登录

默认管理员账号：
- 用户名: `admin`
- 密码: `admin123`

可在后端 `DataSeeder.java` 中修改默认账号。

## 后端 API

| 端点 | 方法 | 说明 |
|:-----|:----|:-----|
| `/api/auth/login` | POST | 用户登录，返回 JWT Token |
| `/api/auth/register` | POST | 注册新用户 |
| `/api/auth/users` | GET | 获取用户列表 |
| `/api/auth/users/{id}` | DELETE | 删除用户 |
| `/api/conversations` | GET | 获取对话列表 |
| `/api/conversations` | POST | 创建新对话 |
| `/api/conversations/{id}/messages` | GET | 获取对话消息 |
| `/api/conversations/{id}/stream` | POST | 流式发送消息 (SSE) |
| `/api/conversations/{id}/context-usage` | GET | 上下文用量 |
| `/api/conversations/{id}/steer` | POST | 转向（中断+重新发送） |
| `/api/conversations/{id}/queue-status` | GET | 排队状态 |
| `/api/file/upload` | POST | 上传文件（支持图片、Office、PDF） |
| `/api/file/upload-base64` | POST | Base64 上传文件 |
| `/api/file/download/{id}` | GET | 下载文件 |
| `/api/file/parse/{id}` | GET | 解析文件内容 |
| `/api/chat/admin/all` | GET | 管理员查看所有用户聊天 |

## 前端功能

- 🔐 JWT 登录认证
- 💬 实时聊天（WebSocket + SSE 流式响应）
- 📜 对话历史加载与管理
- ⚡ 流式响应显示（打字机效果）
- 📝 Markdown 渲染（代码块、表格、图片、公式）
- 📎 文件上传（拖拽上传 + 选择文件）
- 🖼️ 图片上传预览
- 📄 多格式文件解析（Word、Excel、PPT、PDF、TXT）
- 🔄 转向/排队机制
- 📊 上下文用量指示器
- 💅 Markdown 快捷工具栏
- 👑 管理面板（用户管理、聊天审计）
- 🎨 主题切换（亮色/暗色）
- ⏹️ 停止响应按钮
- 📋 代码块复制按钮
- ✏️ 会话自动恢复

## 项目结构

```
src/
├── api/              # Gateway API 封装
├── components/
│   ├── auth/         # 登录表单
│   ├── chat/         # 聊天面板、消息气泡、输入框
│   ├── common/       # 通用组件
│   ├── info/         # 客户信息面板
│   ├── layout/       # 布局组件
│   └── sidebar/      # 会话列表
├── hooks/            # 自定义 Hooks
├── pages/            # 页面组件（Chat、Admin）
├── store/            # Zustand 状态管理
├── styles/           # 全局样式
└── types/            # TypeScript 类型定义

backend/              # Spring Boot 后端项目
├── src/main/java/
│   ├── config/       # 安全、JWT、Swagger 配置
│   ├── controller/   # REST 控制器
│   ├── dto/          # 数据传输对象
│   ├── entity/       # JPA 实体
│   ├── repository/   # 数据访问层
│   ├── service/      # 业务逻辑
│   └── util/         # 工具类（文件解析、编码检测）
└── src/main/resources/
    └── application.yml  # 后端配置
```

## 后端技术栈

- **框架**: Spring Boot 3.2.5
- **数据库**: H2 (文件存储，无需额外安装)
- **ORM**: Spring Data JPA + Hibernate
- **安全**: Spring Security + JWT (bcrypt 密码加密)
- **文档**: SpringDoc OpenAPI (Swagger UI)
- **AI 网关**: OpenClaw Gateway (WebSocket + HTTP)
- **文件解析**: Apache POI (Office)、PDFBox (PDF)
- **编码检测**: 自动检测 GBK/GB2312/UTF-8
- **限流**: Spring Boot Actuator + 速率限制
- **缓存**: 内存缓存 (幂等性保护)

## 配置说明

### 后端配置 (`application.yml`)

```yaml
openclaw:
  gateway:
    base-url: http://127.0.0.1:18789    # OpenClaw Gateway 地址
    token: your-gateway-token              # Gateway 认证 Token
    max-tokens: 128000                     # AI 模型最大 Token
  nginx:
    base-url: http://127.0.0.1:8080       # nginx 内部地址
    public-base-url: https://your.domain   # 外部访问地址
    output-dir: /path/to/nginx/html        # 文件输出目录
```

### 前端配置 (`.env`)

```env
VITE_GATEWAY_URL=http://127.0.0.1:18789   # Gateway WebSocket 地址
VITE_GATEWAY_TOKEN=your-gateway-token
```

## 连接到 OpenClaw

前端通过 WebSocket 直接连接 OpenClaw Gateway，使用标准 RPC 协议：

| 方法 | 用途 |
|------|------|
| `connect` | 网关握手认证 |
| `sessions.list` | 获取会话列表 |
| `sessions.send` | 发送消息 |
| `chat.history` | 获取历史消息 |
| `agents.list` | 获取智能体列表 |

后端通过 HTTP + SSE 与 OpenClaw Gateway 通信，实现流式响应、文件生成等功能。
