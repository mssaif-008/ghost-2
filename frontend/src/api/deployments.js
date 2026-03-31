import api from './axios';

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
  const response = await api.get(`/logs/${id}`);
  return response.data;
};
