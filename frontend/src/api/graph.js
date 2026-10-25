import client from './client';

export const getGlobalGraph = async (limit = 150) => {
  const response = await client.get('/graph/global', {
    params: { limit }
  });
  return response.data;
};
