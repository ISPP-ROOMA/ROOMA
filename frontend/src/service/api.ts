import axios, { AxiosError, type AxiosResponse } from "axios"
import { useAuthStore } from "../store/authStore"
import { refreshToken } from "./auth.service"

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
    const originalRequest = error.config as any

    if (originalRequest?.url?.includes("/refresh")) {
      return Promise.reject(error)
    }

    if (error.response?.status === 401 && !originalRequest?._retry) {
      originalRequest._retry = true
      console.log("refresco automático")
      await refreshToken() 
      console.log("Se ha refrescado el token")
      return api(originalRequest)
    }
    return Promise.reject(error)
  }
)
