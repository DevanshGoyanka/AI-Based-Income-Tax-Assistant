import axiosInstance from './axiosInstance';

export const itrApi = {
  getFormData: async (clientId: number, year: string) => {
    const { data } = await axiosInstance.get(`/clients/${clientId}/itr/${year}`);
    return data;
  },
  saveFormData: async (clientId: number, year: string, formData: any) => {
    const { data } = await axiosInstance.put(`/clients/${clientId}/itr/${year}`, formData);
    return data;
  },
  computeTax: async (clientId: number, year: string, formData: any) => {
    const { data } = await axiosInstance.post(`/clients/${clientId}/itr/${year}/compute`, formData);
    return data;
  },
  validate: async (clientId: number, year: string, formData: any) => {
    const { data } = await axiosInstance.post(`/clients/${clientId}/itr/${year}/validate`, formData);
    return data as { valid: boolean; errors: string[]; warnings: string[] };
  },
  downloadJson: async (clientId: number, year: string) => {
    const res = await axiosInstance.get(`/clients/${clientId}/itr/${year}/download`, { responseType: 'blob' });
    const url = URL.createObjectURL(new Blob([res.data]));
    const a = document.createElement('a');
    a.href = url; a.download = `ITR_${clientId}_${year}.json`;
    document.body.appendChild(a); a.click(); a.remove();
    URL.revokeObjectURL(url);
  },
  downloadPdf: async (clientId: number, year: string) => {
    const res = await axiosInstance.get(`/clients/${clientId}/itr/${year}/download-pdf`, { responseType: 'blob' });
    const url = URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }));
    const a = document.createElement('a');
    a.href = url; a.download = `ITR_${clientId}_${year}.pdf`;
    document.body.appendChild(a); a.click(); a.remove();
    URL.revokeObjectURL(url);
  },
};
