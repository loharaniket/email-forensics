import React, { createContext, useContext, useState, useEffect } from 'react'
import { loginApi, getMeApi } from '../api/auth'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('auth_token'))
  const [user, setUser] = useState(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const initializeAuth = async () => {
      const storedToken = localStorage.getItem('auth_token')
      if (storedToken) {
        try {
          const profile = await getMeApi()
          setUser(profile)
          setToken(storedToken)
        } catch {
          localStorage.removeItem('auth_token')
          setToken(null)
          setUser(null)
        }
      }
      setIsLoading(false)
    }

    initializeAuth()
  }, [])

  const login = async (username, password) => {
    const response = await loginApi(username, password)
    localStorage.setItem('auth_token', response.accessToken)
    setToken(response.accessToken)
    setUser(response.analyst)
    return response
  }

  const logout = () => {
    localStorage.removeItem('auth_token')
    setToken(null)
    setUser(null)
  }

  return (
    <AuthContext.Provider
      value={{
        token,
        user,
        isAuthenticated: !!token && !!user,
        isLoading,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
