import client from './client';

export const uploadEvidence = async (file) => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await client.post('/evidence/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;
};

export const getEvidenceList = async (page = 0, size = 20) => {
  const response = await client.get('/evidence', {
    params: { page, size },
  });
  return response.data;
};

export const getCaseEvidence = getEvidenceList;

export const getEvidenceStatus = async (emailId) => {
  const response = await client.get(`/evidence/${emailId}/status`);
  return response.data;
};

export const checkEvidenceIntegrity = async (emailId) => {
  const response = await client.get(`/evidence/${emailId}/integrity`);
  return response.data;
};

export const downloadOriginalEvidence = async (emailId, originalFilename) => {
  const response = await client.get(`/evidence/${emailId}/original`, {
    responseType: 'blob',
  });
  const url = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', originalFilename || 'evidence.eml');
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};

export const getInvestigationDetail = async (emailId) => {
  const response = await client.get(`/evidence/${emailId}/investigation`);
  return response.data;
};

export const getEvidenceHeaders = async (emailId) => {
  const response = await client.get(`/evidence/${emailId}/headers`);
  return response.data;
};

export const getEvidenceHops = async (emailId) => {
  const response = await client.get(`/evidence/${emailId}/hops`);
  return response.data;
};

export const getEvidenceUrls = async (emailId) => {
  const response = await client.get(`/evidence/${emailId}/urls`);
  return response.data;
};

export const getEvidenceInfrastructure = async (emailId) => {
  const response = await client.get(`/evidence/${emailId}/infrastructure`);
  return response.data;
};

export const getEvidenceVisuals = async (emailId) => {
  const response = await client.get(`/evidence/${emailId}/images`);
  return response.data;
};

export const getEvidenceImages = async (emailId) => {
  const response = await client.get(`/evidence/${emailId}/images`);
  return response.data;
};

export const getEvidenceQrCodes = async (emailId) => {
  const response = await client.get(`/evidence/${emailId}/qr`);
  return response.data;
};

export const getEvidenceImageUrl = (emailId, imageId) => {
  return `/api/v1/evidence/${emailId}/images/${imageId}/content`;
};

export const getEvidenceContentAnalysis = async (emailId) => {
  const response = await client.get(`/evidence/${emailId}/content-analysis`);
  return response.data;
};

export const getEvidenceRiskBreakdown = async (emailId) => {
  const response = await client.get(`/evidence/${emailId}/risk-breakdown`);
  return response.data;
};

export const getEvidenceCorrelations = async (emailId) => {
  const response = await client.get(`/evidence/${emailId}/correlations`);
  return response.data;
};

export const getEvidenceAiAssessment = async (emailId) => {
  const response = await client.get(`/evidence/${emailId}/ai-assessment`);
  return response.data;
};

export const generateEvidenceAiAssessment = async (emailId, forceRegenerate = true) => {
  const response = await client.post(`/evidence/${emailId}/ai-assessment/generate`, {
    forceRegenerate
  });
  return response.data;
};

export const retryEvidenceAnalysis = async (emailId) => {
  const response = await client.post(`/evidence/${emailId}/retry`);
  return response.data;
};
