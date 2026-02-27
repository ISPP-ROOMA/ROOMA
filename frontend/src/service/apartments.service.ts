import { api } from './api'

export interface Apartment {
  id: number
  title: string
  description: string
  price: number
  bills: string
  ubication: string
  state: string
  coverImageUrl?: string
}

export interface CreateApartmentPayload {
  title: string
  description: string
  price: number
  bills: string
  ubication: string
  state: string
}

export interface UpdateApartmentPayload {
  title: string
  description: string
  price: number
  bills: string
  ubication: string
  state: string
}

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
  images: File[]
): Promise<Apartment> => {
  const formData = new FormData()
  
  formData.append(
    'data',
    new Blob([JSON.stringify(data)], { type: 'application/json' })
  )
  
  images.forEach((file) => {
    formData.append('images', file)
  })
  
  try {
    const response = await api.post<Apartment>('/apartments', formData)
    return response.data
  } catch (error: any) {
    console.error('[apartments.service] Error POST /apartments', {
      status: error?.response?.status,
      data: error?.response?.data,
      message: error?.message,
    })
    throw error
  }
}

export const updateApartment = async (
  id: number,
  data: UpdateApartmentPayload
): Promise<Apartment> => {
  const response = await api.put<Apartment>(`/apartments/${id}`, data)
  return response.data
}
