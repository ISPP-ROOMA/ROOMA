import axios from 'axios'
import { api } from './api'

export interface User {
  id: string | number
  email: string
  role: string
  name?: string
}

export interface UsersResponse {
  users: User[]
  page: number
  totalPages: number
}

export interface UserResponse {
  email: string
  role: string
  error?: string
}

export type UpdateUserPayload = {
  email: string
  role: string
  password?: string
}

export const getUserProfile = async (): Promise<User | undefined> => {
  try {
    const response = await api.get<User>('/users/profile')
    return response.data
  } catch (error) {
    console.error(error)
    return undefined
  }
}

export const getUsers = async (page: number): Promise<UsersResponse | undefined> => {
  try {
    const response = await api.get<UsersResponse>(`/users?page=${page}`)
    return response.data
  } catch (error) {
    console.error(error)
    return undefined
  }
}

export const getUser = async (id: string | number): Promise<User | undefined> => {
  try {
    const response = await api.get<User>(`/users/${id}`)
    return response.data
  } catch (error) {
    console.error(error)
    return undefined
  }
}

export const updateUser = async (
  id: string | number,
  data: UpdateUserPayload
): Promise<UserResponse> => {
  try {
    const response = await api.put<UserResponse>(`/users/${id}`, data)
    return response.data
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      const status = error.response.status
      const resData = error.response.data as { message?: string }

      if (status === 400 && resData.message) {
        return { error: resData.message, email: '', role: '' }
      }

      return { error: 'Error inesperado', email: '', role: '' }
    }

    return { error: 'No se pudo conectar con el servidor', email: '', role: '' }
  }
}
