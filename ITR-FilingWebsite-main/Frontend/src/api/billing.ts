import api from './axios';

export const billingApi = {
  list: (params?: any) =>
    api.get('/billing/invoices', { params }).then(r => r.data),

  create: (data: any) =>
    api.post('/billing/invoices', data).then(r => r.data),

  recordPayment: (id: string, amount: number) =>
    api.post(`/billing/invoices/${id}/payment`, { amount }).then(r => r.data),

  sendWhatsApp: (id: string) =>
    api.post(`/billing/invoices/${id}/whatsapp`).then(r => r.data),

  download: (id: string) =>
    api.get(`/billing/invoices/${id}/pdf`, { responseType: 'blob' }).then(r => r.data),

  getStats: () =>
    api.get('/billing/stats').then(r => r.data),
};
