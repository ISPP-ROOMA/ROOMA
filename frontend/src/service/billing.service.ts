import { api } from './api'

export type DebtStatus = 'PENDING' | 'PAID'
export type BillStatus = 'PENDING' | 'PAID' | 'CANCELLED'

export interface BillDTO {
  id: number
  reference: string
  totalAmount: number
  status: BillStatus
  duDate: string
}

export interface TenantDebtDTO {
  id: number
  amount: number
  status: DebtStatus
  bill: BillDTO
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
