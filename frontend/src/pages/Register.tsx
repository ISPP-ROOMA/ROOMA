import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { getDeviceId, registerUser } from '../service/auth.service'
import { useAuthStore } from '../store/authStore'

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
    <div className="flex items-center justify-center mt-4 sm:mt-6 p-3 sm:p-4">
      <div className="card w-full max-w-md bg-base-100 shadow">
        <div className="card-body">
          <h2 className="card-title justify-center text-2xl">Crear cuenta</h2>
          <p className="text-center text-sm text-gray-500 mb-2">Elige como quieres usar Rooma</p>

          <form className="space-y-5" onSubmit={handleSubmit(onSubmit)}>
            <div className="form-control">
              <label className="label">
                <span className="label-text font-semibold">Tipo de cuenta</span>
              </label>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={() => setValue('role', 'TENANT', { shouldValidate: true })}
                  className={`flex flex-col items-center gap-2 p-4 rounded-2xl border-2 transition-all ${
                    selectedRole === 'TENANT'
                      ? 'border-primary bg-primary/5 shadow-sm'
                      : 'border-base-300 hover:border-base-content/20'
                  }`}
                >
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    className="h-8 w-8 text-primary"
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
                  <span className="font-semibold text-sm">Inquilino</span>
                  <span className="text-[11px] text-gray-500 text-center">Busco piso o habitacion</span>
                </button>
                <button
                  type="button"
                  onClick={() => setValue('role', 'LANDLORD', { shouldValidate: true })}
                  className={`flex flex-col items-center gap-2 p-4 rounded-2xl border-2 transition-all ${
                    selectedRole === 'LANDLORD'
                      ? 'border-primary bg-primary/5 shadow-sm'
                      : 'border-base-300 hover:border-base-content/20'
                  }`}
                >
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    className="h-8 w-8 text-primary"
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
                  <span className="font-semibold text-sm">Propietario</span>
                  <span className="text-[11px] text-gray-500 text-center">Publico pisos o habitaciones</span>
                </button>
              </div>
              {errors.role && <p className="text-error text-sm mt-1">{errors.role.message}</p>}
            </div>

            <div className="form-control">
              <label className="label">
                <span className="label-text">Email</span>
              </label>
              <input
                {...register('email')}
                id="email"
                type="text"
                placeholder="tu@email.com"
                className="input input-bordered w-full rounded-2xl"
              />
              {errors.email && <p className="text-error text-sm mt-1">{errors.email.message}</p>}
            </div>

            <div className="form-control">
              <label className="label">
                <span className="label-text">Contrasena</span>
              </label>
              <input
                {...register('password')}
                id="password"
                type={isPasswordVisible ? 'text' : 'password'}
                placeholder="Minimo 4 caracteres"
                className="input input-bordered w-full rounded-2xl"
              />
              {errors.password && <p className="text-error text-sm mt-1">{errors.password.message}</p>}
            </div>

            {error && <p className="text-error text-center">{error}</p>}

            <div className="flex flex-col gap-2">
              <button
                type="button"
                onClick={togglePassword}
                className="btn btn-link btn-sm px-0 self-start order-2"
              >
                {isPasswordVisible ? 'Ocultar contrasena' : 'Ver contrasena'}
              </button>
              <button type="submit" className="btn btn-primary w-full order-1 rounded-2xl border-none">
                Registrarse
              </button>
            </div>

            <p className="text-center text-sm text-gray-500">
              Ya tienes cuenta?{' '}
              <Link to="/login" className="link link-primary">
                Inicia sesion
              </Link>
            </p>
          </form>
        </div>
      </div>
    </div>
  )
}
