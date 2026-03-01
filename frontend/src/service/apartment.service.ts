import { api } from './api'

export interface UserDTO {
  id: number
  email: string
  role: string
  hobbies?: string
  schedule?: string
  profession?: string
}

export interface ApartmentMemberDTO {
  id: number
  apartmentId: number
  userId: number
  role: string
  joinDate: string
}

export interface ApartmentPhotoDTO {
  id: number
  url: string
  publicId: string
  orden: number
  portada: boolean
}

export interface ApartmentDTO {
  id: number
  title: string
  description: string
  price: number
  bills: string
  ubication: string
  state: string
  coverImageUrl?: string
  members?: ApartmentMemberDTO[]
}

export interface SwipeActionDTO {
  interest: boolean
}

export const searchApartments = async (
  ubication?: string,
  minPrice?: number,
  maxPrice?: number,
  state?: string
): Promise<ApartmentDTO[]> => {
  try {
    const params = new URLSearchParams()
    if (ubication) params.append('ubication', ubication)
    if (minPrice) params.append('minPrice', minPrice.toString())
    if (maxPrice) params.append('maxPrice', maxPrice.toString())
    if (state) params.append('state', state)

    const response = await api.get<ApartmentDTO[]>(`/apartments/search?${params.toString()}`)
    return response.data
  } catch (error) {
    console.error('Error fetching apartments:', error)
    return []
  }
}

export const swipeApartment = async (
  candidateId: number,
  apartmentId: number,
  interest: boolean
): Promise<unknown> => {
  try {
    const response = await api.post(
      `/apartments-matches/swipe/candidate/${candidateId}/apartment/${apartmentId}/action/${interest}`,
      interest,
      { headers: { 'Content-Type': 'application/json' } }
    )
    return response.data
  } catch (error) {
    console.error('Error swiping apartment:', error)
    throw error
  }
}

export const getApartmentPhotos = async (apartmentId: number): Promise<ApartmentPhotoDTO[]> => {
  try {
    const response = await api.get<{ apartment: ApartmentDTO; images: ApartmentPhotoDTO[] }>(
      `/apartments/${apartmentId}/photos`
    )
    return response.data.images ?? []
  } catch (error) {
    console.error('Error fetching apartment photos:', error)
    return []
  }
}
