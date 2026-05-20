interface AvatarProps {
  name: string
  url?: string | null
  size?: 'sm' | 'md' | 'lg'
  online?: boolean
}

const sizeMap = { sm: 'w-8 h-8 text-xs', md: 'w-10 h-10 text-sm', lg: 'w-12 h-12 text-base' }

export function Avatar({ name, url, size = 'md', online }: AvatarProps) {
  const initials = name.slice(0, 2).toUpperCase()
  const colors = ['bg-blue-500', 'bg-green-500', 'bg-purple-500', 'bg-orange-500', 'bg-pink-500']
  const colorIdx = name.charCodeAt(0) % colors.length

  return (
    <div className={`relative flex-shrink-0 ${sizeMap[size]}`}>
      {url ? (
        <img src={url} alt={name} className="w-full h-full rounded-full object-cover" />
      ) : (
        <div className={`w-full h-full rounded-full ${colors[colorIdx]} flex items-center justify-center text-white font-medium`}>
          {initials}
        </div>
      )}
      {online !== undefined && (
        <span className={`absolute bottom-0 right-0 w-2.5 h-2.5 border-2 border-white rounded-full ${online ? 'bg-green-500' : 'bg-gray-400'}`} />
      )}
    </div>
  )
}
