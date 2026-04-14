import api from './axios';
import type { BatchJob, PageResponse } from './types';

export interface JobFilters {
  status?: string;
  assessmentYear?: string;
  page?: number;
  size?: number;
}

export const jobsApi = {
  list: (filters: JobFilters) =>
    api.get<PageResponse<BatchJob>>('/jobs', { params: filters }).then(r => r.data),

  get: (jobId: number) =>
    api.get<BatchJob>(`/jobs/${jobId}`).then(r => r.data),

  getStats: () =>
    api.get('/jobs/stats').then(r => r.data),

  stop: (jobId: number) =>
    api.post(`/jobs/${jobId}/stop`).then(r => r.data),

  restart: (jobId: number) =>
    api.post(`/jobs/${jobId}/restart`).then(r => r.data),

  getLogs: (jobId: number) =>
    api.get<string[]>(`/jobs/${jobId}/logs`).then(r => r.data),
};
