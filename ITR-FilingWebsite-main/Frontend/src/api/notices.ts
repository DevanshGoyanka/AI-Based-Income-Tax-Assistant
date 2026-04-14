import api from './axios';

export const noticesApi = {
  list: (params?: any) =>
    api.get('/notices', { params }).then(r => r.data),

  get: (id: number) =>
    api.get(`/notices/${id}`).then(r => r.data),

  update: (id: number, data: any) =>
    api.put(`/notices/${id}`, data).then(r => r.data),

  addNote: (id: number, note: string) =>
    api.post(`/notices/${id}/notes`, { note }).then(r => r.data),

  close: (id: number) =>
    api.post(`/notices/${id}/close`).then(r => r.data),

  getDeadlines: (month: string) =>
    api.get('/notices/deadlines', { params: { month } }).then(r => r.data),
};
