import axiosInstance from './axiosInstance';

const multipartPost = async (endpoint: string, file: File, params?: Record<string, string>) => {
  const fd = new FormData();
  fd.append('file', file);
  const { data } = await axiosInstance.post(endpoint, fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
    params,
  });
  return data;
};

export const integrationApi = {
  extractForm16: (file: File) => multipartPost('/integration/form16/extract', file),
  importAIS: (file: File, pan?: string, dob?: string) => 
    multipartPost('/integration/ais/import', file, pan && dob ? { pan, dob } : undefined),
  importTIS: (file: File, pan?: string, dob?: string) => 
    multipartPost('/integration/tis/import', file, pan && dob ? { pan, dob } : undefined),
  import26AS: (file: File, pan?: string, dob?: string) => 
    multipartPost('/integration/26as/import', file, pan && dob ? { pan, dob } : undefined),
  importITDPrefill: (file: File) => multipartPost('/integration/prefill/import', file),
  autoPopulateFromForm16: async (itrData: any, form16Data: any) => {
    const { data } = await axiosInstance.post('/integration/autopopulate/form16', itrData, {
      params: { form16: form16Data }
    });
    return data;
  },
  autoPopulateFromAIS: async (itrData: any, aisData: any) => {
    const { data } = await axiosInstance.post('/integration/autopopulate/ais', itrData, {
      params: { ais: aisData }
    });
    return data;
  },
  autoPopulateFromPrefill: async (itrData: any, prefillData: any) => {
    // Backend expects form data as body and prefill as query param
    const { data } = await axiosInstance.post('/integration/autopopulate/prefill', itrData, {
      params: { prefill: JSON.stringify(prefillData) }
    });
    return data;
  },
};
