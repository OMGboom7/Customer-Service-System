import ReactMarkdown, { Components } from 'react-markdown'
import remarkGfm from 'remark-gfm'

const components: Components = {
  code: ({ className, children }) => {
    const match = /language-(\w+)/.exec(className || '')
    const isInline = !match && !className
    const code = String(children || '')
    if (isInline) {
      return (
        <code className="bg-gray-100 text-red-600 px-1 py-0.5 rounded text-xs font-mono">
          {code}
        </code>
      )
    }
    return (
      <pre className="bg-gray-50 border border-gray-200 text-gray-800 rounded-lg p-3 my-2 overflow-x-auto text-xs leading-relaxed">
        <code className={`font-mono ${className || ''}`}>
          {code}
        </code>
      </pre>
    )
  },
  pre: ({ children }) => <>{children}</>,
  table: ({ children }) => (
    <div className="overflow-x-auto my-2">
      <table className="min-w-full border-collapse border border-gray-300 text-xs">{children}</table>
    </div>
  ),
  th: ({ children }) => <th className="border border-gray-300 bg-gray-100 px-2 py-1 font-semibold text-left">{children}</th>,
  td: ({ children }) => <td className="border border-gray-300 px-2 py-1">{children}</td>,
}

export function MarkdownRenderer({ text }: { text: string }) {
  if (!text) return null
  return (
    <div className="prose prose-sm max-w-none prose-p:my-1 prose-headings:my-2 prose-a:text-blue-600 prose-ul:my-1 prose-ol:my-1 prose-li:my-0 prose-strong:font-semibold">
      <ReactMarkdown remarkPlugins={[remarkGfm]} components={components}>
        {text}
      </ReactMarkdown>
    </div>
  )
}
