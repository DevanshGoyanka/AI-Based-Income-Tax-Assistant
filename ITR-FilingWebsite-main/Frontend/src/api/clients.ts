import api from './axios';
import type { Client, PageResponse } from './types';

export interface ClientFilters {
  search?: string;
  type?: string;
  filingStatus?: string;
  assessmentYear?: string;
  page?: number;
  size?: number;
}

export const clientsApi = {
  list: (filters: ClientFilters) =>
    api.get<PageResponse<Client>>('/clients', { params: filters }).then(r => r.data),

  get: (id: number) =>
    api.get<Client>(`/clients/${id}`).then(r => r.data),

  create: (data: Partial<Client>) =>
    api.post<Client>('/clients', data).then(r => r.data),

  update: (id: number, data: Partial<Client>) =>
    api.put<Client>(`/clients/${id}`, data).then(r => r.data),

  delete: (id: number) =>
    api.delete(`/clients/${id}`).then(r => r.data),

  getDashboardStats: () =>
    api.get<{
      total: number; filed: number; inProgress: number;
      docPending: number; watchList: number;
      totalMismatches: number; totalNotices: number;
    }>('/dashboard/stats').then(r => r.data),
};
