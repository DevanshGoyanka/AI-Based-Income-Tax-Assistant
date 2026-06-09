import httpClient from './httpClient';

export const documentApi = {
  upload: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return httpClient.post('/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(r => r.data);
  },

  download: (id: number) =>
    httpClient.get(`/documents/${id}/download`).then(r => r.data),

  share: (id: number) =>
    httpClient.post(`/documents/${id}/share`).then(r => r.data),

  getShared: (token: string) =>
    httpClient.get(`/documents/share/${token}`).then(r => r.data),

  watermark: (id: number) =>
    httpClient.post(`/documents/${id}/watermark`).then(r => r.data),

  generateComputationSheet: (clientId: number, ay: string) =>
    httpClient.post(`/documents/computation-sheet/${clientId}/${ay}`).then(r => r.data),
};
