import { useState, useEffect, ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { validateToken } from '../service/auth.service'
import { useAuthStore } from '../store/authStore'

interface PrivateRouteProps {
  children: ReactNode
}

export default function PrivateRoute({ children }: PrivateRouteProps) {
  const { token } = useAuthStore()
  const [isValid, setIsValid] = useState<boolean | null>(null)

  useEffect(() => {
    const checkToken = async () => {
      if (!token) {
        setIsValid(false)
        return
      }
      try {
        const res = await validateToken()
        console.log()
        if (res?.authenticated) {
          setIsValid(true)
        } else {
          setIsValid(false)
        }
      } catch (error) {
        console.error(error)
        setIsValid(false)
      }
    }
    checkToken()
  }, [token])

  if (isValid === null)
    return (
      <div className="flex items-center justify-center p-8">
        <span className="loading loading-dots loading-lg"></span>
      </div>
    )

  if (!isValid) return <Navigate to="/login" replace />

  return <>{children}</>
}
