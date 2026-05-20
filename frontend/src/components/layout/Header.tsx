import type { AgentStatus } from '../../types/agent'
import { Avatar } from '../common/Avatar'

interface HeaderProps {
  agentName: string
  status: AgentStatus
  onlineCount: number
  queueCount: number
  onStatusChange: (s: AgentStatus) => void
  onLogout: () => void
}

const statusLabels: Record<AgentStatus, string> = {
  online: '在线',
  away: '离开',
  busy: '忙碌',
  offline: '离线',
}

export function Header({ agentName, status, onlineCount, queueCount, onStatusChange, onLogout }: HeaderProps) {
  return (
    <header className="h-14 bg-white border-b border-gray-200 flex items-center justify-between px-4 flex-shrink-0">
      <div className="flex items-center gap-3">
        <div className="flex items-center gap-2">
          <span className="text-lg font-bold text-gray-800">Claw 客服</span>
        </div>
        <div className="h-4 w-px bg-gray-300" />
        <div className="flex items-center gap-3 text-sm text-gray-500">
          <span className="flex items-center gap-1">
            <span className="inline-block w-2 h-2 rounded-full bg-green-500" />
            在线 {onlineCount}
          </span>
          <span className="flex items-center gap-1">
            <span className="inline-block w-2 h-2 rounded-full bg-yellow-500" />
            排队 {queueCount}
          </span>
        </div>
      </div>

      <div className="flex items-center gap-3">
        <select
          value={status}
          onChange={(e) => onStatusChange(e.target.value as AgentStatus)}
          className="text-sm border border-gray-300 rounded px-2 py-1 bg-white focus:outline-none focus:border-blue-400"
        >
          {Object.entries(statusLabels).map(([val, label]) => (
            <option key={val} value={val}>{label}</option>
          ))}
        </select>

        <Avatar name={agentName} size="sm" />
        <span className="text-sm text-gray-700">{agentName}</span>

        <button onClick={onLogout} className="text-sm text-gray-400 hover:text-red-500 transition-colors">
          退出
        </button>
      </div>
    </header>
  )
}
