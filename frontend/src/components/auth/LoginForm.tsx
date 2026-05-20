import { useState } from 'react'
import { useAuthStore } from '../../store/authStore'

export function LoginForm() {
  const [url, setUrl] = useState(useAuthStore.getState().gatewayUrl || 'https://oc.p07.icu')
  const [token, setToken] = useState(useAuthStore.getState().token || '')
  const storeConnect = useAuthStore((s) => s.connect)
  const connecting = useAuthStore((s) => s.connecting)
  const error = useAuthStore((s) => s.error)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!url.trim() || !token.trim()) return
    try {
      await storeConnect(url.trim(), token.trim())
    } catch {
      // error is set in store
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <div className="bg-white rounded-lg shadow-md p-8 w-full max-w-sm">
        <div className="text-center mb-6">
          <h1 className="text-2xl font-bold text-gray-800">Claw 客服工作台</h1>
          <p className="text-sm text-gray-500 mt-1">连接到 OpenClaw Gateway</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">网关地址</label>
            <input
              type="text"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder="https://oc.p07.icu"
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Token</label>
            <input
              type="password"
              value={token}
              onChange={(e) => setToken(e.target.value)}
              placeholder="请输入 Gateway Token"
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
            />
          </div>

          {error && (
            <div className="p-2 bg-red-50 border border-red-200 rounded text-sm text-red-600">{error}</div>
          )}

          <button
            type="submit"
            disabled={connecting}
            className={`w-full py-2 px-4 rounded-md text-sm font-medium transition-all ${
              connecting
                ? 'bg-gray-400 text-white cursor-not-allowed'
                : 'bg-blue-600 text-white hover:bg-blue-700'
            }`}
          >
            {connecting ? (
              <span className="flex items-center justify-center gap-2">
                <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                </svg>
                连接中...
              </span>
            ) : '连接'}
          </button>
        </form>
      </div>
    </div>
  )
}
