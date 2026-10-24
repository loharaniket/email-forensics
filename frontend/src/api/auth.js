import apiClient from './client'

export const loginApi = async (username, password) => {
  const response = await apiClient.post('/auth/login', { username, password })
  return response.data
}

export const getMeApi = async () => {
  const response = await apiClient.get('/auth/me')
  return response.data
}
