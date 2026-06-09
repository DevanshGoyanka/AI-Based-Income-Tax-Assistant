import httpClient from './httpClient';

export const clientApi = {
  list: (page = 0, size = 20, search?: string) =>
    httpClient.get('/clients', { params: { page, size, search } }).then(r => r.data),

  create: (data: Record<string, unknown>) =>
    httpClient.post('/clients', data).then(r => r.data),

  get: (id: number) =>
    httpClient.get(`/clients/${id}`).then(r => r.data),

  update: (id: number, data: Record<string, unknown>) =>
    httpClient.put(`/clients/${id}`, data).then(r => r.data),

  delete: (id: number) =>
    httpClient.delete(`/clients/${id}`).then(r => r.data),

  verifyPAN: (id: number) =>
    httpClient.post(`/clients/${id}/verify-pan`).then(r => r.data),

  bulkImport: (data: Record<string, unknown>) =>
    httpClient.post('/clients/import', data).then(r => r.data),
};
