import { useAuthStore } from "../store/authStore"
import { api } from "./api"

export interface LoginData {
  email: string
  password: string
  deviceId?: string
  role?: 'TENANT' | 'LANDLORD'
}

export interface AuthResponse {
  token: string
  role: string
  error?: string
}

export const generateDeviceId = () => {
    return crypto.randomUUID();
}

export const getDeviceId = () => {
    let deviceId = localStorage.getItem("deviceId")
    if(!deviceId) {
        deviceId = generateDeviceId()
        localStorage.setItem("deviceId", deviceId)
    }
    return deviceId
}

export const registerUser = async (loginData: LoginData): Promise<AuthResponse> => {
    try {
        const response = await api.post<AuthResponse>('/auth/register', loginData)
        return response.data
    } catch (error) {
        console.error(error)
        return {error: 'Invalid credentials', token: '', role: '' }
    }
}

export const loginUser = async (loginData: LoginData): Promise<AuthResponse> => {
    try {
        const response = await api.post<AuthResponse>('/auth/login', loginData)
        return response.data
    } catch (error) {
        console.error(error)
        return {error: 'Invalid credentials', token: '', role: '' }
    }
}

export const refreshToken = async (): Promise<AuthResponse | undefined> => {
    try {
        console.log("Ejecutando refresh")
        const response = await api.post<AuthResponse>('/auth/refresh', { deviceId: getDeviceId() })
        useAuthStore.getState()
            .login({
                token: response.data.token,
                role: response.data.role,
            })
        return response.data
    } catch (error) {
        useAuthStore.getState().logout()
        console.error(error)
    }
}

export const logout = async (): Promise<void> => {
    try {
        await api.post('/auth/logout', { deviceId: getDeviceId() })
        useAuthStore.getState().logout()
    } catch (error) {
        console.error(error)
        useAuthStore.getState().logout()
    }
}

export const validateToken = async (): Promise<any> => {
    try {
        const response = await api.get('/auth/validate')
        return response.data
    } catch (error) {
        console.error(error)
    }
}
