import apiClient from './client';

export interface PortfolioGroup {
  id: string;
  name: string;
  sortOrder: number;
  createdAt: string;
}

export interface CreateGroupRequest {
  name: string;
  sortOrder?: number;
}

export interface UpdateGroupRequest {
  name?: string;
  sortOrder?: number;
}

export const portfolioGroupApi = {
  async list() {
    const response = await apiClient.get('/portfolio-groups');
    return response.data;
  },

  async create(request: CreateGroupRequest) {
    const response = await apiClient.post('/portfolio-groups', request);
    return response.data;
  },

  async update(id: string, request: UpdateGroupRequest) {
    const response = await apiClient.patch(`/portfolio-groups/${id}`, request);
    return response.data;
  },

  async delete(id: string) {
    const response = await apiClient.delete(`/portfolio-groups/${id}`);
    return response.data;
  },
};
