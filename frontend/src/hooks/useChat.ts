import { useCallback } from 'react'
import { useChatStore } from '../store/chatStore'
import { useSessionStore } from '../store/sessionStore'

export function useChat() {
  const currentSessionKey = useSessionStore((s) => s.selectedKey)
  const { messages, loading, sending, draft, error, setDraft, fetchHistory, sendMessage, clear } = useChatStore()

  const loadHistory = useCallback(async () => {
    if (currentSessionKey) {
      await fetchHistory(currentSessionKey)
    }
  }, [currentSessionKey, fetchHistory])

  const send = useCallback(async (content: string) => {
    if (currentSessionKey && content.trim()) {
      await sendMessage(currentSessionKey, content.trim())
    }
  }, [currentSessionKey, sendMessage])

  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      if (draft.trim() && !sending) {
        send(draft)
      }
    }
  }, [draft, sending, send])

  return {
    messages,
    loading,
    sending,
    draft,
    error,
    setDraft,
    send,
    loadHistory,
    clear,
    handleKeyDown,
  }
}
