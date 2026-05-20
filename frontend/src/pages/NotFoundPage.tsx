import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <div className="text-center">
        <h1 className="text-6xl font-bold text-gray-300 mb-4">404</h1>
        <p className="text-gray-500 mb-4">页面未找到</p>
        <Link to="/" className="text-blue-500 hover:underline text-sm">
          返回首页
        </Link>
      </div>
    </div>
  )
}
