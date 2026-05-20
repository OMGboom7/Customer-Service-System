import type { GatewaySession } from '../../types/session'
import { Avatar } from '../common/Avatar'
import { Badge } from '../common/Badge'

interface SessionItemProps {
  session: GatewaySession
  selected: boolean
  unread: number
  onClick: () => void
}

function formatTime(ts: number): string {
  const d = new Date(ts)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return `${d.getMonth() + 1}/${d.getDate()}`
}

function channelIcon(channel?: string): string {
  const map: Record<string, string> = {
    whatsapp: '💬',
    telegram: '✈️',
    discord: '🎮',
    webchat: '🌐',
    wechat: '💚',
  }
  return map[channel?.toLowerCase() || ''] || '💬'
}

export function SessionItem({ session, selected, unread, onClick }: SessionItemProps) {
  const name = session.sessionId?.slice(0, 8) || '未知用户'
  const channel = session.origin?.provider || session.lastChannel || 'unknown'

  return (
    <button
      onClick={onClick}
      className={`w-full px-3 py-2.5 flex items-center gap-2.5 hover:bg-gray-50 transition-colors text-left ${selected ? 'bg-blue-50 border-l-2 border-blue-500' : ''}`}
    >
      <div className="relative">
        <Avatar name={name} size="sm" />
        <span className="absolute -bottom-0.5 -right-0.5 text-xs">{channelIcon(channel)}</span>
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-center justify-between">
          <span className={`text-sm truncate ${selected ? 'font-semibold text-gray-900' : 'text-gray-700'}`}>
            {name}
          </span>
          <span className="text-[10px] text-gray-400 flex-shrink-0 ml-1">
            {session.updatedAt ? formatTime(session.updatedAt) : ''}
          </span>
        </div>
        <div className="flex items-center justify-between mt-0.5">
          <span className="text-xs text-gray-400 truncate">{channel}</span>
        </div>
      </div>
      <Badge count={unread} />
    </button>
  )
}
