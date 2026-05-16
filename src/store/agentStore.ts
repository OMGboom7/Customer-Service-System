import { create } from 'zustand'
import type { AgentInfo, AgentStatus } from '../types/agent'
import { getAgentApi } from '../api'

interface AgentState {
  agents: AgentInfo[]
  currentAgent: AgentInfo | null
  status: AgentStatus
  loading: boolean
  error: string | null
  fetchAgents: () => Promise<void>
  setStatus: (s: AgentStatus) => void
  setCurrentAgent: (a: AgentInfo) => void
}

export const useAgentStore = create<AgentState>((set) => ({
  agents: [],
  currentAgent: { id: 'agent-1', name: '客服-01', status: 'online' },
  status: 'online',
  loading: false,
  error: null,

  fetchAgents: async () => {
    set({ loading: true, error: null })
    try {
      const api = getAgentApi()
      if (!api) throw new Error('API not initialized')
      const result = await api.list()
      set({ agents: result.agents || [], loading: false })
    } catch (err) {
      set({ error: (err as Error).message, loading: false })
    }
  },

  setStatus: (s) => set({ status: s }),

  setCurrentAgent: (a) => set({ currentAgent: a }),
}))
