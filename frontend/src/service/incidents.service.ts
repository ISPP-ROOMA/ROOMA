import { api } from './api'
import { AxiosError } from 'axios'

export type IncidentStatus =
  | 'OPEN'
  | 'RECEIVED'
  | 'IN_PROGRESS'
  | 'TECHNICIAN_NOTIFIED'
  | 'RESOLVED'
  | 'CLOSED'
  | 'CLOSED_INACTIVITY'

export type IncidentCategory =
  | 'PLUMBING'
  | 'ELECTRICITY'
  | 'APPLIANCES'
  | 'LOCKSMITH'
  | 'PAINT_WALLS'
  | 'CLIMATE'
  | 'PESTS'
  | 'OTHER'

export type IncidentUrgency = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'

export type IncidentZone =
  | 'KITCHEN'
  | 'BATHROOM'
  | 'LIVING_ROOM'
  | 'BEDROOM'
  | 'COMMON_AREAS'
  | 'EXTERIOR'

export interface IncidentStatusHistoryDTO {
  status: IncidentStatus
  changedAt: string
  userId: number
  userEmail: string
}

export interface IncidentDTO {
  id: number
  incidentCode: string
  apartmentId: number
  tenantId: number
  tenantEmail: string
  landlordId: number
  landlordEmail: string
  title: string
  description: string
  category: IncidentCategory
  zone: IncidentZone
  urgency: IncidentUrgency
  status: IncidentStatus
  photos: string[]
  rejectionReason?: string
  createdAt: string
  updatedAt: string
  resolvedAt?: string
  closedAt?: string
  statusHistory: IncidentStatusHistoryDTO[]
}

export interface CreateIncidentPayload {
  title: string
  description: string
  category: IncidentCategory
  zone: IncidentZone
  urgency: IncidentUrgency
}

export interface LandlordUpdateIncidentStatusPayload {
  status: IncidentStatus
}

export const getApartmentIncidents = async (
  apartmentId: number,
  bucket?: 'ACTIVE' | 'CLOSED'
): Promise<IncidentDTO[]> => {
  try {
    const response = await api.get<IncidentDTO[]>(`/apartments/${apartmentId}/incidents`, {
      params: bucket ? { bucket } : undefined,
    })
    return response.data ?? []
  } catch (error) {
    const err = error as AxiosError
    if (err.response?.status === 403 || err.response?.status === 404) {
      throw error
    }
    console.error('Error fetching incidents:', error)
    return []
  }
}

export const getIncidentById = async (
  apartmentId: number,
  incidentId: number
): Promise<IncidentDTO | null> => {
  try {
    const response = await api.get<IncidentDTO>(`/apartments/${apartmentId}/incidents/${incidentId}`)
    return response.data
  } catch (error) {
    console.error('Error fetching incident detail:', error)
    return null
  }
}

export const createIncident = async (
  apartmentId: number,
  payload: CreateIncidentPayload,
  images: File[]
): Promise<IncidentDTO> => {
  const formData = new FormData()
  formData.append('data', new Blob([JSON.stringify(payload)], { type: 'application/json' }))
  images.forEach((image) => {
    formData.append('images', image)
  })

  const response = await api.post<IncidentDTO>(`/apartments/${apartmentId}/incidents`, formData)
  return response.data
}

export const landlordUpdateIncidentStatus = async (
  apartmentId: number,
  incidentId: number,
  payload: LandlordUpdateIncidentStatusPayload
): Promise<IncidentDTO> => {
  const response = await api.patch<IncidentDTO>(
    `/apartments/${apartmentId}/incidents/${incidentId}/status`,
    payload
  )
  return response.data
}

export const confirmIncidentSolution = async (
  apartmentId: number,
  incidentId: number
): Promise<IncidentDTO> => {
  const response = await api.patch<IncidentDTO>(
    `/apartments/${apartmentId}/incidents/${incidentId}/confirm-solution`
  )
  return response.data
}

export const rejectIncidentSolution = async (
  apartmentId: number,
  incidentId: number,
  reason: string
): Promise<IncidentDTO> => {
  const response = await api.patch<IncidentDTO>(
    `/apartments/${apartmentId}/incidents/${incidentId}/reject-solution`,
    { reason }
  )
  return response.data
}
