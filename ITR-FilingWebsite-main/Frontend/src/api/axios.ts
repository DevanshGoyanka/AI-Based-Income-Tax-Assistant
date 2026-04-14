import axios from 'axios';
import toast from 'react-hot-toast';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('itr_erp_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (error) => {
    const status = error.response?.status;
    const message = error.response?.data?.message || 'Something went wrong';
    if (status === 401) {
      localStorage.removeItem('itr_erp_token');
      window.location.href = '/login';
    } else if (status === 429) {
      toast.error('Too many requests — please wait');
    } else if (status >= 500) {
      toast.error('Server error: ' + message);
    }
    return Promise.reject(error);
  }
);

export default api;
