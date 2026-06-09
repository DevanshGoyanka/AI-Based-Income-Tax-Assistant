import httpClient from './httpClient';

export interface FilingStatus {
  clientId: number;
  ay: string;
  status: 'DRAFT' | 'PREFILLED' | 'COMPUTED' | 'VALIDATED' | 'SUBMITTED' | 'VERIFIED';
}

export const filingApi = {
  prefill: (clientId: number, ay: string) =>
    httpClient.post(`/filing/${clientId}/${ay}/prefill`).then(r => r.data),

  compute: (clientId: number, ay: string) =>
    httpClient.post(`/filing/${clientId}/${ay}/compute`).then(r => r.data),

  validate: (clientId: number, ay: string) =>
    httpClient.post(`/filing/${clientId}/${ay}/validate`).then(r => r.data),

  submit: (clientId: number, ay: string) =>
    httpClient.post(`/filing/${clientId}/${ay}/submit`).then(r => r.data),

  verify: (clientId: number, ay: string) =>
    httpClient.post(`/filing/${clientId}/${ay}/verify`).then(r => r.data),

  getStatus: (clientId: number, ay: string) =>
    httpClient.get<FilingStatus>(`/filing/${clientId}/${ay}/status`).then(r => r.data),

  revise: (clientId: number, ay: string) =>
    httpClient.post(`/filing/${clientId}/${ay}/revise`).then(r => r.data),
};
