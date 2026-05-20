import type { ReactNode } from 'react'

interface BadgeProps {
  count: number
  children?: ReactNode
}

export function Badge({ count = 0, children }: BadgeProps) {
  if (count <= 0) return <>{children}</>
  return (
    <div className="relative inline-flex">
      {children}
      <span className="absolute -top-1 -right-1 inline-flex items-center justify-center min-w-[16px] h-4 px-1 text-[10px] font-bold text-white bg-red-500 rounded-full">
        {count > 99 ? '99+' : count}
      </span>
    </div>
  )
}
