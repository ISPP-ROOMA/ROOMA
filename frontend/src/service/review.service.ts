import { api } from './api'

export interface ApartmentInfo {
  id: number
  title: string
  ubication: string
  ownerEmail: string
  ownerId: number
  members: { userId: number; joinDate: string; endDate?: string }[]
}

export interface CreateReviewPayload {
  reviewedUserId: number
  apartmentId: number
  rating: number
  comment: string
}

export interface ReviewDTO {
  id: number
  rating: number
  comment: string
  response?: string
  reviewerUserId: number
  reviewerEmail: string
  reviewedUserId: number
  reviewedEmail: string
  apartmentId: number
  published: boolean
  reviewDate: string
}

export interface ReviewableUser {
  id: number
  email: string
  role: string
  hasReviewedYou: boolean
  youReviewedThem: boolean
}

export interface PendingReviewApartment {
  apartmentId: number
  apartmentTitle: string
  apartmentUbication: string
  pendingUsers: ReviewableUser[]
}

export const getApartmentInfo = async (apartmentId: number): Promise<ApartmentInfo | null> => {
  try {
    const res = await api.get(`/apartments/${apartmentId}`)
    const apt = res.data
    return {
      id: apt.id,
      title: apt.title,
      ubication: apt.ubication,
      ownerEmail: apt.user?.email ?? 'Desconocido',
      ownerId: apt.user?.id ?? 0,
      members: (apt.members ?? []).map(
        (m: { userId: number; joinDate: string; endDate?: string }) => ({
          userId: m.userId,
          joinDate: m.joinDate,
          endDate: m.endDate,
        })
      ),
    }
  } catch (error) {
    console.error('Error fetching apartment info:', error)
    return null
  }
}

export const submitReviewAsTenant = async (payload: CreateReviewPayload): Promise<ReviewDTO> => {
  const res = await api.post<ReviewDTO>('/reviews/tenant', payload)
  return res.data
}

export const submitReviewAsLandlord = async (payload: CreateReviewPayload): Promise<ReviewDTO> => {
  const res = await api.post<ReviewDTO>('/reviews/landlord', payload)
  return res.data
}

export const getReceivedReviews = async (): Promise<ReviewDTO[]> => {
  try {
    const res = await api.get<ReviewDTO[]>('/reviews/received')
    return res.data
  } catch (error) {
    console.error('Error fetching received reviews:', error)
    return []
  }
}

export const getMadeReviews = async (): Promise<ReviewDTO[]> => {
  try {
    const res = await api.get<ReviewDTO[]>('/reviews/made')
    return res.data
  } catch (error) {
    console.error('Error fetching made reviews:', error)
    return []
  }
}

export const getReviewableUsers = async (apartmentId: number): Promise<ReviewableUser[]> => {
  try {
    const res = await api.get<ReviewableUser[]>(`/reviews/reviewable/${apartmentId}`)
    return res.data
  } catch (error) {
    console.error('Error fetching reviewable users:', error)
    return []
  }
}

export const getPendingReviewApartments = async (): Promise<PendingReviewApartment[]> => {
  try {
    const res = await api.get<PendingReviewApartment[]>('/reviews/pending')
    return res.data
  } catch (error) {
    console.error('Error fetching pending review apartments:', error)
    return []
  }
}

export const respondToReview = async (
  reviewId: number,
  response: string
): Promise<ReviewDTO | null> => {
  try {
    const res = await api.put<ReviewDTO>(`/reviews/${reviewId}/respond`, { response })
    return res.data
  } catch (error) {
    console.error('Error responding to review:', error)
    return null
  }
}
