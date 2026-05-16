import type { ChatMessage } from '../../types/message'
import { extractText } from '../../types/message'
import { MarkdownRenderer } from './MarkdownRenderer'

interface MessageBubbleProps {
  message: ChatMessage
}

export function MessageBubble({ message }: MessageBubbleProps) {
  const isUser = message.role === 'user'
  const text = extractText(message.content)
  const time = new Date(message.timestamp || Date.now())
  const timeStr = `${time.getHours().toString().padStart(2, '0')}:${time.getMinutes().toString().padStart(2, '0')}`

  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div className={`max-w-[80%] ${isUser ? 'order-1' : 'order-1'}`}>
        <div
          className={`px-3 py-2 rounded-lg text-sm leading-relaxed ${
            isUser
              ? 'bg-blue-500 text-white rounded-br-sm'
              : 'bg-gray-100 text-gray-800 rounded-bl-sm'
          }`}
        >
          {isUser ? (
            <span className="whitespace-pre-wrap">{text}</span>
          ) : (
            <MarkdownRenderer text={text || '(empty)'} />
          )}
        </div>
        <div className={`text-[10px] text-gray-400 mt-0.5 ${isUser ? 'text-right' : 'text-left'}`}>
          {timeStr}
        </div>
      </div>
    </div>
  )
}
