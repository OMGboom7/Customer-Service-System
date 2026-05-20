import type { ReactNode } from 'react'

export function AppLayout({ sidebar, chat, info, header }: { sidebar: ReactNode; chat: ReactNode; info: ReactNode; header: ReactNode }) {
  return (
    <div className="h-full flex flex-col bg-gray-50">
      {header}
      <div className="flex flex-1 overflow-hidden">
        <div className="w-72 border-r border-gray-200 bg-white flex-shrink-0">
          {sidebar}
        </div>
        <div className="flex-1 flex flex-col min-w-0 min-h-0">
          {chat}
        </div>
        <div className="w-72 border-l border-gray-200 bg-white flex-shrink-0">
          {info}
        </div>
      </div>
    </div>
  )
}
