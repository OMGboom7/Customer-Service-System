export interface AgentInfo {
  id: string
  name: string
  avatar?: string
  status?: AgentStatus
  agentId?: string
}

export type AgentStatus = 'online' | 'away' | 'busy' | 'offline'
