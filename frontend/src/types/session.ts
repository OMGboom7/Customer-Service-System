export interface GatewaySession {
  key: string
  sessionId: string
  kind?: string
  chatType?: string
  updatedAt: number
  createdAt?: number
  origin?: {
    provider?: string
    surface?: string
    chatType?: string
    label?: string
    from?: string
    to?: string
  }
  modelProvider?: string
  model?: string
  inputTokens?: number
  outputTokens?: number
  totalTokens?: number
  contextTokens?: number
  hasActiveRun?: boolean
  lastChannel?: string
}

export interface SessionListParams {
  activeMinutes?: number
  limit?: number
  search?: string
  kinds?: string[]
  agentId?: string
}

export interface SessionDefaults {
  modelProvider?: string
  model?: string
  contextTokens?: number
}

export interface SessionListResult {
  sessions: GatewaySession[]
  count?: number
  totalCount?: number
  hasMore?: boolean
  defaults?: SessionDefaults
}
