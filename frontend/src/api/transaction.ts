import { get, post } from './client';
import type { Transaction, TransactionLeg, TransactionType } from '@/types';

export interface CreateTransactionRequest {
  occurredAt: string;
  type: TransactionType;
  settleDate?: string;
  note?: string;
  tags?: string[];
  legs: Omit<TransactionLeg, 'id'>[];
}

export interface TransactionListParams {
  from?: string;
  to?: string;
  type?: TransactionType;
  limit?: number;
  cursor?: string;
}

export const transactionApi = {
  // List transactions for portfolio
  list: (portfolioId: string, params?: TransactionListParams) =>
    get<Transaction[]>(`/v1/portfolios/${portfolioId}/transactions`, params),

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
