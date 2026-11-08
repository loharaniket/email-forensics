import apiClient from './client'

export const fetchHealth = async () => {
  const response = await apiClient.get('/health')
  return response.data
}

export const truncateDatabaseAndEvidence = async () => {
  const response = await apiClient.post('/system/truncate-all')
  return response.data
}
