import { api } from './api'
import { AxiosError } from 'axios'
import { z } from 'zod'

export interface Apartment {
  id: number
  title: string
  description: string
  price: number
  bills: string
  ubication: string
  state: string
  coverImageUrl?: string
  idealTenantProfile?: string
}

export interface CreateApartmentPayload {
  title: string
  description: string
  price: number
  bills: string
  ubication: string
  state: string
  maxTenants: number
}

export const updateApartmentSchema = z.object({
  title: z
    .string()
    .min(1, 'El título es obligatorio')
    .max(100, 'El título no puede superar los 100 caracteres'),
  description: z
    .string()
    .min(1, 'La descripción es obligatoria')
    .max(1000, 'La descripción no puede superar los 1000 caracteres'),
  price: z
    .number({ message: 'El precio debe ser un número' })
    .positive('El precio debe ser mayor que 0'),
  bills: z
    .string()
    .max(255, 'Los gastos no pueden superar los 255 caracteres')
    .optional()
    .or(z.literal('')),
  ubication: z
    .string()
    .min(1, 'La ubicación es obligatoria')
    .max(255, 'La ubicación no puede superar los 255 caracteres'),
  state: z.enum(['ACTIVE', 'MATCHING', 'CLOSED'], {
    message: 'Estado de apartamento inválido',
  }),
  idealTenantProfile: z
    .string()
    .max(1000, 'El perfil ideal no puede superar los 1000 caracteres')
    .optional()
    .or(z.literal('')),
})

export type UpdateApartmentPayload = z.infer<typeof updateApartmentSchema>

/** Get ALL apartments (public) */
export const getApartments = async (): Promise<Apartment[]> => {
  const response = await api.get<Apartment[]>('/apartments')
  return response.data
}

/** Get only the authenticated user's apartments (RENTER role) */
export const getMyApartments = async (): Promise<Apartment[]> => {
  const response = await api.get<Apartment[]>('/apartments/my')
  return response.data
}

/** Get a single apartment by id */
export const getApartment = async (id: number): Promise<Apartment | undefined> => {
  try {
    const response = await api.get<Apartment>(`/apartments/${id}`)
    return response.data
  } catch (error) {
    console.error(error)
  }
}

/** Create a new apartment (auto-links as RENTER) */
export const createApartment = async (
  data: CreateApartmentPayload,
  images: File[],
): Promise<Apartment> => {
  const formData = new FormData()

  formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }))

  images.forEach((file) => {
    formData.append('images', file)
  })

  try {
    const response = await api.post<Apartment>('/apartments', formData)
    return response.data
  } catch (error) {
    const err = error as AxiosError
    console.error('[apartments.service] Error POST /apartments', {
      status: err.response?.status,
      data: err.response?.data,
      message: err.message,
    })
    throw error
  }
}

export const updateApartment = async (
  id: number,
  data: UpdateApartmentPayload
): Promise<Apartment> => {
  const payload = updateApartmentSchema.parse(data)
  const response = await api.put<Apartment>(`/apartments/${id}`, payload)
  return response.data
}
