import axios, { AxiosResponse, AxiosError } from 'axios';
import { getErrorHandler } from '../utils/errorHandler';
import type { 
  AuthTokens, 
  LoginRequest, 
  User, 
  Portfolio, 
  Position, 
  Order, 
  MarketData, 
  ApiResponse, 
  PaginatedResponse,
  CreatePortfolioRequest,
  PlaceOrderRequest,
  RiskAlert,
  PerformanceMetrics
} from '../types';

// API Configuration
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

// Create axios instance with default configuration
export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
});

// Request interceptor to add auth token
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle token refresh and global error handling
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as any;
    const errorHandler = getErrorHandler();
    
    // Handle 401 errors with token refresh
    if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        const refreshToken = localStorage.getItem('refreshToken');
        if (refreshToken) {
          const response = await axios.post(`${API_BASE_URL}/api/v1/auth/refresh`, {
            refreshToken
          });
          
          const { accessToken } = response.data.data;
          localStorage.setItem('accessToken', accessToken);
          
          // Retry original request with new token
          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          return apiClient(originalRequest);
        }
      } catch (refreshError) {
        // Refresh failed, clear tokens and handle session expiry
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        
        if (errorHandler) {
          errorHandler.handleError(error, 'Token Refresh');
        }
        
        // Redirect to auth page after a short delay
        setTimeout(() => {
          window.location.href = '/auth';
        }, 2000);
      }
    }
    
    // Global error handling for non-401 errors
    if (errorHandler && error.response?.status !== 401) {
      const context = originalRequest?.url ? `API: ${originalRequest.url}` : 'API Request';
      errorHandler.handleError(error, context);
    }
    
    return Promise.reject(error);
  }
);

// Generic API response handler
const handleApiResponse = <T>(response: AxiosResponse<ApiResponse<T>>): T => {
  if (!response.data.success) {
    throw new Error(response.data.message || 'API request failed');
  }
  return response.data.data!;
};

// Authentication API
export const authApi = {
  login: async (credentials: LoginRequest): Promise<{ user: User; tokens: AuthTokens }> => {
    const response = await apiClient.post<ApiResponse<{ user: User; tokens: AuthTokens }>>(
      '/api/v1/auth/login',
      credentials
    );
    return handleApiResponse(response);
  },

  logout: async (): Promise<void> => {
    await apiClient.post('/api/v1/auth/logout');
  },

  refreshToken: async (refreshToken: string): Promise<AuthTokens> => {
    const response = await apiClient.post<ApiResponse<AuthTokens>>(
      '/api/v1/auth/refresh',
      { refreshToken }
    );
    return handleApiResponse(response);
  },

  getCurrentUser: async (): Promise<User> => {
    const response = await apiClient.get<ApiResponse<User>>('/api/v1/auth/me');
    return handleApiResponse(response);
  },

  register: async (userData: {
    username: string;
    email: string;
    password: string;
    firstName: string;
    lastName: string;
  }): Promise<User> => {
    const response = await apiClient.post<ApiResponse<User>>(
      '/api/v1/auth/register',
      userData
    );
    return handleApiResponse(response);
  },
};

// Portfolio API
export const portfolioApi = {
  getPortfolios: async (params?: {
    status?: string;
    page?: number;
    size?: number;
  }): Promise<PaginatedResponse<Portfolio>> => {
    const response = await apiClient.get<ApiResponse<PaginatedResponse<Portfolio>>>(
      '/api/v1/portfolios',
      { params }
    );
    return handleApiResponse(response);
  },

  getPortfolio: async (portfolioId: string): Promise<Portfolio> => {
    const response = await apiClient.get<ApiResponse<Portfolio>>(
      `/api/v1/portfolios/${portfolioId}`
    );
    return handleApiResponse(response);
  },

  createPortfolio: async (request: CreatePortfolioRequest): Promise<Portfolio> => {
    const response = await apiClient.post<ApiResponse<Portfolio>>(
      '/api/v1/portfolios',
      request
    );
    return handleApiResponse(response);
  },

  updatePortfolio: async (
    portfolioId: string,
    updates: Partial<Portfolio>
  ): Promise<Portfolio> => {
    const response = await apiClient.put<ApiResponse<Portfolio>>(
      `/api/v1/portfolios/${portfolioId}`,
      updates
    );
    return handleApiResponse(response);
  },

  deletePortfolio: async (portfolioId: string): Promise<void> => {
    await apiClient.delete(`/api/v1/portfolios/${portfolioId}`);
  },

  getPortfolioSummary: async (portfolioId: string): Promise<any> => {
    const response = await apiClient.get<ApiResponse<any>>(
      `/api/v1/portfolios/${portfolioId}/summary`
    );
    return handleApiResponse(response);
  },

  getPortfolioPerformance: async (
    portfolioId: string,
    startDate: string,
    endDate: string,
    benchmark?: string
  ): Promise<PerformanceMetrics> => {
    const response = await apiClient.get<ApiResponse<PerformanceMetrics>>(
      `/api/v1/portfolios/${portfolioId}/performance`,
      {
        params: { startDate, endDate, benchmark }
      }
    );
    return handleApiResponse(response);
  },

  getRiskAlerts: async (
    portfolioId: string,
    severity?: string
  ): Promise<RiskAlert[]> => {
    const response = await apiClient.get<ApiResponse<RiskAlert[]>>(
      `/api/v1/portfolios/${portfolioId}/risk/alerts`,
      { params: { severity } }
    );
    return handleApiResponse(response);
  },
};

