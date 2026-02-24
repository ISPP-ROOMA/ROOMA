import axios, { AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '../store/authStore'
import { refreshToken } from './auth.service'

interface CustomAxiosRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean
}

const API_BASE = (import.meta.env.VITE_API_URL as string) ?? 'http://localhost:8080'

export const api = axios.create({
  baseURL: `${API_BASE}/api`,
  withCredentials: true,
})

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as CustomAxiosRequestConfig

    if (
      originalRequest?.url?.includes('/refresh') ||
      originalRequest?.url?.includes('/login') ||
      originalRequest?.url?.includes('/register')
    ) {
      return Promise.reject(error)
    }

    if (error.response?.status === 401 && !originalRequest?._retry) {
      originalRequest._retry = true
      const refreshed = await refreshToken()
      if (!refreshed?.token) {
        return Promise.reject(error)
      }
      return api(originalRequest)
    }
    return Promise.reject(error)
  }
)
