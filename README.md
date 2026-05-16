# Claw 客服工作台

基于 OpenClaw Gateway 的 AI 客服工作台，连接 OpenClaw 智能体进行客服问答。

## 技术栈

- React 18 + TypeScript
- Vite 6 + SWC
- Tailwind CSS 3
- Zustand（状态管理）
- WebSocket（OpenClaw Gateway 协议）

## 快速开始

### 1. 安装依赖

```bash
npm install
```

### 2. 配置网关

复制环境变量模板并填写：

```bash
cp .env.example .env
```

编辑 `.env` 文件：

```
VITE_GATEWAY_URL=http://<你的网关地址>:18789
VITE_GATEWAY_TOKEN=<你的网关 Token>
```

如果使用 Vite 代理（默认支持 localhost），开发环境会自动通过 `/gw` 路径代理 WebSocket 连接到网关。

### 3. 启动开发服务器

```bash
npm run dev
```

默认在 `http://localhost:5173` 启动。

### 4. 构建生产版本

```bash
npm run build
```

产物输出到 `dist/` 目录。

## 功能

- 🔐 网关登录（地址 + Token 认证）
- 💬 实时聊天（WebSocket 通信）
- 📜 消息历史加载
- ⚡ 流式响应显示
- 📝 Markdown 渲染（代码块、表格等）
- 👥 会话列表（搜索、未读标记）
- ℹ️ 客户信息展示
- ⚡ 快捷回复

## 项目结构

```
src/
├── api/           # Gateway API 封装（RPC + WebSocket）
├── components/
│   ├── auth/       # 登录表单
│   ├── chat/       # 聊天面板、消息气泡、输入框
│   ├── common/     # 通用组件（头像、徽章、加载等）
│   ├── info/       # 客户信息面板
│   ├── layout/     # 布局组件
│   └── sidebar/    # 会话列表
├── hooks/          # 自定义 Hooks
├── pages/          # 页面组件
├── store/          # Zustand 状态管理
├── styles/         # 全局样式
└── types/          # TypeScript 类型定义
```

## 连接到 OpenClaw

该系统通过 WebSocket 直接连接 OpenClaw Gateway，使用标准的 RPC 协议：

| 方法 | 用途 |
|------|------|
| `connect` | 网关握手认证 |
| `sessions.list` | 获取会话列表 |
| `sessions.send` | 发送消息 |
| `chat.history` | 获取历史消息 |
| `agents.list` | 获取智能体列表 |
