import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface User {
  id: string
  email: string
  firstName: string
  lastName: string
  role: 'ADMIN' | 'TRADER' | 'COMPLIANCE_OFFICER' | 'SUPPORT_AGENT'
}

interface AuthStore {
  user: User | null
  token: string | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (email: string, password: string) => Promise<void>
  logout: () => void
  setUser: (user: User) => void
}

// Mock authentication service
const authService = {
  login: async (email: string, password: string): Promise<{ user: User; token: string }> => {
    // Simulate API call delay
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    // Mock authentication logic
    if (email === 'admin@trademaster.com' && password === 'admin123') {
      return {
        user: {
          id: '1',
          email,
          firstName: 'Admin',
          lastName: 'User',
          role: 'ADMIN'
        },
        token: 'mock-jwt-token-admin'
      }
    } else if (email === 'trader@trademaster.com' && password === 'trader123') {
      return {
        user: {
          id: '2',
          email,
          firstName: 'John',
          lastName: 'Trader',
          role: 'TRADER'
        },
        token: 'mock-jwt-token-trader'
      }
    } else {
      throw new Error('Invalid credentials')
    }
  }
}

export const useAuthStore = create<AuthStore>()(
  persist(
    (set, get) => ({
      user: null,
      token: null,
      isAuthenticated: false,
      isLoading: false,

      login: async (email: string, password: string) => {
        set({ isLoading: true })
        try {
          const response = await authService.login(email, password)
          set({
            user: response.user,
            token: response.token,
            isAuthenticated: true,
            isLoading: false,
          })
        } catch (error) {
          set({ isLoading: false })
          throw error
        }
      },

      logout: () => {
        set({
          user: null,
          token: null,
          isAuthenticated: false,
          isLoading: false,
        })
      },

      setUser: (user: User) => {
        set({ user })
      },
    }),
    {
      name: 'trademaster-auth',
      partialize: (state) => ({
        user: state.user,
        token: state.token,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
)