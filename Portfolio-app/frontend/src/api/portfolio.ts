import { get, post, patch, put, del } from './client';
import type {
  Portfolio,
  PortfolioWithTargets,
  PortfolioTarget,
  PortfolioType,
} from '@/types';

export interface CreatePortfolioRequest {
  name: string;
  baseCurrency: string;
  type: PortfolioType;
  groupId?: string;
  description?: string;
  tags?: string[];
}

export interface UpdatePortfolioRequest {
  name?: string;
  description?: string;
  groupId?: string;
  tags?: string[];
  archived?: boolean;
}

export const portfolioApi = {
  // List portfolios
  list: (params?: { groupId?: string; archived?: boolean }) =>
    get<Portfolio[]>('/v1/portfolios', params),

  // Get portfolio by ID
  getById: (id: string) =>
    get<PortfolioWithTargets>(`/v1/portfolios/${id}`),

  // Create portfolio
  create: (data: CreatePortfolioRequest) =>
    post<Portfolio>('/v1/portfolios', data),

  // Update portfolio
  update: (id: string, data: UpdatePortfolioRequest) =>
    patch<Portfolio>(`/v1/portfolios/${id}`, data),

  // Delete (archive) portfolio
  delete: (id: string) =>
    del<void>(`/v1/portfolios/${id}`),

  // Get targets
  getTargets: (id: string) =>
    get<PortfolioTarget[]>(`/v1/portfolios/${id}/targets`),

  // Update targets
  updateTargets: (id: string, targets: PortfolioTarget[], normalize = false) =>
    put<PortfolioTarget[]>(`/v1/portfolios/${id}/targets?normalize=${normalize}`, { targets }),

  // Clone portfolio
  clone: (id: string, newName: string) =>
    post<Portfolio>(`/v1/portfolios/${id}/clone`, { name: newName }),
};
