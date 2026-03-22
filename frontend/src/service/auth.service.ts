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
  userId: number
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

export const generateDeviceId = (): string => {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }

  // Fallback for browsers/environments that do not implement randomUUID.
  return `device-${Date.now()}-${Math.random().toString(36).slice(2, 12)}`
}

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
      const userId = await fetchCurrentUserId(response.data.token)
      if (userId !== null) {
        response.data.userId = typeof userId === 'string' ? parseInt(userId, 10) : userId
      }
    }
    return response.data
  } catch (error) {
    console.error(error)
    return { error: 'Invalid credentials', token: '', role: 'TENANT', userId: 0 }
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
      const userId = await fetchCurrentUserId(response.data.token)
      if (userId !== null) {
        response.data.userId = typeof userId === 'string' ? parseInt(userId, 10) : userId
      }
    }
    return response.data
  } catch (error) {
    console.error(error)
    return { error: 'Invalid credentials', token: '', role: 'TENANT', userId: 0 }
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
      useAuthStore.getState().login({
        token: response.data.token,
        role: response.data.role,
        userId: response.data.userId,
      })
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

const fetchCurrentUserId = async (token: string): Promise<string | number | null> => {
  try {
    const response = await api.get<{ id?: string | number }>('/users/profile', {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })

    return response.data.id ?? null
  } catch (error) {
    console.error('Error fetching current user id:', error)
    return null
  }
}

export const ensureCurrentUserId = async (): Promise<string | number | null> => {
  const currentUserId = useAuthStore.getState().userId
  if (currentUserId) {
    return currentUserId
  }

  const token = useAuthStore.getState().token
  if (!token) {
    return null
  }

  const userId = await fetchCurrentUserId(token)
  if (userId !== null) {
    useAuthStore.setState({ userId })
  }

  return userId
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

export interface ValidateResponse {
  valid: boolean
  message?: string
  authenticated: boolean
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
