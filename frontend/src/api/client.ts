import { GatewaySocket } from './socket'
import { createSessionApi, createAgentApi } from './endpoints'
import type { SessionApi, AgentApi } from './endpoints'

let socket: GatewaySocket | null = null
let sessionApi: SessionApi | null = null
let agentApi: AgentApi | null = null

export function getGatewayUrl(): string {
  return import.meta.env.VITE_GATEWAY_URL || 'https://oc.p07.icu'
}

export function getGatewayToken(): string {
  return import.meta.env.VITE_GATEWAY_TOKEN || ''
}

export async function initGateway(url?: string, token?: string): Promise<void> {
  const gwUrl = url || getGatewayUrl()
  const gwToken = token || getGatewayToken()
  socket = new GatewaySocket(gwUrl, gwToken)
  await socket.connect()
  sessionApi = createSessionApi((method, params) => socket!.request(method, params))
  agentApi = createAgentApi((method, params) => socket!.request(method, params))
}

export function getSocket(): GatewaySocket | null {
  return socket
}

export function getSessionApi(): SessionApi | null {
  return sessionApi
}

export function getAgentApi(): AgentApi | null {
  return agentApi
}
