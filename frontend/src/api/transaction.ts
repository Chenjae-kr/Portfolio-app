import { get, post } from './client';
import type { Transaction, TransactionLeg, TransactionType } from '@/types';

export interface CreateTransactionRequest {
  occurredAt: string;
  type: TransactionType;
  note?: string;
  legs: Omit<TransactionLeg, 'id'>[];
}

export interface TransactionListParams {
  page?: number;
  size?: number;
  type?: TransactionType;
  from?: string;
  to?: string;
}

export interface TransactionListResponse {
  items: Transaction[];
  totalElements: number;
  totalPages: number;
}

export const transactionApi = {
  // List transactions for portfolio
  list: (portfolioId: string, params?: TransactionListParams) =>
    get<Transaction[]>(`/v1/portfolios/${portfolioId}/transactions`, params as Record<string, unknown>),

  // Get transaction by ID
  getById: (id: string) =>
    get<Transaction>(`/v1/transactions/${id}`),

  // Create transaction
  create: (portfolioId: string, data: CreateTransactionRequest) =>
    post<Transaction>(`/v1/portfolios/${portfolioId}/transactions`, data),

  // Void transaction
  void: (id: string) =>
    post<Transaction>(`/v1/transactions/${id}/void`),
};
