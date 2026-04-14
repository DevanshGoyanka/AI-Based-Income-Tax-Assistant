import api from './axios';

export interface FilingFilters {
  assessmentYear?: string;
  stage?: string;
  itrForm?: string;
  page?: number;
  size?: number;
}

export const filingApi = {
  list: (filters: FilingFilters) =>
    api.get('/filing', { params: filters }).then(r => r.data),

  getITRData: (clientId: number, assessmentYear: string) =>
    api.get(`/clients/${clientId}/itr/${assessmentYear}`).then(r => r.data),

  saveDraft: (clientId: number, data: any) =>
    api.put(`/clients/${clientId}/itr/${data.assessmentYear || '2025-26'}`, data).then(r => r.data),

  submitITR: (data: any) =>
    api.post(`/clients/${data.clientId}/itr/${data.assessmentYear}/submit`, data).then(r => r.data),

  validate: (data: any) =>
    api.post(`/clients/${data.clientId}/itr/${data.assessmentYear}/validate`, data).then(r => r.data),

  compute: (data: any) =>
    api.post(`/clients/${data.clientId}/itr/${data.assessmentYear}/compute`, data).then(r => r.data),

  getRegimeRecommendation: (data: any) =>
    api.post(`/clients/${data.clientId}/itr/${data.assessmentYear}/regime-recommendation`, data).then(r => r.data),

  reconcileAIS: (clientId: number, assessmentYear: string) =>
    api.post(`/clients/${clientId}/itr/${assessmentYear}/reconcile`).then(r => r.data),

  exportJSON: (clientId: number, assessmentYear: string) =>
    api.get(`/clients/${clientId}/itr/${assessmentYear}/download`, { responseType: 'blob' }).then(r => r.data),

  downloadITRV: (clientId: number, assessmentYear: string) =>
    api.get(`/clients/${clientId}/itr/${assessmentYear}/itrv`, { responseType: 'blob' }).then(r => r.data),

  initiateEVC: (pan: string, mode: string) =>
    api.post('/evc/initiate', { pan, mode }).then(r => r.data),

  confirmEVC: (pan: string, transactionId: string, otp: string, ackNo: string) =>
    api.post('/evc/confirm', { pan, transactionId, otp, acknowledgementNumber: ackNo }).then(r => r.data),

  uploadForm16: (clientId: number, partA: File, partB: File) => {
    const fd = new FormData();
    fd.append('partA', partA);
    fd.append('partB', partB);
    return api.post(`/import/form16/${clientId}`, fd, { 
      headers: { 'Content-Type': 'multipart/form-data' } 
    }).then(r => r.data);
  },
};
