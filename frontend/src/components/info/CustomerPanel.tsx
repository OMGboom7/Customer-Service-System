import { useChat } from '../../hooks/useChat'
import type { GatewaySession } from '../../types/session'

interface CustomerPanelProps {
  session: GatewaySession | null
}

const channelNames: Record<string, string> = {
  whatsapp: 'WhatsApp',
  telegram: 'Telegram',
  discord: 'Discord',
  webchat: '网页',
  wechat: '微信',
}

const quickReplies = [
  { text: '您好，请问有什么可以帮您？', label: '问候' },
  { text: '请稍等，我查询一下。', label: '稍等' },
  { text: '感谢您的耐心等待。', label: '感谢等待' },
  { text: '感谢您的咨询，祝您愉快！', label: '结束语' },
]

export function CustomerPanel({ session }: CustomerPanelProps) {
  const { send } = useChat()
  if (!session) {
    return (
      <div className="h-full flex items-center justify-center text-sm text-gray-400">
        选择会话查看详情
      </div>
    )
  }

  const s = session as unknown as Record<string, string | undefined>
  const name = s.displayName || s.label || session.sessionId?.slice(0, 8) || '未知用户'
  const channel = s.channel || session.origin?.provider || 'unknown'
  const channelDisplay = channelNames[channel.toLowerCase()] || channel
  const sessionId = session.sessionId || session.key
  const since = new Date(session.createdAt || session.updatedAt).toLocaleDateString('zh-CN')

  return (
    <div className="h-full flex flex-col">
      <div className="p-4 border-b border-gray-200">
        <h3 className="text-sm font-semibold text-gray-800 mb-3">客户信息</h3>
        <div className="space-y-2 text-sm">
          <div>
            <span className="text-gray-400">姓名：</span>
            <span className="text-gray-700">{name}</span>
          </div>
          <div>
            <span className="text-gray-400">渠道：</span>
            <span className="text-gray-700">{channelDisplay}</span>
          </div>
          <div>
            <span className="text-gray-400">会话 ID：</span>
            <span className="text-gray-500 text-xs">{sessionId.slice(0, 16)}...</span>
          </div>
          <div>
            <span className="text-gray-400">创建时间：</span>
            <span className="text-gray-700">{since}</span>
          </div>
        </div>
      </div>

      <div className="p-4 border-b border-gray-200">
        <h3 className="text-sm font-semibold text-gray-800 mb-3">快捷回复</h3>
        <div className="space-y-1.5">
          {quickReplies.map((item) => (
            <button
              key={item.label}
              onClick={() => send(item.text)}
              className="w-full text-left px-2 py-1.5 text-xs text-gray-600 bg-gray-50 rounded hover:bg-gray-100 transition-colors"
            >
              <span className="text-gray-400">[{item.label}] </span>
              {item.text}
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
