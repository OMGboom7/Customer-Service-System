import type { GatewaySession, SessionListParams, SessionListResult } from '../types/session'
import type { ChatMessage } from '../types/message'
import type { AgentInfo } from '../types/agent'

type RpcHandler = (method: string, params: Record<string, unknown>) => Promise<unknown>

interface ChatHistoryResult {
  sessionKey: string
  sessionId: string
  messages: ChatMessage[]
}

export function createSessionApi(request: RpcHandler) {
  return {
    list(params?: SessionListParams): Promise<SessionListResult> {
      return request('sessions.list', (params || {}) as Record<string, unknown>) as Promise<SessionListResult>
    },

    send(sessionKey: string, message: string): Promise<unknown> {
      return request('sessions.send', { key: sessionKey, message })
    },

    history(params: { sessionKey: string; limit?: number }): Promise<ChatHistoryResult> {
      return request('chat.history', params as Record<string, unknown>) as Promise<ChatHistoryResult>
    },
  }
}

export function createAgentApi(request: RpcHandler) {
  return {
    list(): Promise<{ agents: AgentInfo[] }> {
      return request('agents.list', {}) as Promise<{ agents: AgentInfo[] }>
    },
  }
}

export type SessionApi = ReturnType<typeof createSessionApi>
export type AgentApi = ReturnType<typeof createAgentApi>
