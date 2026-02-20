import { create } from "zustand"

interface LoginData {
  token: string
  role: string
}

interface AuthStore {
  token: string | null
  role: string | null
  login: (data: LoginData) => void
  logout: () => void
}

export const useAuthStore = create<AuthStore>((set) => ({
    token: null,
    userId: null,
    role: null,

    login: (data: LoginData) => {
        set({
            token: data.token,
            role: data.role,
        })
    },

    logout: () => {
        set({
            token: null,
            role: null,
        })
    }
}))
