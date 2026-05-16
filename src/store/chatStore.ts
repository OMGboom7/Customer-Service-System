import { create } from 'zustand'
import type { ChatMessage, ContentBlock } from '../types/message'
import { getSessionApi } from '../api'

interface ChatState {
  messages: ChatMessage[]
  streamingContent: string
  loading: boolean
  sending: boolean
  draft: string
  error: string | null
  fetchHistory: (sessionKey: string) => Promise<void>
  sendMessage: (sessionKey: string, content: string) => Promise<void>
  appendStream: (delta: string) => void
  setStreamingContent: (v: string) => void
  setSending: (v: boolean) => void
  addMessage: (msg: ChatMessage) => void
  setDraft: (d: string) => void
  clear: () => void
}

export const useChatStore = create<ChatState>((set) => ({
  messages: [],
  streamingContent: '',
  loading: false,
  sending: false,
  draft: '',
  error: null,

  fetchHistory: async (sessionKey: string) => {
    set({ loading: true, error: null, sending: false, streamingContent: '' })
    try {
      const api = getSessionApi()
      if (!api) throw new Error('API not initialized')
      const result = await api.history({ sessionKey, limit: 50 })
      const messages = (result.messages || []).map((m: ChatMessage, i: number) => ({
        ...m,
        id: m.id || `msg-${i}`,
        timestamp: new Date().toISOString(),
        sessionKey,
      }))
      set({ messages, loading: false })
    } catch (err) {
      set({ error: err instanceof Error ? err.message : '获取历史消息失败', loading: false })
    }
  },

  sendMessage: async (sessionKey: string, content: string) => {
    set({ sending: true, error: null, streamingContent: '' })
    try {
      const api = getSessionApi()
      if (!api) throw new Error('API not initialized')
      await api.send(sessionKey, content)

      const msg: ChatMessage = {
        id: `msg-${Date.now()}`,
        role: 'user',
        content,
        timestamp: new Date().toISOString(),
        sessionKey,
      }
      set((s) => ({ messages: [...s.messages, msg], draft: '' }))
    } catch (err) {
      set({ error: err instanceof Error ? err.message : '发送失败', sending: false })
    }
  },

  appendStream: (delta: string) => {
    set((s) => ({ streamingContent: s.streamingContent + delta }))
  },

  setStreamingContent: (v) => set({ streamingContent: v }),
  setSending: (v) => set({ sending: v }),

  addMessage: (msg) => {
    set((s) => ({ messages: [...s.messages, msg], streamingContent: '', sending: false }))
  },

  setDraft: (d) => set({ draft: d }),
  clear: () => set({ messages: [], streamingContent: '', loading: false, sending: false, draft: '', error: null }),
}))
