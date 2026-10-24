import apiClient from './client'

export const getCasesApi = async (page = 0, size = 10, status = null) => {
  const params = { page, size }
  if (status && status !== 'ALL') {
    params.status = status
  }
  const response = await apiClient.get('/cases', { params })
  return response.data
}

export const getCaseByIdApi = async (caseId) => {
  const response = await apiClient.get(`/cases/${caseId}`)
  return response.data
}

export const createCaseApi = async (caseData) => {
  const response = await apiClient.post('/cases', caseData)
  return response.data
}

export const updateCaseNotesApi = async (caseId, notesData) => {
  const response = await apiClient.patch(`/cases/${caseId}/notes`, notesData)
  return response.data
}
