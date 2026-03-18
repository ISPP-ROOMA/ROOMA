import { zodResolver } from '@hookform/resolvers/zod'
import { useCallback, useEffect, useRef, useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { getDeviceId, googleLogin, registerUser, type UserRole } from '../service/auth.service'
import { useAuthStore } from '../store/authStore'

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID

const registerSchema = z.object({
  email: z.email('Email no valido'),
  password: z.string().min(4, 'La contrasena debe tener al menos 4 caracteres'),
  role: z.enum(['TENANT', 'LANDLORD'], { message: 'Selecciona un tipo de cuenta' }),
})

type RegisterFormData = z.infer<typeof registerSchema>

export default function Register() {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const [isPasswordVisible, setIsPasswordVisible] = useState(false)
  const [googleReady, setGoogleReady] = useState(false)
  const selectedRoleRef = useRef<UserRole | null>(null)

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors },
    control,
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
  })

  const selectedRole = useWatch({ control, name: 'role' })

  // Keep a ref in sync so the Google callback always has the latest role
  useEffect(() => {
    selectedRoleRef.current = selectedRole ?? null
  }, [selectedRole])

  const handleGoogleResponse = useCallback(
    async (response: { credential: string }) => {
      setError(null)

      const role = selectedRoleRef.current
      if (!role) {
        setError('Selecciona un tipo de cuenta antes de continuar con Google')
        return
      }

      const res = await googleLogin(response.credential, role)

      if (res.error || !res.token) {
        setError(res.error ?? 'Error signing in with Google')
        return
      }

      useAuthStore.getState().login({
        token: res.token,
        role: res.role,
        userId: res.userId,
      })

      if (res.role === 'LANDLORD') {
        navigate('/apartments/my')
      } else {
        navigate('/')
      }
    },
    [navigate],
  )

  useEffect(() => {
    if (!GOOGLE_CLIENT_ID || !window.google) return

    window.google.accounts.id.initialize({
      client_id: GOOGLE_CLIENT_ID,
      callback: handleGoogleResponse,
    })
    setGoogleReady(true)
  }, [handleGoogleResponse])

  const handleGoogleClick = () => {
    if (!selectedRoleRef.current) {
      setError('Selecciona un tipo de cuenta antes de continuar con Google')
      return
    }
    if (!window.google || !googleReady) return
    window.google.accounts.id.prompt()
  }

  const onSubmit = async (data: RegisterFormData) => {
    const deviceId = getDeviceId()
    const res = await registerUser({
      email: data.email,
      password: data.password,
      deviceId,
      role: data.role,
    })
    if (res.error) {
      setError(res.error)
      return
    }

    useAuthStore.getState().login({
      token: res.token,
      role: res.role,
      userId: res.userId,
    })

    if (res.role === 'LANDLORD') {
      navigate('/apartments/my')
    } else {
      navigate('/')
    }
  }

  const togglePassword = () => {
    setIsPasswordVisible(!isPasswordVisible)
  }

  return (
    <div className="mx-auto flex w-full max-w-md items-center justify-center px-3 pb-24 pt-4 sm:px-4 sm:pt-6">
      <div className="relative w-full overflow-hidden rounded-[30px] border border-base-300/80 bg-base-100/95 p-5 shadow-[0_22px_55px_rgba(0,0,0,0.16)] backdrop-blur-sm sm:p-6">
        <div className="pointer-events-none absolute inset-x-0 top-0 h-20 bg-gradient-to-b from-primary/10 to-transparent" />

        <div className="relative mb-5 text-center">
          <div className="mx-auto mb-3 inline-flex items-center rounded-full border border-primary/25 bg-primary/10 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.08em] text-primary">
            Rooma
          </div>
          <h2 className="text-3xl font-semibold tracking-tight text-base-content">Crear cuenta</h2>
          <p className="mt-2 text-sm text-base-content/65">Elige como quieres usar Rooma</p>
        </div>

        <form className="space-y-5" onSubmit={handleSubmit(onSubmit)}>
          <div className="form-control">
            <label className="label pb-0 mb-3">
              <span className="label-text font-semibold tracking-wide text-base-content/85">
                Tipo de cuenta
              </span>
            </label>
            <div className="mt-3 grid grid-cols-1 sm:grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => setValue('role', 'TENANT', { shouldValidate: true })}
                className={`group relative flex min-h-[124px] flex-col items-center justify-center gap-1.5 rounded-3xl border px-4 py-3 transition-all duration-200 ${selectedRole === 'TENANT'
                    ? 'border-primary bg-primary/10 ring-2 ring-primary/20 shadow-[0_10px_24px_rgba(0,128,128,0.2)]'
                    : 'border-base-300 bg-base-100 hover:border-primary/35 hover:bg-base-200/50'
                  }`}
              >
                <span
                  className={`absolute right-3 top-3 grid h-5 w-5 place-items-center rounded-full border text-[10px] font-bold ${selectedRole === 'TENANT'
                      ? 'border-primary bg-primary text-primary-content'
                      : 'border-base-300 bg-base-100 text-transparent'
                    }`}
                >
                  ✓
                </span>
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className={`h-8 w-8 transition-transform duration-200 ${selectedRole === 'TENANT'
                      ? 'text-primary scale-105'
                      : 'text-base-content/50 group-hover:text-primary/80'
                    }`}
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={1.5}
                    d="M21 21l-4.35-4.35M11 19a8 8 0 100-16 8 8 0 000 16z"
                  />
                </svg>
                <span className="mt-1 font-semibold text-base">Inquilino</span>
                <span className="text-[11px] text-base-content/55 text-center">
                  Busco piso o habitacion
                </span>
              </button>
              <button
                type="button"
                onClick={() => setValue('role', 'LANDLORD', { shouldValidate: true })}
                className={`group relative flex min-h-[124px] flex-col items-center justify-center gap-1.5 rounded-3xl border px-4 py-3 transition-all duration-200 ${selectedRole === 'LANDLORD'
                    ? 'border-primary bg-primary/10 ring-2 ring-primary/20 shadow-[0_10px_24px_rgba(0,128,128,0.2)]'
                    : 'border-base-300 bg-base-100 hover:border-primary/35 hover:bg-base-200/50'
                  }`}
              >
                <span
                  className={`absolute right-3 top-3 grid h-5 w-5 place-items-center rounded-full border text-[10px] font-bold ${selectedRole === 'LANDLORD'
                      ? 'border-primary bg-primary text-primary-content'
                      : 'border-base-300 bg-base-100 text-transparent'
                    }`}
                >
                  ✓
                </span>
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className={`h-8 w-8 transition-transform duration-200 ${selectedRole === 'LANDLORD'
                      ? 'text-primary scale-105'
                      : 'text-base-content/50 group-hover:text-primary/80'
                    }`}
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={1.5}
                    d="M3 11.5L12 4l9 7.5V20a1 1 0 01-1 1h-5v-6H9v6H4a1 1 0 01-1-1v-8.5z"
                  />
                </svg>
                <span className="mt-1 font-semibold text-base">Propietario</span>
                <span className="text-[11px] text-base-content/55 text-center">
                  Publico pisos o habitaciones
                </span>
              </button>
            </div>
            {errors.role && <p className="text-error text-sm mt-1">{errors.role.message}</p>}
          </div>

          <div className="form-control">
            <label className="label">
              <span className="label-text font-medium text-base-content/85">Email</span>
            </label>
            <input
              {...register('email')}
              id="email"
              type="text"
              placeholder="tu@email.com"
              className="input input-bordered h-12 w-full rounded-2xl border-base-300 bg-base-100/90 px-4 text-base-content placeholder:text-base-content/45 focus:border-primary focus:outline-none"
            />
            {errors.email && <p className="text-error text-sm mt-1">{errors.email.message}</p>}
          </div>

          <div className="form-control">
            <label className="label">
              <span className="label-text font-medium text-base-content/85">Contrasena</span>
            </label>
            <input
              {...register('password')}
              id="password"
              type={isPasswordVisible ? 'text' : 'password'}
              placeholder="Minimo 4 caracteres"
              className="input input-bordered h-12 w-full rounded-2xl border-base-300 bg-base-100/90 px-4 text-base-content placeholder:text-base-content/45 focus:border-primary focus:outline-none"
            />
            {errors.password && (
              <p className="text-error text-sm mt-1">{errors.password.message}</p>
            )}
          </div>

          {error && (
            <p className="rounded-xl border border-error/30 bg-error/10 px-3 py-2 text-center text-sm text-error">
              {error}
            </p>
          )}

          <div className="flex flex-col gap-2">
            <button
              type="button"
              onClick={togglePassword}
              className="btn btn-link btn-sm h-auto min-h-0 self-start px-0 text-primary/80 hover:text-primary"
            >
              {isPasswordVisible ? 'Ocultar contrasena' : 'Ver contrasena'}
            </button>
            <button
              type="submit"
              className="btn btn-primary order-1 h-12 w-full rounded-2xl border-none text-base font-semibold shadow-[0_12px_24px_rgba(0,128,128,0.26)] hover:translate-y-[-1px] hover:shadow-[0_14px_30px_rgba(0,128,128,0.34)]"
            >
              Registrarse
            </button>
          </div>

          <div className="divider text-base-content/50 text-xs">o</div>
          <div className="flex justify-center">
            <button
              type="button"
              onClick={handleGoogleClick}
              className="btn btn-circle btn-outline h-14 w-14 border-base-300 bg-white hover:bg-base-200 hover:border-base-400 shadow-sm transition-all duration-200"
            >
              <img src="/Logo Google.png" alt="Google" className="h-7 w-7" />
            </button>
          </div>

          <p className="pt-1 text-center text-sm text-base-content/65">
            Ya tienes cuenta?{' '}
            <Link to="/login" className="link link-primary">
              Inicia sesion
            </Link>
          </p>
        </form>
      </div>
    </div>
  )
}
