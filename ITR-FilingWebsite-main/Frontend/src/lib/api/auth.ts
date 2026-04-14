import axiosInstance from './axiosInstance';

export const authApi = {
  login: async (email: string, password: string) => {
    const { data } = await axiosInstance.post('/auth/login', { email, password });
    return data as { token: string; email: string; expiresIn: number };
  },
  register: async (email: string, password: string) => {
    const { data } = await axiosInstance.post('/auth/register', { email, password });
    return data as { token: string; email: string; expiresIn: number };
  },
};
