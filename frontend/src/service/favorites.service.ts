import { api } from './api'

export type FavoriteAvailabilityStatus = 'AVAILABLE' | 'CLOSED'

export interface FavoriteItem {
  apartmentId: number
  title: string
  city: string
  price: number
  mainImageUrl?: string
  isFavorite: boolean
  availabilityStatus: FavoriteAvailabilityStatus
  detailAccessible: boolean
  statusMessage?: string | null
}

export interface FavoriteToggleResponse {
  apartmentId: number
  isFavorite: boolean
  message?: string
}

export interface FavoriteIdsResponse {
  favoriteApartmentIds: number[]
}

export const getFavorites = async (): Promise<FavoriteItem[]> => {
  const response = await api.get<FavoriteItem[]>('/favorites')
  return response.data
}

export const addFavorite = async (apartmentId: number): Promise<FavoriteToggleResponse> => {
  const response = await api.put<FavoriteToggleResponse>(`/favorites/${apartmentId}`)
  return response.data
}

export const removeFavorite = async (apartmentId: number): Promise<FavoriteToggleResponse> => {
  const response = await api.delete<FavoriteToggleResponse>(`/favorites/${apartmentId}`)
  return response.data
}

export const getFavoriteApartmentIds = async (apartmentIds: number[]): Promise<number[]> => {
  if (!apartmentIds.length) {
    return []
  }

  const params = new URLSearchParams()
  params.set('apartmentIds', apartmentIds.join(','))

  const response = await api.get<FavoriteIdsResponse>(`/favorites/ids?${params.toString()}`)
  return response.data.favoriteApartmentIds ?? []
}
