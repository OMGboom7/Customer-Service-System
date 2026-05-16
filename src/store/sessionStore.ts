import { create } from 'zustand'
import type { GatewaySession } from '../types/session'
import { getSessionApi } from '../api'
import { getSocket } from '../api'

interface SessionState {
  sessions: GatewaySession[]
  loading: boolean
  error: string | null
  selectedKey: string | null
  searchQuery: string
  filterStatus: string
  fetchSessions: () => Promise<void>
  selectSession: (key: string) => void
  setSearchQuery: (q: string) => void
  setFilterStatus: (s: string) => void
  markUnread: (key: string) => void
  clearUnread: (key: string) => void
  unreadMap: Record<string, number>
}

export const useSessionStore = create<SessionState>((set, get) => ({
  sessions: [],
  loading: false,
  error: null,
  selectedKey: null,
  searchQuery: '',
  filterStatus: 'all',
  unreadMap: {},

  fetchSessions: async () => {
    set({ loading: true, error: null })
    try {
      const api = getSessionApi()
      if (!api) throw new Error('API not initialized')
      const result = await api.list({ activeMinutes: 1440, limit: 50 })
      set({ sessions: result.sessions || [], loading: false })
    } catch (err) {
      set({ error: err instanceof Error ? err.message : '获取会话失败', loading: false })
    }
  },

  selectSession: (key) => {
    set({ selectedKey: key })
    get().clearUnread(key)
  },

  setSearchQuery: (q) => set({ searchQuery: q }),
  setFilterStatus: (s) => set({ filterStatus: s }),

  markUnread: (key) => {
    const map = { ...get().unreadMap }
    map[key] = (map[key] || 0) + 1
    set({ unreadMap: map })
  },

  clearUnread: (key) => {
    const map = { ...get().unreadMap }
    delete map[key]
    set({ unreadMap: map })
  },
}))
