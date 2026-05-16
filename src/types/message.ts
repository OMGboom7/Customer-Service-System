export interface ContentBlock {
  type: string
  text?: string
  thinking?: string
  thinkingSignature?: string
}

export interface ChatMessage {
  id?: string
  role: 'user' | 'assistant' | 'system'
  content: string | ContentBlock[]
  timestamp?: string
  sessionKey?: string
  sessionId?: string
}

export interface MessageSendParams {
  sessionKey: string
  message: string
}

export interface SessionHistoryParams {
  sessionKey: string
  limit?: number
  before?: number
}

export function extractText(content: string | ContentBlock[]): string {
  if (typeof content === 'string') return content
  return content
    .filter(b => b.type === 'text')
    .map(b => b.text || '')
    .join('')
}
