import apiClient from './client'

export const getEvidenceReport = async (emailId) => {
  const response = await apiClient.get(`/evidence/${emailId}/report`)
  return response.data
}

