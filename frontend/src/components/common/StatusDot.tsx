import type { AgentStatus } from '../../types/agent'

const statusColors: Record<AgentStatus, string> = {
  online: 'bg-green-500',
  away: 'bg-yellow-500',
  busy: 'bg-red-500',
  offline: 'bg-gray-400',
}

export function StatusDot({ status }: { status: AgentStatus }) {
  return <span className={`inline-block w-2 h-2 rounded-full ${statusColors[status]}`} />
}
