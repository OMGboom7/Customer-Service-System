export interface RpcRequest {
  method: string
  params: Record<string, unknown>
  id: string
}

export interface RpcResponse {
  id: string
  result?: unknown
  error?: { code: number; message: string }
}

export interface GatewayEvent {
  type: string
  data?: unknown
  sessionKey?: string
  message?: ChatMessageEvent
}

export interface ChatMessageEvent {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: string
  sessionKey: string
}

import type { ChatMessage } from './message'
