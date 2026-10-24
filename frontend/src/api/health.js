import apiClient from './client'

export const fetchHealth = async () => {
  const response = await apiClient.get('/health')
  return response.data
}
