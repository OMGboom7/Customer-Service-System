type EventHandler = (event: { type: string; payload?: unknown }) => void

export class GatewaySocket {
  private url: string
  private token: string
  private callId = 0
  private ws: WebSocket | null = null
  private pending = new Map<string, { resolve: (v: unknown) => void; reject: (e: Error) => void; timeout: ReturnType<typeof setTimeout> }>()
  private handlers = new Map<string, EventHandler[]>()
  private connected = false

  constructor(url: string, token: string) {
    this.url = url
    this.token = token
  }

  on(event: string, handler: EventHandler) {
    const list = this.handlers.get(event) || []
    list.push(handler)
    this.handlers.set(event, list)
  }

  off(event: string, handler: EventHandler) {
    const list = this.handlers.get(event)
    if (!list) return
    const idx = list.indexOf(handler)
    if (idx >= 0) list.splice(idx, 1)
  }

  private getWsUrl(): string {
    const isDev = location.hostname === 'localhost' || location.hostname === '127.0.0.1'
    if (isDev) return `ws://${location.host}/gw`
    return this.url.replace(/^http/, 'ws')
  }

  async connect(): Promise<void> {
    if (this.ws && this.connected) return

    return new Promise((resolve, reject) => {
      const wsUrl = this.getWsUrl()
      const ws = new WebSocket(wsUrl)
      const cid = 'conn-' + Date.now()
      let settled = false

      const timeout = setTimeout(() => {
        ws.close()
        if (!settled) { settled = true; reject(new Error('连接超时')) }
      }, 15000)

      ws.onopen = () => {
        ws.send(JSON.stringify({
          type: 'req', id: cid,
          method: 'connect',
          params: {
            minProtocol: 3, maxProtocol: 3,
            client: { id: 'openclaw-control-ui', version: '1.0', platform: 'web', mode: 'webchat', instanceId: 'cs-' + cid },
            role: 'operator',
            scopes: ['operator.admin', 'operator.read', 'operator.write', 'operator.approvals', 'operator.pairing'],
            caps: ['tool-events'],
            auth: { token: this.token },
            userAgent: navigator.userAgent,
            locale: navigator.language || 'zh-CN',
          },
        }))
      }

      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)

          if (data.type === 'event' && data.event === 'connect.challenge') return

          if (data.type === 'res') {
            if (data.id === cid) {
              if (data.ok) {
                clearTimeout(timeout)
                this.ws = ws
                this.connected = true
                settled = true
                resolve()
              } else {
                clearTimeout(timeout)
                settled = true
                ws.close()
                reject(new Error(data.error?.message || '连接失败'))
              }
              return
            }

            const p = this.pending.get(data.id)
            if (p) {
              clearTimeout(p.timeout)
              this.pending.delete(data.id)
              if (data.ok) p.resolve(data.payload)
              else p.reject(new Error(data.error?.message || 'RPC 错误'))
            }
            return
          }

          if (data.type === 'event') {
            const handlers = this.handlers.get(data.event) || []
            handlers.forEach(h => h({ type: data.event, payload: data.payload }))
          }
        } catch { /* ignore */ }
      }

      ws.onerror = () => { if (!settled) { settled = true; reject(new Error('无法连接到网关')) } }
      ws.onclose = () => {
        this.connected = false
        this.ws = null
        this.pending.forEach((p) => p.reject(new Error('连接断开')))
        this.pending.clear()
        if (!settled) { settled = true; reject(new Error('连接断开')) }
      }
    })
  }

  async request(method: string, params: Record<string, unknown> = {}): Promise<unknown> {
    if (!this.ws || !this.connected) throw new Error('未连接到网关')

    const id = 'r' + String(++this.callId)
    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        this.pending.delete(id)
        reject(new Error('请求超时'))
      }, 30000)
      this.pending.set(id, { resolve, reject, timeout })
      this.ws!.send(JSON.stringify({ type: 'req', id, method, params }))
    })
  }

  disconnect() {
    this.ws?.close()
    this.ws = null
    this.connected = false
    this.pending.forEach((p) => p.reject(new Error('手动断开')))
    this.pending.clear()
  }
}
