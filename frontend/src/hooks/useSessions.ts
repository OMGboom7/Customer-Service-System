import { useCallback } from 'react'
import { useSessionStore } from '../store/sessionStore'

export function useSessions() {
  const { sessions, loading, error, selectedKey, searchQuery, filterStatus, fetchSessions, selectSession, setSearchQuery } = useSessionStore()

  const filteredSessions = sessions.filter((s) => {
    if (searchQuery) {
      const q = searchQuery.toLowerCase()
      const extra = s as unknown as Record<string, string | undefined>
      const label = (extra.displayName || extra.label || s.sessionId || s.key || '').toLowerCase()
      if (!label.includes(q)) return false
    }
    return true
  })

  return {
    sessions: filteredSessions,
    allSessions: sessions,
    loading,
    error,
    selectedKey,
    searchQuery,
    filterStatus,
    fetchSessions,
    selectSession,
    setSearchQuery,
  }
}
