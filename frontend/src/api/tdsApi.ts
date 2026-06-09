import httpClient from './httpClient';

export const tdsApi = {
  listReturns: () =>
    httpClient.get('/tds/returns').then(r => r.data),

  createReturn: (data: Record<string, unknown>) =>
    httpClient.post('/tds/returns', data).then(r => r.data),

  getForm16: (id: number) =>
    httpClient.get(`/tds/returns/${id}/form16`).then(r => r.data),

  generateFVU: (id: number) =>
    httpClient.post(`/tds/returns/${id}/fvu`).then(r => r.data),

  reconcile: (clientId: number, ay: string) =>
    httpClient.post(`/tds/reconcile/${clientId}/${ay}`).then(r => r.data),

  recordChallan: (data: Record<string, unknown>) =>
    httpClient.post('/tds/challans', data).then(r => r.data),
};
