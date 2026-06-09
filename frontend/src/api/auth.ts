import axiosInstance from './axiosInstance';

export interface AuthResponse {
  token: string;
  refreshToken: string;
  email: string;
  expiresIn: number;
}

export const authApi = {
  login: async (email: string, password: string) => {
    const { data } = await axiosInstance.post('/auth/login', { email, password });
    // Backend returns accessToken, we map it to token for frontend compatibility
    return {
      token: data.accessToken,
      refreshToken: data.refreshToken,
      email: data.email,
      expiresIn: data.expiresIn
    } as AuthResponse;
  },
  register: async (email: string, password: string) => {
    const { data } = await axiosInstance.post('/auth/register', { email, password });
    return {
      token: data.accessToken,
      refreshToken: data.refreshToken,
      email: data.email,
      expiresIn: data.expiresIn
    } as AuthResponse;
  },
};
