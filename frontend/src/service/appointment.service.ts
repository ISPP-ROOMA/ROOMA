import { api } from './api'
import type { UserDTO } from './apartment.service'

export interface CreateAvailabilityBlockDTO {
  blockDate: string
  startTime: string
  endTime: string
  slotDurationMinutes?: number
}

export interface AppointmentSlotDTO {
  id: number
  availabilityBlockId: number
  blockDate: string
  startTime: string
  endTime: string
  tenant: UserDTO | null
  status: 'AVAILABLE' | 'BOOKED' | 'CANCELED'
}

export interface AvailabilityBlockDTO {
  id: number
  apartmentId: number
  blockDate: string
  startTime: string
  endTime: string
  slotDurationMinutes: number
  slots: AppointmentSlotDTO[]
}

export async function createAvailabilityBlock(apartmentId: number, data: CreateAvailabilityBlockDTO): Promise<AvailabilityBlockDTO> {
  const response = await api.post<AvailabilityBlockDTO>(`/appointments/blocks/apartment/${apartmentId}`, data)
  return response.data
}

export async function getBlocksForApartment(apartmentId: number): Promise<AvailabilityBlockDTO[]> {
  const response = await api.get<AvailabilityBlockDTO[]>(`/appointments/blocks/apartment/${apartmentId}`)
  return response.data
}

export async function getAvailableSlots(apartmentId: number): Promise<AppointmentSlotDTO[]> {
  const response = await api.get<AppointmentSlotDTO[]>(`/appointments/apartment/${apartmentId}/available`)
  return response.data
}

export async function getAvailableSlotsForMatch(matchId: number): Promise<AppointmentSlotDTO[]> {
  const response = await api.get<AppointmentSlotDTO[]>(`/appointments/match/${matchId}/available`)
  return response.data
}

export async function bookAppointmentSlot(slotId: number): Promise<AppointmentSlotDTO> {
  const response = await api.post<AppointmentSlotDTO>(`/appointments/slots/${slotId}/book`)
  return response.data
}

export async function cancelAppointmentSlot(slotId: number): Promise<AppointmentSlotDTO> {
  const response = await api.post<AppointmentSlotDTO>(`/appointments/slots/${slotId}/cancel`)
  return response.data
}
