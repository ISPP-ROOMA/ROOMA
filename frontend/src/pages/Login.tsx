import { zodResolver } from '@hookform/resolvers/zod'
import { useCallback, useEffect, useRef, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { getDeviceId, googleLogin, loginUser } from '../service/auth.service'
import { useAuthStore } from '../store/authStore'

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: Record<string, unknown>) => void
          renderButton: (element: HTMLElement, config: Record<string, unknown>) => void
        }
      }
    }
  }
}

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID

const schema = z.object({
  email: z.email('Invalid email'),
  password: z.string().min(4, 'Password must be at least 4 characters'),
})

type LoginFormData = z.infer<typeof schema>

export default function Login() {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const [isPasswordVisible, setIsPasswordVisible] = useState(false)
  const googleBtnRef = useRef<HTMLDivElement>(null)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(schema),
  })

  const handleGoogleResponse = useCallback(
    async (response: { credential: string }) => {
      setError(null)
      const res = await googleLogin(response.credential)

      if (res.error || !res.token) {
        if (res.error?.toLowerCase().includes('role is required')) {
          setError('No tienes cuenta. Regístrate primero para continuar con Google.')
        } else {
          setError(res.error ?? 'Error signing in with Google')
        }
        return
      }

      useAuthStore.getState().login({
        token: res.token,
        role: res.role,
        userId: res.userId,
      })

      navigate('/')
    },
    [navigate],
  )

  useEffect(() => {
    if (!GOOGLE_CLIENT_ID) return

    const initGoogle = () => {
      if (!window.google) return false

      window.google.accounts.id.initialize({
        client_id: GOOGLE_CLIENT_ID,
        callback: handleGoogleResponse,
      })

      if (googleBtnRef.current) {
        window.google.accounts.id.renderButton(googleBtnRef.current, {
          type: 'icon',
          size: 'large',
          shape: 'circle',
          theme: 'outline',
        })
      }
      return true
    }

    if (!initGoogle()) {
      const interval = setInterval(() => {
        if (initGoogle()) clearInterval(interval)
      }, 200)
      return () => clearInterval(interval)
    }
  }, [handleGoogleResponse])

  const onSubmit = async (data: LoginFormData) => {
    setError(null)
    const deviceId = getDeviceId()
    const res = await loginUser({
      email: data.email,
      password: data.password,
      deviceId,
      role: 'TENANT',
    })

    if (res.error || !res.token) {
      setError(res.error ?? 'Invalid credentials')
      return
    }

    useAuthStore.getState().login({
      token: res.token,
      role: res.role,
      userId: res.userId,
    })

    navigate('/')
  }

  return (
    <div className="flex items-center justify-center min-h-[85vh] w-full px-4">
      <div className="card w-full max-w-md bg-base-100/40 backdrop-blur-2xl shadow-[0_8px_32px_rgba(0,0,0,0.12)] border border-white/20 rounded-xl">
        <div className="card-body">
          <h2 className="card-title justify-center text-2xl">Iniciar sesión</h2>

          <form className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
            <div className="form-control">
              <label className="label">
                <span className="label-text">Email</span>
              </label>
              <input
                {...register('email')}
                id="email"
                type="text"
                className="input input-bordered w-full rounded-2xl"
              />
              {errors.email && <p className="text-error text-sm mt-1">{errors.email.message}</p>}
            </div>

            <div className="form-control">
              <label className="label">
                <span className="label-text">Password</span>
              </label>
              <input
                {...register('password')}
                id="password"
                type={isPasswordVisible ? 'text' : 'password'}
                className="input input-bordered w-full rounded-2xl"
              />
              {errors.password && (
                <p className="text-error text-sm mt-1">{errors.password.message}</p>
              )}
            </div>

            {error && <p className="text-error text-sm text-center">{error}</p>}

            <div className="flex justify-between items-center">
              <button
                type="button"
                onClick={() => setIsPasswordVisible((prev) => !prev)}
                className="btn btn-link px-0"
              >
                {isPasswordVisible ? 'Ocultar contraseña' : 'Ver contraseña'}
              </button>
              <button type="submit" className="btn btn-primary rounded-2xl px-8 border-none">
                Login
              </button>
            </div>

            <div className="divider text-base-content/50 text-xs">o</div>
            <div className="flex justify-center">
              <div ref={googleBtnRef} />
            </div>

            <p className="text-center text-sm text-gray-500">
              ¿No tienes cuenta?{' '}
              <Link to="/register" className="link link-primary">
                Regístrate
              </Link>
            </p>
          </form>
        </div>
      </div>
    </div>
  )
}
