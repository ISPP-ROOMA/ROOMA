import { useAuthStore } from '../store/authStore'
import { api } from './api'

export type UserRole = 'TENANT' | 'LANDLORD' | 'ADMIN'

export interface LoginData {
  email: string
  password: string
  deviceId: string
  role?: UserRole
}

export interface AuthResponse {
  token: string
  role: UserRole
  error?: string
}

export interface ValidateResponse {
  authenticated: boolean
  message?: string
}

const SESSION_HINT_KEY = 'hasSessionHint'
let refreshInFlight: Promise<AuthResponse | undefined> | null = null

export const hasSessionHint = (): boolean => localStorage.getItem(SESSION_HINT_KEY) === '1'

const markSessionHint = (): void => {
  localStorage.setItem(SESSION_HINT_KEY, '1')
}

const clearSessionHint = (): void => {
  localStorage.removeItem(SESSION_HINT_KEY)
}

export const generateDeviceId = (): string => crypto.randomUUID()

export const getDeviceId = (): string => {
  let deviceId = localStorage.getItem('deviceId')
  if (!deviceId) {
    deviceId = generateDeviceId()
    localStorage.setItem('deviceId', deviceId)
  }
  return deviceId
}

export const registerUser = async (loginData: LoginData): Promise<AuthResponse> => {
  try {
    const payload = {
      ...loginData,
      role: loginData.role ?? 'TENANT',
    }
    const response = await api.post<AuthResponse>('/auth/register', payload)
    if (response.data.token) {
      markSessionHint()
    }
    return response.data
  } catch (error) {
    console.error(error)
    return { error: 'Invalid credentials', token: '', role: 'TENANT' }
  }
}

export const loginUser = async (loginData: LoginData): Promise<AuthResponse> => {
  try {
    const payload = {
      ...loginData,
      role: loginData.role ?? 'TENANT',
    }
    const response = await api.post<AuthResponse>('/auth/login', payload)
    if (response.data.token) {
      markSessionHint()
    }
    return response.data
  } catch (error) {
    console.error(error)
    return { error: 'Invalid credentials', token: '', role: 'TENANT' }
  }
}

export const refreshToken = async (): Promise<AuthResponse | undefined> => {
  if (refreshInFlight) {
    return refreshInFlight
  }

  refreshInFlight = (async () => {
    try {
      const response = await api.post<AuthResponse>('/auth/refresh', { deviceId: getDeviceId() })
      markSessionHint()
      useAuthStore.getState().login({ token: response.data.token, role: response.data.role })
      return response.data
    } catch (error) {
      console.error(error)
      clearSessionHint()
      return undefined
    } finally {
      refreshInFlight = null
    }
  })()

  return refreshInFlight
}

export const logout = async (): Promise<void> => {
  try {
    await api.post('/auth/logout', { deviceId: getDeviceId() })
  } catch (error) {
    console.error(error)
  } finally {
    clearSessionHint()
    useAuthStore.getState().logout()
  }
}

export const validateToken = async (): Promise<ValidateResponse | undefined> => {
  try {
    const response = await api.get<ValidateResponse>('/auth/validate')
    return response.data
  } catch (error) {
    console.error(error)
    clearSessionHint()
    useAuthStore.getState().logout()
    return undefined
  }
}
