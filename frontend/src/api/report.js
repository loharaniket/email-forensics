import apiClient from './client'

export const getEvidenceReport = async (emailId) => {
  const response = await apiClient.get(`/evidence/${emailId}/report`)
  return response.data
}

export const downloadEvidenceReportJson = async (emailId) => {
  const response = await apiClient.get(`/evidence/${emailId}/report/json`, {
    responseType: 'blob',
  })
  const blob = new Blob([response.data], { type: 'application/json' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', `forensic-report-${emailId}.json`)
  document.body.appendChild(link)
  link.click()
  link.parentNode.removeChild(link)
  window.URL.revokeObjectURL(url)
}

export const getCaseReport = async (caseId) => {
  const response = await apiClient.get(`/cases/${caseId}/report`)
  return response.data
}
