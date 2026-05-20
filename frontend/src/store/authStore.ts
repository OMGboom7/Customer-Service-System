import { create } from 'zustand'
import { initGateway, getGatewayUrl, getGatewayToken, getSocket } from '../api'

interface AuthState {
  connected: boolean
  connecting: boolean
  gatewayUrl: string
  token: string
  error: string | null
  connect: (url?: string, token?: string) => Promise<void>
  disconnect: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  connected: false,
  connecting: false,
  gatewayUrl: getGatewayUrl(),
  token: getGatewayToken(),
  error: null,

  connect: async (url?: string, token?: string) => {
    set({ connecting: true, error: null })
    try {
      await initGateway(url, token)
      set({ connecting: false, connected: true, error: null, gatewayUrl: url || getGatewayUrl(), token: token || getGatewayToken() })
    } catch (err) {
      const msg = err instanceof Error ? err.message : '连接失败'
      set({ connecting: false, connected: false, error: msg })
      throw new Error(msg)
    }
  },

  disconnect: () => {
    const s = getSocket()
    if (s) s.disconnect()
    set({ connected: false, error: null })
  },
}))