// Position API
export const positionApi = {
  getPositions: async (
    portfolioId: string,
    params?: {
      symbol?: string;
      positionType?: string;
      activeOnly?: boolean;
      page?: number;
      size?: number;
    }
  ): Promise<PaginatedResponse<Position>> => {
    const response = await apiClient.get<ApiResponse<PaginatedResponse<Position>>>(
      `/api/v1/portfolios/${portfolioId}/positions`,
      { params }
    );
    return handleApiResponse(response);
  },

  getPosition: async (portfolioId: string, positionId: string): Promise<Position> => {
    const response = await apiClient.get<ApiResponse<Position>>(
      `/api/v1/portfolios/${portfolioId}/positions/${positionId}`
    );
    return handleApiResponse(response);
  },

  getPositionBySymbol: async (portfolioId: string, symbol: string): Promise<Position> => {
    const response = await apiClient.get<ApiResponse<Position>>(
      `/api/v1/portfolios/${portfolioId}/positions/symbol/${symbol}`
    );
    return handleApiResponse(response);
  },

  closePosition: async (
    portfolioId: string,
    positionId: string,
    strategy?: string
  ): Promise<any> => {
    const response = await apiClient.post<ApiResponse<any>>(
      `/api/v1/portfolios/${portfolioId}/positions/${positionId}/close`,
      { strategy }
    );
    return handleApiResponse(response);
  },
};

// Trading API
export const tradingApi = {
  getOrders: async (params?: {
    portfolioId?: string;
    symbol?: string;
    status?: string;
    page?: number;
    size?: number;
  }): Promise<PaginatedResponse<Order>> => {
    const response = await apiClient.get<ApiResponse<PaginatedResponse<Order>>>(
      '/api/v1/trading/orders',
      { params }
    );
    return handleApiResponse(response);
  },

  getOrder: async (orderId: string): Promise<Order> => {
    const response = await apiClient.get<ApiResponse<Order>>(
      `/api/v1/trading/orders/${orderId}`
    );
    return handleApiResponse(response);
  },

  placeOrder: async (orderRequest: PlaceOrderRequest): Promise<Order> => {
    const response = await apiClient.post<ApiResponse<Order>>(
      '/api/v1/trading/orders',
      orderRequest
    );
    return handleApiResponse(response);
  },

  cancelOrder: async (orderId: string): Promise<Order> => {
    const response = await apiClient.put<ApiResponse<Order>>(
      `/api/v1/trading/orders/${orderId}/cancel`
    );
    return handleApiResponse(response);
  },

  modifyOrder: async (
    orderId: string,
    modifications: Partial<PlaceOrderRequest>
  ): Promise<Order> => {
    const response = await apiClient.put<ApiResponse<Order>>(
      `/api/v1/trading/orders/${orderId}`,
      modifications
    );
    return handleApiResponse(response);
  },
};

// Market Data API
export const marketDataApi = {
  getQuote: async (symbol: string): Promise<MarketData> => {
    const response = await apiClient.get<ApiResponse<MarketData>>(
      `/api/v1/market-data/quote/${symbol}`
    );
    return handleApiResponse(response);
  },

  getMultipleQuotes: async (symbols: string[]): Promise<MarketData[]> => {
    const response = await apiClient.post<ApiResponse<MarketData[]>>(
      '/api/v1/market-data/quotes',
      { symbols }
    );
    return handleApiResponse(response);
  },

  getHistoricalData: async (
    symbol: string,
    startDate: string,
    endDate: string,
    interval?: string
  ): Promise<any[]> => {
    const response = await apiClient.get<ApiResponse<any[]>>(
      `/api/v1/market-data/historical/${symbol}`,
      {
        params: { startDate, endDate, interval }
      }
    );
    return handleApiResponse(response);
  },

  searchSymbols: async (query: string): Promise<any[]> => {
    const response = await apiClient.get<ApiResponse<any[]>>(
      '/api/v1/market-data/search',
      { params: { q: query } }
    );
    return handleApiResponse(response);
  },
};

// Utility functions
export const formatApiError = (error: AxiosError): string => {
  if (error.response?.data) {
    const apiError = error.response.data as ApiResponse;
    return apiError.message || 'An error occurred';
  }
  return error.message || 'Network error occurred';
};

export const isApiError = (error: any): error is AxiosError => {
  return error?.isAxiosError === true;
};

export default {
  auth: authApi,
  portfolio: portfolioApi,
  position: positionApi,
  trading: tradingApi,
  marketData: marketDataApi,
};