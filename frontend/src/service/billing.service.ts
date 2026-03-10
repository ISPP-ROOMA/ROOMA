import { api } from './api'

export type DebtStatus = 'PENDING' | 'PAID'
export type BillStatus = 'PENDING' | 'PAID' | 'CANCELLED'

export interface UserSummary {
  id: number
  email: string
  profession?: string
}

export interface TenantDebtInBill {
  id: number
  amount: number
  status: DebtStatus
  user: UserSummary
}

export interface BillDTO {
  id: number
  reference: string
  totalAmount: number
  status: BillStatus
  duDate: string
  tenantDebts?: TenantDebtInBill[]
}

export interface TenantDebtDTO {
  id: number
  amount: number
  status: DebtStatus
  bill: BillDTO
}

export interface CreateBillPayload {
  reference: string
  totalAmount: number
  duDate: string
}

export interface ApartmentMemberDTO {
  id: number
  apartmentId: number
  userId: number
  role: string
  joinDate: string
}

export const getMyDebts = async (): Promise<TenantDebtDTO[]> => {
  try {
    const response = await api.get<TenantDebtDTO[]>(`/bills/me/debts`)
    return response.data
  } catch (error) {
    console.error('Error fetching debts:', error)
    return []
  }
}

export const payDebt = async (debtId: number): Promise<TenantDebtDTO | null> => {
  try {
    const response = await api.post<TenantDebtDTO>(`/bills/debts/${debtId}/pay`)
    return response.data
  } catch (error) {
    console.error(`Error paying debt ${debtId}:`, error)
    return null
  }
}

export const createBill = async (
  apartmentId: number,
  payload: CreateBillPayload
): Promise<BillDTO | null> => {
  try {
    const response = await api.post<BillDTO>(`/bills/apartment/${apartmentId}`, payload)
    return response.data
  } catch (error) {
    console.error('Error creating bill:', error)
    throw error
  }
}

export const getApartmentMembers = async (apartmentId: number): Promise<ApartmentMemberDTO[]> => {
  try {
    const response = await api.get<ApartmentMemberDTO[]>(`/apartments/${apartmentId}/members`)
    return response.data
  } catch (error) {
    console.error('Error fetching apartment members:', error)
    return []
  }
}

export const getApartmentBills = async (apartmentId: number): Promise<BillDTO[]> => {
  try {
    const response = await api.get<BillDTO[]>(`/bills/apartment/${apartmentId}`)
    return response.data
  } catch (error) {
    console.error('Error fetching apartment bills:', error)
    return []
  }
}
