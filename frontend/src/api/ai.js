import client from './client';

export const getAiAssessment = async (emailId) => {
  const response = await client.get(`/evidence/${emailId}/ai-assessment`);
  return response.data;
};

export const generateAiAssessment = async (emailId, forceRegenerate = true) => {
  const response = await client.post(`/evidence/${emailId}/ai-assessment/generate`, {
    forceRegenerate
  });
  return response.data;
};
