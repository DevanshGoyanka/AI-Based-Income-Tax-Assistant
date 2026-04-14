import api from './axios';

export const communicationApi = {
  broadcast: (template: string, recipients: string, schedule?: string) =>
    api.post('/communication/broadcast', { template, recipients, schedule }).then(r => r.data),

  getTemplates: () =>
    api.get('/communication/templates').then(r => r.data),

  getHistory: () =>
    api.get('/communication/history').then(r => r.data),

  getChannelStatus: () =>
    api.get('/communication/channels').then(r => r.data),
};
