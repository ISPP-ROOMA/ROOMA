import { create } from 'zustand'

interface LoginData {
  token: string
  role: string
  userId?: string | number | null
}

interface AuthStore {
  token: string | null
  role: string | null
  userId: string | number | null
  login: (data: LoginData) => void
  logout: () => void
}

export const useAuthStore = create<AuthStore>((set) => ({
  token: null,
  role: null,
  userId: null,

  login: (data: LoginData) => {
    set({
      token: data.token,
      role: data.role,
      userId: data.userId ?? null,
    })
  },

  logout: () => {
    set({
      token: null,
      role: null,
      userId: null,
    })
  },
}))
