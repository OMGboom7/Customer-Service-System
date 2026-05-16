import { useAuthStore } from '../store/authStore'
import { useSessionStore } from '../store/sessionStore'
import { useAgentStore } from '../store/agentStore'
import { AppLayout } from '../components/layout/AppLayout'
import { Header } from '../components/layout/Header'
import { SessionList } from '../components/sidebar/SessionList'
import { ChatPanel } from '../components/chat/ChatPanel'
import { CustomerPanel } from '../components/info/CustomerPanel'
import { useSocket } from '../hooks/useSocket'

export function WorkspacePage() {
  const connected = useAuthStore((s) => s.connected)
  const disconnect = useAuthStore((s) => s.disconnect)
  const sessions = useSessionStore((s) => s.sessions)
  const selectedKey = useSessionStore((s) => s.selectedKey)
  const currentAgent = useAgentStore((s) => s.currentAgent)
  const status = useAgentStore((s) => s.status)
  const setStatus = useAgentStore((s) => s.setStatus)

  useSocket()

  const selectedSession = sessions.find((s) => s.key === selectedKey) || null

  return (
    <AppLayout
      header={
        <Header
          agentName={currentAgent?.name || '客服'}
          status={status}
          onlineCount={3}
          queueCount={2}
          onStatusChange={setStatus}
          onLogout={disconnect}
        />
      }
      sidebar={<SessionList />}
      chat={<ChatPanel />}
      info={<CustomerPanel session={selectedSession} />}
    />
  )
}
