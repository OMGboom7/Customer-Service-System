import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { LoginForm } from '../components/auth/LoginForm'

export function LoginPage() {
  const connected = useAuthStore((s) => s.connected)
  const navigate = useNavigate()

  useEffect(() => {
    if (connected) {
      navigate('/', { replace: true })
    }
  }, [connected, navigate])

  if (connected) return null
  return <LoginForm />
}
