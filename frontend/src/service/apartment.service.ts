import { api } from './api'

export interface UserDTO {
  id: number
  email: string
  role: string
  name?: string
  gender?: string
  smoker?: boolean
  profileImageUrl?: string
  hobbies?: string
  schedule?: string
  profession?: string
}

export interface ApartmentMemberDTO {
  id: number
  apartmentId: number
  userId: number
  userEmail: string
  role: string
  joinDate: string
}

export interface RoommateDTO {
  memberId: number
  userId: number
  email: string
  profession?: string
  hobbies?: string
  schedule?: string
  profileImageUrl?: string
  memberRole: string
  joinDate: string
  currentUser: boolean
}

export interface BillingSummaryDTO {
  pendingDebts: number
  pendingAmount: number
  nextDueDate?: string
  nextReference?: string
}

export interface ApartmentPhotoDTO {
  id: number
  url: string
  publicId: string
  orden: number
  portada: boolean
}

export interface ApartmentRulesDTO {
  permiteMascotas: boolean
  permiteFumadores: boolean
  fiestasPermitidas: boolean
}

export interface ApartmentDTO {
  imageUrl: string
  id: number
  title: string
  description: string
  price: number
  bills: string
  ubication: string
  state: string
  coverImageUrl?: string
  members?: ApartmentMemberDTO[]
  idealTenantProfile?: string
}

export interface ApartmentHomeDTO {
  apartment: ApartmentDTO
  roommates: RoommateDTO[]
  photos: ApartmentPhotoDTO[]
  billing: BillingSummaryDTO
}

export interface SwipeActionDTO {
  interest: boolean
}

export type MatchStatus = 'ACTIVE' | 'MATCH' | 'INVITED' | 'REJECTED' | 'SUCCESSFUL' | 'CANCELED'

export interface ApartmentMatchDTO {
  id: number
  candidateId: number
  apartmentId: number
  matchStatus: MatchStatus
}

export interface ApartmentMatchTenantDetailsDTO {
  id: number
  tenant: UserDTO
  apartment: ApartmentDTO
  matchStatus: MatchStatus
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

export const swipeApartment = async (apartmentId: number, interest: boolean): Promise<unknown> => {
  try {
    const response = await api.post(
      `/apartments-matches/swipe/apartment/${apartmentId}/tenant`,
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

export const updateApartmentRules = async (
  apartmentId: number,
  rules: ApartmentRulesDTO
): Promise<void> => {
  await api.put(`/apartments/${apartmentId}/rules`, rules)
}

export const uploadApartmentImages = async (
  apartmentId: number,
  files: File[],
  replace: boolean
): Promise<void> => {
  if (!files.length) return
  const formData = new FormData()
  files.forEach((file) => formData.append('files', file))
  formData.append('replace', String(replace))

  await api.post(`/images/apartment/${apartmentId}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
export const getMatchesForCandidate = async (
  candidateId: number,
  status: MatchStatus
): Promise<ApartmentMatchDTO[]> => {
  try {
    const response = await api.get<ApartmentMatchDTO[]>(
      `/apartments-matches/candidate/${candidateId}/status/${status}`
    )
    return response.data
  } catch (error) {
    console.error('Error fetching matches:', error)
    return []
  }
}

interface ApartmentMatchLandlordResponseDTO {
  id: number
  apartment: { id: number }
  matchStatus: MatchStatus
}

export const getMatchesForLandlord = async (
  landlordId: number,
  status: MatchStatus
): Promise<ApartmentMatchDTO[]> => {
  try {
    const response = await api.get<ApartmentMatchLandlordResponseDTO[]>(
      `/apartments-matches/${landlordId}/interested-candidates/${status}`
    )
    return response.data.map((item: ApartmentMatchLandlordResponseDTO) => ({
      id: item.id,
      candidateId: 0,
      apartmentId: item.apartment.id,
      matchStatus: item.matchStatus,
    }))
  } catch (error) {
    console.error('Error fetching landlord matches:', error)
    return []
  }
}

export const getAllApartments = async (): Promise<ApartmentDTO[]> => {
  try {
    const response = await api.get<ApartmentDTO[]>(`/apartments`)
    return response.data
  } catch (error) {
    console.error('Error fetching all apartments:', error)
    return []
  }
}

export const getMyHomeSnapshot = async (): Promise<ApartmentHomeDTO | null> => {
  try {
    const response = await api.get<ApartmentHomeDTO>(`/apartments/me/home`)
    return response.data
  } catch (error) {
    console.error('Error fetching my home snapshot:', error)
    return null
  }
}
export const cancelApartmentMatch = async (matchId: number): Promise<void> => {
  await api.patch(`/apartments-matches/apartmentMatch/${matchId}/status/canceled`)
}

export const rejectApartmentMatch = async (matchId: number): Promise<void> => {
  await api.post(`/apartments-matches/apartmentMatch/${matchId}/respond-request?interest=false`)
}

export const acceptApartmentMatch = async (matchId: number): Promise<void> => {
  await api.post(`/apartments-matches/apartmentMatch/${matchId}/respond-request?interest=true`)
}

export const respondToInvitation = async (
  apartmentMatchId: number,
  accepted: boolean
): Promise<void> => {
  await api.post(
    `/apartments-matches/apartmentMatch/${apartmentMatchId}/respond-invitation`,
    accepted,
    {
      headers: { 'Content-Type': 'application/json' },
    }
  )
}

export const sendInvitationToMatch = async (apartmentMatchId: number): Promise<void> => {
  await api.post(`/apartments-matches/apartmentMatch/${apartmentMatchId}/send-invitation`)
}

export const getLandlordMatchDetails = async (
  apartmentMatchId: number
): Promise<ApartmentMatchTenantDetailsDTO | null> => {
  try {
    const response = await api.get<ApartmentMatchTenantDetailsDTO>(
      `/apartments-matches/apartmentMatch/${apartmentMatchId}/landlord-match-details`
    )
    return response.data
  } catch (error) {
    console.error('Error fetching landlord match details:', error)
    return null
  }
}

export const getDeckForCandidate = async (candidateId: number): Promise<ApartmentDTO[]> => {
  try {
    const response = await api.get<ApartmentDTO[]>(`/apartments/deck/${candidateId}`)
    return response.data
  } catch (error) {
    console.error('Error fetching deck:', error)
    return []
  }
}
