import { useState } from "react"
import { useNavigate } from "react-router-dom"
import { useForm } from "react-hook-form"
import { z } from "zod"
import { zodResolver } from "@hookform/resolvers/zod"
import { getDeviceId, loginUser } from "../service/auth.service"
import { useAuthStore } from "../store/authStore"

const schema = z.object({
  email: z.email('Invalid email'),
  password: z.string().min(6, 'Password must be at least 6 characters'),
  deviceId: z.string().optional()
})

type LoginFormData = z.infer<typeof schema>

export default function Login() {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const [isPasswordVisible, setIsPasswordVisible] = useState(false)

  const { register, handleSubmit, formState: { errors } } = useForm<LoginFormData>({
    resolver: zodResolver(schema),
  })

  const onSubmit = async (data: LoginFormData) => {
    const deviceId = getDeviceId()
    const res = await loginUser({ email: data.email, password: data.password, deviceId: deviceId})
    if (res.error) {
      setError(res.error)
      return
    }

    useAuthStore.getState()
      .login({
        token: res.token,
        role: res.role
      })

    navigate("/")
  }

  const togglePassword = () => {
    setIsPasswordVisible(!isPasswordVisible)
  }

  return (
    <div className="flex items-center justify-center mt-6 p-4">
      <div className="card w-full max-w-md bg-base-100 shadow">
        <div className="card-body">
          <h2 className="card-title justify-center">Login</h2>

          <form className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
            <div className="form-control">
              <label className="label"><span className="label-text">Email</span></label>
              <input {...register("email")} id="email" type="text" className="input input-bordered w-full" />
              {errors.email && <p className="text-error text-sm mt-1">{errors.email.message}</p>}
            </div>

            <div className="form-control">
              <label className="label"><span className="label-text">Password</span></label>
              <input {...register("password")} id="password" type={isPasswordVisible ? 'text' : 'password'} className="input input-bordered w-full" />
              {errors.password && <p className="text-error text-sm mt-1">{errors.password.message}</p>}
            </div>

            {error && <p className="text-error text-center">{error}</p>}

            <div className="flex items-center justify-between">
              <button type="button" onClick={togglePassword} className="btn btn-link">{isPasswordVisible ? 'Ocultar contraseña' : 'Ver contraseña'}</button>
              <button type="submit" className="btn btn-primary">Login</button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}
