import api from './axios';

export const syncApi = {
  trigger: (params: { scope: string; assessmentYear: string; documents: string; batchSize: number }) =>
    api.post('/sync/trigger', params).then(r => r.data),

  getHistory: () =>
    api.get('/sync/history').then(r => r.data),

  stop: (jobId: number) =>
    api.post(`/sync/${jobId}/stop`).then(r => r.data),

  getDetails: (jobId: number) =>
    api.get(`/sync/${jobId}`).then(r => r.data),
};
