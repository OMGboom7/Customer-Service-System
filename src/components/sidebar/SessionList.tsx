import { useSessionStore } from '../../store/sessionStore'
import { SessionItem } from './SessionItem'
import { Loading } from '../common/Loading'

export function SessionList() {
  const { sessions, loading, error, selectedKey, unreadMap, selectSession, searchQuery, setSearchQuery, fetchSessions } = useSessionStore()

  return (
    <div className="h-full flex flex-col">
      <div className="p-3 border-b border-gray-200">
        <div className="relative">
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="搜索会话..."
            className="w-full pl-8 pr-3 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:border-blue-400 bg-gray-50"
          />
          <svg className="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto">
        {loading ? (
          <Loading text="加载会话..." />
        ) : error ? (
          <div className="p-4 text-sm text-red-500">
            <p>{error}</p>
            <button onClick={fetchSessions} className="mt-2 text-blue-500 hover:underline">
              重试
            </button>
          </div>
        ) : sessions.length === 0 ? (
          <div className="p-4 text-center text-sm text-gray-400">
            {searchQuery ? '未找到匹配的会话' : '暂无会话'}
          </div>
        ) : (
          sessions.map((s) => (
            <SessionItem
              key={s.key}
              session={s}
              selected={selectedKey === s.key}
              unread={unreadMap[s.key] || 0}
              onClick={() => selectSession(s.key)}
            />
          ))
        )}
      </div>
    </div>
  )
}
