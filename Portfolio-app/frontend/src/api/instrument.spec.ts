import { describe, it, expect, vi, beforeEach } from 'vitest';
import { instrumentApi } from './instrument';
import apiClient from './client';

vi.mock('./client');

describe('instrumentApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('search', () => {
    it('검색 파라미터로 API 호출', async () => {
      const mockResponse = {
        data: {
          data: {
            content: [
              {
                id: 'inst-1',
                name: 'Apple Inc.',
                ticker: 'AAPL',
                assetClass: 'EQUITY',
              },
            ],
            totalElements: 1,
          },
        },
      };

      vi.mocked(apiClient.get).mockResolvedValue(mockResponse);

      const result = await instrumentApi.search({ q: 'Apple', size: 10 });

      expect(apiClient.get).toHaveBeenCalledWith('/instruments/search', {
        params: { q: 'Apple', size: 10 },
      });
      expect(result.data.content).toHaveLength(1);
    });

    it('자산 클래스 필터링', async () => {
      const mockResponse = {
        data: {
          data: {
            content: [],
          },
        },
      };

      vi.mocked(apiClient.get).mockResolvedValue(mockResponse);

      await instrumentApi.search({ assetClass: 'EQUITY' });

      expect(apiClient.get).toHaveBeenCalledWith('/instruments/search', {
        params: { assetClass: 'EQUITY' },
      });
    });
  });

  describe('getById', () => {
    it('ID로 종목 조회', async () => {
      const mockResponse = {
        data: {
          data: {
            id: 'inst-1',
            name: 'Apple Inc.',
          },
        },
      };

      vi.mocked(apiClient.get).mockResolvedValue(mockResponse);

      const result = await instrumentApi.getById('inst-1');

      expect(apiClient.get).toHaveBeenCalledWith('/instruments/inst-1');
      expect(result.data.id).toBe('inst-1');
    });
  });

  describe('getAll', () => {
    it('전체 종목 조회', async () => {
      const mockResponse = {
        data: {
          data: [
            { id: 'inst-1', name: 'Apple' },
            { id: 'inst-2', name: 'Google' },
          ],
        },
      };

      vi.mocked(apiClient.get).mockResolvedValue(mockResponse);

      const result = await instrumentApi.getAll();

      expect(apiClient.get).toHaveBeenCalledWith('/instruments', {
        params: { assetClass: undefined },
      });
      expect(result.data).toHaveLength(2);
    });
  });
});
