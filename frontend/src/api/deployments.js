import api from './axios';

const getErrorMessage = (error, fallbackMessage) =>
  error.response?.data?.message ||
  error.response?.data?.error ||
  fallbackMessage;

export const getDeployments = async () => {
  const response = await api.get('/deploy');
  return response.data;
};

export const createDeployment = async (deploymentData) => {
  const response = await api.post('/deploy', deploymentData);
  return response.data;
};

export const getDeployment = async (id) => {
  const response = await api.get(`/deploy/${id}`);
  return response.data;
};

export const deleteDeployment = async (id) => {
  const response = await api.delete(`/deploy/${id}`);
  return response.data;
};

export const getDeploymentJobs = async (id) => {
  try {
    const response = await api.get(`/logs/${id}`);
    return response.data;
  } catch (error) {
    if (error.response?.status === 404) {
      return { deploymentId: id, steps: [] };
    }
    throw new Error(getErrorMessage(error, 'Failed to fetch deployment logs'));
  }
};
