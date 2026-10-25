import client from './client';

export const getCampaigns = async (page = 0, size = 20) => {
  const response = await client.get('/campaigns', {
    params: { page, size }
  });
  return response.data;
};

export const getCampaignDetail = async (campaignId) => {
  const response = await client.get(`/campaigns/${campaignId}`);
  return response.data;
};

export const getCampaignsForEvidence = async (emailId) => {
  const response = await client.get(`/campaigns/evidence/${emailId}`);
  return response.data;
};
