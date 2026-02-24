import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { getDeviceId, loginUser } from '../service/auth.service'
import { useAuthStore } from '../store/authStore'

const schema = z.object({
  email: z.email('Invalid email'),
  password: z.string().min(4, 'Password must be at least 4 characters'),
})

type LoginFormData = z.infer<typeof schema>

export default function Login() {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const [isPasswordVisible, setIsPasswordVisible] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(schema),
  })

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
    })

    if (res.role === 'LANDLORD') {
      navigate('/apartments/my')
      return
    }

    navigate('/')
  }

  return (
    <div className="flex items-center justify-center mt-6 p-4">
      <div className="card w-full max-w-md bg-base-100 shadow">
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
                className="input input-bordered w-full"
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
                className="input input-bordered w-full"
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
              <button type="submit" className="btn btn-primary">
                Login
              </button>
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
