import apiClient from './client';

export interface Instrument {
  id: string;
  instrumentType: string;
  name: string;
  ticker: string;
  currency: string;
  country: string;
  assetClass: string;
  sector?: string;
  industry?: string;
  provider?: string;
  status: string;
}

export interface InstrumentSearchParams {
  q?: string;
  assetClass?: string;
  page?: number;
  size?: number;
}

export const instrumentApi = {
  async search(params: InstrumentSearchParams) {
    const response = await apiClient.get('/v1/instruments/search', { params });
    return response.data;
  },

  async getById(id: string) {
    const response = await apiClient.get(`/v1/instruments/${id}`);
    return response.data;
  },

  async getAll(assetClass?: string) {
    const response = await apiClient.get('/v1/instruments', {
      params: { assetClass },
    });
    return response.data;
  },
};
