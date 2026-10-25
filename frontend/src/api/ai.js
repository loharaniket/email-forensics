import client from './client';

export const getAiAssessment = async (emailId) => {
  const response = await client.get(`/evidence/${emailId}/ai-assessment`);
  return response.data;
};

export const getAiAssessmentHistory = async (emailId) => {
  const response = await client.get(`/evidence/${emailId}/ai-assessments/history`);
  return response.data;
};

export const generateAiAssessment = async (emailId, forceRegenerate = true, modelOverride = null) => {
  const response = await client.post(`/evidence/${emailId}/ai-assessment/generate`, {
    forceRegenerate,
    modelOverride
  });
  return response.data;
};
