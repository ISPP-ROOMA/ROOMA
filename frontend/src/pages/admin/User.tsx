import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { getUser, updateUser } from '../../service/users.service.js'

const schema = z.object({
  email: z.email('Email no válido'),
  role: z.enum(['ADMIN', 'TENANT', 'LANDLORD']).catch('TENANT'),
  password: z
    .string()
    .optional()
    .or(z.literal(''))
    .refine((val) => !val || val.length >= 4, {
      message: 'La contraseña debe tener al menos 4 caracteres',
    }),
})

type UserFormData = z.infer<typeof schema>

export default function User() {
  const { id } = useParams<{ id?: string }>()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<UserFormData>({
    resolver: zodResolver(schema),
  })

  useEffect(() => {
    const fetchUser = async () => {
      try {
        const res = await getUser(id!)

        reset({
          email: res?.email || '',
          role: (res?.role || 'TENANT') as 'ADMIN' | 'TENANT' | 'LANDLORD',
          password: '',
        })
      } catch {
        setError('Error al cargar el usuario')
      } finally {
        setIsLoading(false)
      }
    }

    fetchUser()
  }, [id, reset])

  const onSubmit = async (data: UserFormData) => {
    const res = await updateUser(id!, data)

    console.log(res)

    if (res.error) {
      setError(res.error)
      return
    }

    navigate('/users')
  }

  if (isLoading) {
    return <p className="text-center mt-10">Cargando usuario...</p>
  }

  return (
    <div className="flex items-center justify-center mt-6 p-4">
      <div className="card w-full max-w-md bg-base-100 shadow">
        <div className="card-body">
          <h2 className="card-title justify-center">Editar Usuario</h2>

          <form className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
            <div className="form-control">
              <label className="label">
                <span className="label-text">Email</span>
              </label>
              <input
                {...register('email')}
                id="email"
                type="email"
                className="input input-bordered w-full"
                required
              />
              {errors.email && <p className="text-error text-sm mt-1">{errors.email.message}</p>}
            </div>

            <div className="form-control">
              <label className="label">
                <span className="label-text">Rol</span>
              </label>
              <select
                {...register('role')}
                id="role"
                className="select select-bordered w-full"
                required
              >
                <option value="TENANT">Inquilino</option>
                <option value="LANDLORD">Propietario</option>
                <option value="ADMIN">Admin</option>
              </select>
              {errors.role && <p className="text-error text-sm mt-1">{errors.role.message}</p>}
            </div>

            <div className="form-control">
              <label className="label">
                <span className="label-text">Contraseña</span>
              </label>
              <input
                {...register('password')}
                id="password"
                type="password"
                className="input input-bordered w-full"
              />
              {errors.password && (
                <p className="text-error text-sm mt-1">{errors.password.message}</p>
              )}
            </div>

            {error && <p className="text-error text-center">{error}</p>}

            <div className="flex justify-between items-center">
              <button
                type="button"
                onClick={() => {
                  navigate('/users')
                }}
                className="btn"
              >
                Cancelar
              </button>
              <button className="btn btn-primary" type="submit">
                Guardar
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}
