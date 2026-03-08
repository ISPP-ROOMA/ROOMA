import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { getUserProfile, updateUserProfile } from '../../service/users.service'
import type { UpdateUserPayload, User } from '../../service/users.service'
import { useAuthStore } from '../../store/authStore'

const profileSchema = z.object({
    name: z.string().optional(),
    surname: z.string().optional(),
    email: z.string().email({ message: 'Email inválido' }),
    role: z.string(),
    phone: z.string().optional(),
    birthDate: z.string().optional(),
    profilePic: z.string().url({ message: 'Debe ser una URL válida' }).optional().or(z.literal('')),
    gender: z.string().optional(),
    smoker: z.string().optional(),
})

type ProfileFormValues = z.infer<typeof profileSchema>

export default function ProfileEdit() {
    const navigate = useNavigate()
    const { role } = useAuthStore()
    const [isLoading, setIsLoading] = useState(true)
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [userData, setUserData] = useState<User | null>(null)

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
                    reset({
                        name: res.name || '',
                        surname: res.surname || '',
                        email: res.email || '',
                        role: res.role || role || 'TENANT',
                        phone: res.phone || '',
                        birthDate: res.birthDate ? new Date(res.birthDate).toISOString().split('T')[0] : '',
                        profilePic: res.profilePic || '',
                        gender: res.gender || '',
                        smoker: res.smoker !== undefined ? String(res.smoker) : '',
                    })
                }
            } catch (error) {
                console.error('Error fetching profile', error)
            } finally {
                setIsLoading(false)
            }
        }
        fetchProfile()
    }, [reset, role])

    const onSubmit = async (data: ProfileFormValues) => {
        setIsSubmitting(true)

        const payload: UpdateUserPayload = {
            name: data.name,
            surname: data.surname,
            email: data.email,
            role: data.role,
            phone: data.phone,
            birthDate: data.birthDate ? new Date(data.birthDate) : undefined,
            profilePic: data.profilePic || undefined,
            gender: data.gender,
            smoker: data.smoker === 'true' ? true : data.smoker === 'false' ? false : undefined,
        }

        const updated = await updateUserProfile(payload)
        if (updated && !('error' in updated)) {
            navigate('/profile')
        } else {
            alert('Error actualizando perfil')
        }
        setIsSubmitting(false)
    }

    if (isLoading) return <p className="text-center mt-10 text-gray-500">Cargando...</p>

    if (!userData) return <p className="text-center mt-10 text-red-500">Error al cargar el perfil</p>

    return (
        <div className="flex flex-col items-center justify-center min-h-[70vh] p-4">
            <div className="card w-full max-w-2xl bg-base-100 shadow-lg">
                <div className="card-body">
                    <h2 className="text-2xl font-bold mb-6 text-center">Editar Perfil</h2>

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

                            {/* Profile Pic URL */}
                            <div className="form-control w-full">
                                <label className="label">
                                    <span className="label-text">URL de Foto de Perfil</span>
                                </label>
                                <input
                                    type="text"
                                    placeholder="https://..."
                                    className={`input input-bordered w-full ${errors.profilePic ? 'input-error' : ''}`}
                                    {...register('profilePic')}
                                />
                                {errors.profilePic && (
                                    <label className="label">
                                        <span className="label-text-alt text-error">{errors.profilePic.message}</span>
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
