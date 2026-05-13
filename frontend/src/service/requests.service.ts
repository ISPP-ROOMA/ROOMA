import { useAuthStore } from '../store/authStore'
import { api } from './api'

export type RequestStatus = 'PENDING' | 'WAITING' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED'
export type ApartmentStatus = 'FREE' | 'PAUSED' | 'RENTED'
type BackendMatchStatus = 'ACTIVE' | 'WAITING' | 'MATCH' | 'REJECTED' | 'SUCCESSFUL' | 'CANCELED'

type BackendApartmentDTO = {
  id: number
  title?: string
  ubication?: string
  state?: string
  price?: number
  coverImageUrl?: string
}

type BackendUserDTO = {
  id: number
  name?: string
  username?: string
  email?: string
}

type TenantRequestDTO = {
  id: number
  apartment: BackendApartmentDTO
  matchStatus: BackendMatchStatus
  createdAt?: string
}

type LandlordRequestDTO = {
  id: number
  user: BackendUserDTO
  apartment: BackendApartmentDTO
  matchStatus: BackendMatchStatus
  createdAt?: string
}

type FilteredCandidateDTO = {
  id: number
  matchStatus: BackendMatchStatus
  apartment?: BackendApartmentDTO
  tenantHasOpenedMatchDetails?: boolean
}

export interface FilteredCandidateItem {
  id: number
  apartmentId: number | null
  matchStatus: BackendMatchStatus
  tenantHasOpenedMatchDetails?: boolean
  apartment?: {
    id: number
    title?: string
    ubication?: string
    price?: number
    coverImageUrl?: string
  }
}

export interface RequestItem {
  id: number
  apartmentId: number
  apartmentTitle: string
  apartmentAddress: string
  apartmentStatus: ApartmentStatus
  tenantName: string
  landlordName: string
  createdAt: string
  status: RequestStatus
  monthlyPrice?: number
}
export interface CandidateFilter {
  minAge?: number
  maxAge?: number
  requiredProfession?: string
  allowedSmoker?: boolean
  requiredSchedule?: string
}

function mapBackendStatus(status: BackendMatchStatus): RequestStatus {
  switch (status) {
    case 'ACTIVE':
      return 'PENDING'
    case 'WAITING':
      return 'WAITING'
    case 'MATCH':
    case 'SUCCESSFUL':
      return 'ACCEPTED'
    case 'REJECTED':
      return 'REJECTED'
    case 'CANCELED':
      return 'CANCELLED'
  }
}

function mapApartmentStatus(state?: string): ApartmentStatus {
  const normalizedState = state?.toUpperCase()

  if (normalizedState === 'PAUSED' || normalizedState === 'PAUSADO') {
    return 'PAUSED'
  }

  if (normalizedState === 'RENTED' || normalizedState === 'ALQUILADO') {
    return 'RENTED'
  }

  return 'FREE'
}

function formatRequestDate(date?: string): string {
  return date ?? 'Sin fecha'
}

function getLandlordDisplayName(apartment: BackendApartmentDTO): string {
  if (apartment.title) {
    return 'Casero'
  }
  return 'Casero'
}

function getTenantDisplayName(user?: BackendUserDTO): string {
  if (!user) {
    return 'Inquilino'
  }
  return user.name ?? user.username ?? user.email ?? `Inquilino #${user.id}`
}

function mapTenantRequest(dto: TenantRequestDTO): RequestItem {
  return {
    id: dto.id,
    apartmentId: dto.apartment.id,
    apartmentTitle: dto.apartment.title ?? `Apartamento #${dto.apartment.id}`,
    apartmentAddress: dto.apartment.ubication ?? 'Ubicación no disponible',
    apartmentStatus: mapApartmentStatus(dto.apartment.state),
    tenantName: 'Tú',
    landlordName: getLandlordDisplayName(dto.apartment),
    createdAt: formatRequestDate(dto.createdAt),
    status: mapBackendStatus(dto.matchStatus),
    monthlyPrice: dto.apartment.price,
  }
}

function mapLandlordRequest(dto: LandlordRequestDTO): RequestItem {
  return {
    id: dto.id,
    apartmentId: dto.apartment.id,
    apartmentTitle: dto.apartment.title ?? `Apartamento #${dto.apartment.id}`,
    apartmentAddress: dto.apartment.ubication ?? 'Ubicación no disponible',
    apartmentStatus: mapApartmentStatus(dto.apartment.state),
    tenantName: getTenantDisplayName(dto.user),
    landlordName: 'Tú',
    createdAt: formatRequestDate(dto.createdAt),
    status: mapBackendStatus(dto.matchStatus),
    monthlyPrice: dto.apartment.price,
  }
}

export async function getSentRequests(): Promise<RequestItem[]> {
  try {
    const response = await api.get<TenantRequestDTO[]>('/apartments-matches/my/my-requests/ACTIVE')
    return response.data.map(mapTenantRequest)
  } catch (error) {
    console.error('Error fetching tenant requests:', error)
    return []
  }
}

export async function getReceivedRequests(): Promise<RequestItem[]> {
  const userId = useAuthStore.getState().userId

  if (!userId) {
    return []
  }

  try {
    const response = await api.get<LandlordRequestDTO[]>(
      `/apartments-matches/${userId}/interested-candidates/ACTIVE`
    )
    return response.data.map(mapLandlordRequest)
  } catch (error) {
    console.error('Error fetching landlord requests:', error)
    return []
  }
}

export async function acceptRequest(apartmentMatchId: number): Promise<void> {
  await api.patch(
    `/apartments-matches/apartmentMatch/${apartmentMatchId}/landlord-decision?decision=ACCEPT`
  )
}

export async function rejectRequest(apartmentMatchId: number): Promise<void> {
  await api.patch(
    `/apartments-matches/apartmentMatch/${apartmentMatchId}/landlord-decision?decision=REJECT`
  )
}

export async function waitRequest(apartmentMatchId: number): Promise<void> {
  await api.patch(
    `/apartments-matches/apartmentMatch/${apartmentMatchId}/landlord-decision?decision=WAIT`
  )
}

export async function getFilteredCandidates(
  apartmentId: number,
  filter: CandidateFilter
): Promise<FilteredCandidateItem[]> {
  const response = await api.post<FilteredCandidateDTO[]>(
    `/apartments-matches/apartment/${apartmentId}/filtered-candidates`,
    filter
  )
  return (response.data || []).map((item) => ({
    id: item.id,
    apartmentId: item.apartment?.id ?? null,
    matchStatus: item.matchStatus,
    tenantHasOpenedMatchDetails: item.tenantHasOpenedMatchDetails,
    apartment: item.apartment
      ? {
          id: item.apartment.id,
          title: item.apartment.title,
          ubication: item.apartment.ubication,
          price: item.apartment.price,
          coverImageUrl: item.apartment.coverImageUrl,
        }
      : undefined,
  }))
}
