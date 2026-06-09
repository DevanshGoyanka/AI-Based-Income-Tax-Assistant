import httpClient from './httpClient';

export const noticeApi = {
  list: (page = 0, size = 20) =>
    httpClient.get('/notices', { params: { page, size } }).then(r => r.data),

  fetchAll: () =>
    httpClient.post('/notices/fetch-all').then(r => r.data),

  get: (id: number) =>
    httpClient.get(`/notices/${id}`).then(r => r.data),

  analyze: (id: number) =>
    httpClient.post(`/notices/${id}/analyze`).then(r => r.data),

  draftReply: (id: number) =>
    httpClient.post(`/notices/${id}/draft-reply`).then(r => r.data),

  updateReply: (id: number, version: number, data: Record<string, unknown>) =>
    httpClient.put(`/notices/${id}/reply/${version}`, data).then(r => r.data),

  uploadReply: (id: number) =>
    httpClient.post(`/notices/${id}/upload-reply`).then(r => r.data),

  analytics: () =>
    httpClient.get('/notices/analytics').then(r => r.data),
};
