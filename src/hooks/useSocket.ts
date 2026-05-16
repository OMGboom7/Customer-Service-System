import { useEffect, useRef } from 'react'
import { getSocket } from '../api'
import { useChatStore } from '../store/chatStore'
import { useSessionStore } from '../store/sessionStore'
import type { ChatMessage } from '../types/message'

export function useSocket() {
  const selectedKey = useSessionStore((s) => s.selectedKey)
  const fetchSessions = useSessionStore((s) => s.fetchSessions)
  const selectedKeyRef = useRef(selectedKey)
  selectedKeyRef.current = selectedKey

  useEffect(() => {
    const socket = getSocket()
    if (!socket) return

    const onAgent = (event: { type: string; payload?: unknown }) => {
      const payload = event.payload as {
        stream?: string
        data?: { text?: string; delta?: string; phase?: string }
        sessionKey?: string
      }
      if (payload?.stream === 'assistant' && payload?.data?.delta) {
        if (payload.sessionKey === selectedKeyRef.current) {
          useChatStore.getState().appendStream(payload.data.delta)
        }
      }
    }

    const onChat = (event: { type: string; payload?: unknown }) => {
      const payload = event.payload as {
        state?: string
        sessionKey?: string
        message?: { role: string; content: unknown }
      }

      if (payload?.state === 'final' && payload?.message && payload?.sessionKey) {
        const msg: ChatMessage = {
          id: `chat-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
          role: payload.message.role as 'user' | 'assistant',
          content: payload.message.content as string | ChatMessage['content'],
          timestamp: new Date().toISOString(),
          sessionKey: payload.sessionKey,
        }
        if (payload.sessionKey === selectedKeyRef.current) {
          useChatStore.getState().addMessage(msg)
        }
      }
    }

    const onSessionsChanged = () => fetchSessions()

    socket.on('agent', onAgent)
    socket.on('chat', onChat)
    socket.on('sessions.changed', onSessionsChanged)
    fetchSessions()

    return () => {
      socket.off('agent', onAgent)
      socket.off('chat', onChat)
      socket.off('sessions.changed', onSessionsChanged)
    }
  }, [selectedKey, fetchSessions])
}
