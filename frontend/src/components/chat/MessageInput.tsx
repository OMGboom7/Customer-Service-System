import { useRef, useEffect } from 'react'

interface MessageInputProps {
  draft: string
  sending: boolean
  onDraftChange: (v: string) => void
  onSend: () => void
  onKeyDown: (e: React.KeyboardEvent) => void
}

export function MessageInput({ draft, sending, onDraftChange, onSend, onKeyDown }: MessageInputProps) {
  const textRef = useRef<HTMLTextAreaElement>(null)

  useEffect(() => {
    if (textRef.current) {
      textRef.current.style.height = 'auto'
      textRef.current.style.height = `${Math.min(textRef.current.scrollHeight, 120)}px`
    }
  }, [draft])

  return (
    <div className="border-t border-gray-200 p-3 bg-white">
      <div className="flex items-end gap-2">
        <textarea
          ref={textRef}
          value={draft}
          onChange={(e) => onDraftChange(e.target.value)}
          onKeyDown={onKeyDown}
          placeholder="输入消息... (Enter 发送)"
          rows={1}
          className="flex-1 px-3 py-2 text-sm border border-gray-300 rounded-md resize-none focus:outline-none focus:border-blue-400 max-h-[120px]"
        />
        <button
          onClick={onSend}
          disabled={sending || !draft.trim()}
          className="px-4 py-2 bg-blue-600 text-white text-sm rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex-shrink-0"
        >
          {sending ? '发送中...' : '发送'}
        </button>
      </div>
    </div>
  )
}
