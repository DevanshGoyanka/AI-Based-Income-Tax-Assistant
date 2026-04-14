import api from './axios';

export const reconciliationApi = {
  getStats: () =>
    api.get('/reconciliation/stats').then(r => r.data),

  listMismatches: () =>
    api.get('/reconciliation/mismatches').then(r => r.data),

  resolve: (id: number) =>
    api.post(`/reconciliation/${id}/resolve`).then(r => r.data),

  escalate: (id: number) =>
    api.post(`/reconciliation/${id}/escalate`).then(r => r.data),

  downloadReport: (id: number) =>
    api.get(`/reconciliation/${id}/report`, { responseType: 'blob' }).then(r => r.data),
};
