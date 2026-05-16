import { useRef, useEffect } from 'react'
import { useChatStore } from '../../store/chatStore'
import { useSessionStore } from '../../store/sessionStore'
import { useChat } from '../../hooks/useChat'
import { MessageBubble } from './MessageBubble'
import { MessageInput } from './MessageInput'
import { MarkdownRenderer } from './MarkdownRenderer'
import { Loading } from '../common/Loading'

function StreamingMessage({ content }: { content: string }) {
  if (!content) return null
  return (
    <div className="flex justify-start">
      <div className="max-w-[80%]">
        <div className="px-3 py-2 rounded-lg text-sm leading-relaxed bg-gray-100 text-gray-800 rounded-bl-sm">
          <MarkdownRenderer text={content} />
          <span className="inline-block w-1.5 h-4 bg-blue-500 animate-pulse ml-0.5 align-middle" />
        </div>
      </div>
    </div>
  )
}

export function ChatPanel() {
  const selectedKey = useSessionStore((s) => s.selectedKey)
  const messages = useChatStore((s) => s.messages)
  const sending = useChatStore((s) => s.sending)
  const streamingContent = useChatStore((s) => s.streamingContent)
  const { loading, draft, error, setDraft, loadHistory, clear, send, handleKeyDown } = useChat()
  const scrollRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (selectedKey) {
      clear()
      loadHistory()
    }
  }, [selectedKey])

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight
    }
  }, [messages, streamingContent])

  if (!selectedKey) {
    return (
      <div className="flex-1 flex items-center justify-center text-gray-400 text-sm">
        选择一个会话开始聊天
      </div>
    )
  }

  return (
    <div className="flex-1 flex flex-col min-h-0">
      <div ref={scrollRef} className="flex-1 overflow-y-auto min-h-0 p-4 space-y-3">
        {loading ? (
          <Loading text="加载历史消息..." />
        ) : error ? (
          <div className="text-sm text-red-500 text-center">
            <p>{error}</p>
            <button onClick={loadHistory} className="mt-1 text-blue-500 hover:underline">
              重试
            </button>
          </div>
        ) : messages.length === 0 && !sending ? (
          <div className="text-sm text-gray-400 text-center mt-8">暂无消息，开始对话吧</div>
        ) : (
          <>
            {messages.map((msg, idx) => <MessageBubble key={msg.id || `msg-${idx}`} message={msg} />)}
            {(sending || streamingContent) && <StreamingMessage content={streamingContent} />}
          </>
        )}
      </div>

      <MessageInput
        draft={draft}
        sending={sending}
        onDraftChange={setDraft}
        onSend={() => send(draft)}
        onKeyDown={handleKeyDown}
      />
    </div>
  )
}
