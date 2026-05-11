import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useRef, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import {
  getUserProfile,
  updateUserProfile,
  uploadProfilePicture,
  deleteProfilePicture,
} from '../../service/users.service'
import type { UpdateUserPayload, User } from '../../service/users.service'
import { useAuthStore } from '../../store/authStore'
import { useToast } from '../../hooks/useToast'
import { profileSchema, type ProfileFormValues } from './profileSchema'

export default function ProfileEdit() {
  const navigate = useNavigate()
  const { role } = useAuthStore()
  const { showToast } = useToast()
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [userData, setUserData] = useState<User | null>(null)
  const [profilePicPreview, setProfilePicPreview] = useState<string | null>(null)
  const [isUploadingPic, setIsUploadingPic] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
  })

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const res = await getUserProfile()
        if (res) {
          setUserData(res)
          setProfilePicPreview(res.profilePic || null)
          reset({
            name: res.name || '',
            surname: res.surname || '',
            email: res.email || '',
            role: res.role || role || 'TENANT',
            phone: res.phone || '',
            birthDate: res.birthDate ? new Date(res.birthDate).toISOString().split('T')[0] : '',
            gender: res.gender || '',
            smoker: res.smoker !== undefined && res.smoker !== null ? String(res.smoker) : '',
            hobbies: res.hobbies || '',
            schedule: res.schedule || '',
            profession: res.profession || '',
            password: '',
            confirmPassword: '',
          })
        }
      } catch (error) {
        console.error('Error fetching profile', error)
      } finally {
        setIsLoading(false)
      }
    }
    void fetchProfile()
  }, [reset, role])

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    setIsUploadingPic(true)
    const url = await uploadProfilePicture(file)
    if (url) {
      setProfilePicPreview(url)
      setUserData((prev) => (prev ? { ...prev, profilePic: url } : prev))
    } else {
      showToast('No se pudo subir la imagen de perfil.', 'error')
    }
    setIsUploadingPic(false)
    if (fileInputRef.current) fileInputRef.current.value = ''
  }

  const handleDeletePic = async () => {
    setIsUploadingPic(true)
    const success = await deleteProfilePicture()
    if (success) {
      setProfilePicPreview(null)
      setUserData((prev) => (prev ? { ...prev, profilePic: undefined } : prev))
    } else {
      showToast('No se pudo eliminar la imagen de perfil.', 'error')
    }
    setIsUploadingPic(false)
  }

  const onSubmit = async (data: ProfileFormValues) => {
    setIsSubmitting(true)

    const payload: UpdateUserPayload = {
      name: data.name,
      surname: data.surname,
      email: data.email,
      phone: data.phone,
      birthDate: data.birthDate ? new Date(data.birthDate) : undefined,
      profilePic: profilePicPreview || undefined,
      gender: data.gender,
      smoker: data.smoker === 'true' ? true : data.smoker === 'false' ? false : undefined,
      hobbies: data.hobbies || undefined,
      schedule: data.schedule || undefined,
      profession: data.profession || undefined,
      password: data.password && data.password.length > 0 ? data.password : undefined,
    }

    const updated = await updateUserProfile(payload)
    if (updated && !('error' in updated)) {
      showToast('Perfil actualizado correctamente.', 'success')
      navigate('/profile')
    } else {
      showToast('No se pudo actualizar el perfil.', 'error')
    }
    setIsSubmitting(false)
  }

  if (isLoading) return <p className="text-center mt-10 text-gray-500">Cargando...</p>

  if (!userData) return <p className="text-center mt-10 text-red-500">Error al cargar el perfil</p>

  const initial =
    userData.name && userData.name.length > 0
      ? userData.name[0].toUpperCase()
      : userData.email
        ? userData.email[0].toUpperCase()
        : '?'

  return (
    <div className="flex flex-col items-center justify-center min-h-[70vh] p-4">
      <div className="card w-full max-w-2xl bg-base-100 shadow-lg">
        <div className="card-body">
          <h2 className="text-2xl font-bold mb-6 text-center">Editar Perfil</h2>

          {/* Profile Picture Section */}
          <div className="flex flex-col items-center gap-3 mb-6">
            <div className="avatar">
              <div className="w-28 h-28 rounded-full bg-primary text-white flex items-center justify-center text-4xl font-bold overflow-hidden ring ring-primary ring-offset-base-100 ring-offset-2">
                {profilePicPreview ? (
                  <img
                    src={profilePicPreview}
                    alt="Perfil"
                    className="object-cover w-full h-full"
                  />
                ) : (
                  initial
                )}
              </div>
            </div>
            <div className="flex gap-2">
              <button
                type="button"
                className={`btn btn-sm btn-outline btn-primary ${isUploadingPic ? 'loading' : ''}`}
                onClick={() => fileInputRef.current?.click()}
                disabled={isUploadingPic}
              >
                {isUploadingPic ? 'Subiendo...' : 'Cambiar foto'}
              </button>
              {profilePicPreview && (
                <button
                  type="button"
                  className={`btn btn-sm btn-outline btn-error ${isUploadingPic ? 'loading' : ''}`}
                  onClick={() => void handleDeletePic()}
                  disabled={isUploadingPic}
                >
                  Eliminar foto
                </button>
              )}
            </div>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              className="hidden"
              onChange={(e) => void handleFileChange(e)}
            />
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Nombre */}
              <div className="form-control w-full">
                <label className="label">
                  <span className="label-text">Nombre</span>
                </label>
                <input
                  type="text"
                  className={`input input-bordered w-full ${errors.name ? 'input-error' : ''}`}
                  {...register('name')}
                />
                {errors.name && (
                  <label className="label">
                    <span className="label-text-alt text-error">{errors.name.message}</span>
                  </label>
                )}
              </div>

              {/* Apellidos */}
              <div className="form-control w-full">
                <label className="label">
                  <span className="label-text">Apellidos</span>
                </label>
                <input
                  type="text"
                  className={`input input-bordered w-full ${errors.surname ? 'input-error' : ''}`}
                  {...register('surname')}
                />
                {errors.surname && (
                  <label className="label">
                    <span className="label-text-alt text-error">{errors.surname.message}</span>
                  </label>
                )}
              </div>

              {/* Email */}
              <div className="form-control w-full">
                <label className="label">
                  <span className="label-text">Email</span>
                </label>
                <input
                  type="email"
                  className={`input input-bordered w-full ${errors.email ? 'input-error' : ''}`}
                  {...register('email')}
                />
                {errors.email && (
                  <label className="label">
                    <span className="label-text-alt text-error">{errors.email.message}</span>
                  </label>
                )}
              </div>

              {/* Phone */}
              <div className="form-control w-full">
                <label className="label">
                  <span className="label-text">Teléfono</span>
                </label>
                <input
                  type="tel"
                  className={`input input-bordered w-full ${errors.phone ? 'input-error' : ''}`}
                  {...register('phone')}
                />
                {errors.phone && (
                  <label className="label">
                    <span className="label-text-alt text-error">{errors.phone.message}</span>
                  </label>
                )}
              </div>

              {/* Birth Date */}
              <div className="form-control w-full">
                <label className="label">
                  <span className="label-text">Fecha de Nacimiento</span>
                </label>
                <input
                  type="date"
                  className={`input input-bordered w-full ${errors.birthDate ? 'input-error' : ''}`}
                  {...register('birthDate')}
                />
                {errors.birthDate && (
                  <label className="label">
                    <span className="label-text-alt text-error">{errors.birthDate.message}</span>
                  </label>
                )}
              </div>

              {/* Profession */}
              <div className="form-control w-full">
                <label className="label">
                  <span className="label-text">Profesión</span>
                </label>
                <input
                  type="text"
                  className={`input input-bordered w-full ${errors.profession ? 'input-error' : ''}`}
                  {...register('profession')}
                />
                {errors.profession && (
                  <label className="label">
                    <span className="label-text-alt text-error">{errors.profession.message}</span>
                  </label>
                )}
              </div>

              {/* Gender */}
              <div className="form-control w-full">
                <label className="label">
                  <span className="label-text">Género</span>
                </label>
                <select
                  className={`select select-bordered w-full ${errors.gender ? 'select-error' : ''}`}
                  {...register('gender')}
                >
                  <option value="">Selecciona...</option>
                  <option value="Male">Masculino</option>
                  <option value="Female">Femenino</option>
                  <option value="Other">Otro</option>
                  <option value="Prefer not to say">Prefiero no decirlo</option>
                </select>
                {errors.gender && (
                  <label className="label">
                    <span className="label-text-alt text-error">{errors.gender.message}</span>
                  </label>
                )}
              </div>

              {/* Smoker */}
              <div className="form-control w-full">
                <label className="label">
                  <span className="label-text">¿Fumador?</span>
                </label>
                <select
                  className={`select select-bordered w-full ${errors.smoker ? 'select-error' : ''}`}
                  {...register('smoker')}
                >
                  <option value="">Selecciona...</option>
                  <option value="true">Sí</option>
                  <option value="false">No</option>
                </select>
                {errors.smoker && (
                  <label className="label">
                    <span className="label-text-alt text-error">{errors.smoker.message}</span>
                  </label>
                )}
              </div>
            </div>

            {/* Hobbies */}
            <div className="form-control w-full">
              <label className="label">
                <span className="label-text">Hobbies</span>
              </label>
              <textarea
                className={`textarea textarea-bordered w-full ${errors.hobbies ? 'textarea-error' : ''}`}
                placeholder="Ej: Leer, cocinar, deporte..."
                rows={2}
                {...register('hobbies')}
              />
              {errors.hobbies && (
                <label className="label">
                  <span className="label-text-alt text-error">{errors.hobbies.message}</span>
                </label>
              )}
            </div>

            {/* Schedule */}
            <div className="form-control w-full">
              <label className="label">
                <span className="label-text">Horario / Rutina</span>
              </label>
              <textarea
                className={`textarea textarea-bordered w-full ${errors.schedule ? 'textarea-error' : ''}`}
                placeholder="Ej: Trabajo de 9 a 18, noches tranquilas..."
                rows={2}
                {...register('schedule')}
              />
              {errors.schedule && (
                <label className="label">
                  <span className="label-text-alt text-error">{errors.schedule.message}</span>
                </label>
              )}
            </div>

            {/* Password Change Section */}
            <div className="divider">Cambiar Contraseña</div>
            <p className="text-sm text-gray-500 -mt-2">Deja en blanco si no quieres cambiarla</p>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="form-control w-full">
                <label className="label">
                  <span className="label-text">Nueva Contraseña</span>
                </label>
                <input
                  type="password"
                  className={`input input-bordered w-full ${errors.password ? 'input-error' : ''}`}
                  placeholder="••••••••"
                  {...register('password')}
                />
                {errors.password && (
                  <label className="label">
                    <span className="label-text-alt text-error">{errors.password.message}</span>
                  </label>
                )}
              </div>

              <div className="form-control w-full">
                <label className="label">
                  <span className="label-text">Confirmar Contraseña</span>
                </label>
                <input
                  type="password"
                  className={`input input-bordered w-full ${errors.confirmPassword ? 'input-error' : ''}`}
                  placeholder="••••••••"
                  {...register('confirmPassword')}
                />
                {errors.confirmPassword && (
                  <label className="label">
                    <span className="label-text-alt text-error">
                      {errors.confirmPassword.message}
                    </span>
                  </label>
                )}
              </div>
            </div>

            <div className="form-control mt-6">
              <button
                type="submit"
                className={`btn btn-primary ${isSubmitting ? 'loading' : ''}`}
                disabled={isSubmitting}
              >
                {isSubmitting ? 'Guardando...' : 'Guardar Cambios'}
              </button>
            </div>
          </form>

          <div className="flex flex-col items-center gap-2 mt-4 w-full">
            <Link to="/profile" className="btn btn-ghost w-full">
              Cancelar
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}

