import httpClient from './httpClient';

export const otpApi = {
  batchSend: (data: Record<string, unknown>) =>
    httpClient.post('/otp/batch-send', data).then(r => r.data),

  dashboard: () =>
    httpClient.get('/otp/dashboard').then(r => r.data),

  submitOTP: (token: string, otp: string) =>
    httpClient.post(`/otp/collect/${token}`, { otp }).then(r => r.data),

  manualEntry: (clientId: number, ay: string, otp: string) =>
    httpClient.post(`/otp/${clientId}/${ay}/manual`, { otp }).then(r => r.data),

  autoFile: (clientId: number, ay: string) =>
    httpClient.post(`/otp/${clientId}/${ay}/auto-file`).then(r => r.data),
};
