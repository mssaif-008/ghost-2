import axios from 'axios';

// Support both Create React App and Vite ways of handling env variables
const getApiUrl = () => {
  if (typeof process !== 'undefined' && process.env && process.env.REACT_APP_API_URL) {
    return process.env.REACT_APP_API_URL;
  }
  if (typeof import.meta !== 'undefined' && import.meta.env && import.meta.env.VITE_API_URL) {
    return import.meta.env.VITE_API_URL;
  }
  return 'http://localhost:8081';
};

const api = axios.create({
  baseURL: getApiUrl(),
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('ghost_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('ghost_token');
      localStorage.removeItem('ghost_email');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export default api;
