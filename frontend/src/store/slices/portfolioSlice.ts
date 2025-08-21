import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import type { 
  Portfolio, 
  Position, 
  PerformanceMetrics, 
  RiskAlert, 
  LoadingState,
  PaginatedResponse,
  CreatePortfolioRequest 
} from '../../types';
import { portfolioApi, positionApi } from '../../lib/api';

// Async thunks for portfolio operations
export const fetchPortfolios = createAsyncThunk(
  'portfolio/fetchPortfolios',
  async (params: { status?: string; page?: number; size?: number } = {}, { rejectWithValue }) => {
    try {
      return await portfolioApi.getPortfolios(params);
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch portfolios');
    }
  }
);

export const fetchPortfolio = createAsyncThunk(
  'portfolio/fetchPortfolio',
  async (portfolioId: string, { rejectWithValue }) => {
    try {
      return await portfolioApi.getPortfolio(portfolioId);
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch portfolio');
    }
  }
);

export const createPortfolio = createAsyncThunk(
  'portfolio/createPortfolio',
  async (request: CreatePortfolioRequest, { rejectWithValue }) => {
    try {
      return await portfolioApi.createPortfolio(request);
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to create portfolio');
    }
  }
);

export const updatePortfolio = createAsyncThunk(
  'portfolio/updatePortfolio',
  async ({ portfolioId, updates }: { portfolioId: string; updates: Partial<Portfolio> }, { rejectWithValue }) => {
    try {
      return await portfolioApi.updatePortfolio(portfolioId, updates);
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to update portfolio');
    }
  }
);

export const deletePortfolio = createAsyncThunk(
  'portfolio/deletePortfolio',
  async (portfolioId: string, { rejectWithValue }) => {
    try {
      await portfolioApi.deletePortfolio(portfolioId);
      return portfolioId;
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to delete portfolio');
    }
  }
);

export const fetchPortfolioSummary = createAsyncThunk(
  'portfolio/fetchPortfolioSummary',
  async (portfolioId: string, { rejectWithValue }) => {
    try {
      return await portfolioApi.getPortfolioSummary(portfolioId);
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch portfolio summary');
    }
  }
);

export const fetchPortfolioPerformance = createAsyncThunk(
  'portfolio/fetchPortfolioPerformance',
  async ({
    portfolioId,
    startDate,
    endDate,
    benchmark
  }: {
    portfolioId: string;
    startDate: string;
    endDate: string;
    benchmark?: string;
  }, { rejectWithValue }) => {
    try {
      return await portfolioApi.getPortfolioPerformance(portfolioId, startDate, endDate, benchmark);
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch portfolio performance');
    }
  }
);

export const fetchPositions = createAsyncThunk(
  'portfolio/fetchPositions',
  async ({
    portfolioId,
    params = {}
  }: {
    portfolioId: string;
    params?: {
      symbol?: string;
      positionType?: string;
      activeOnly?: boolean;
      page?: number;
      size?: number;
    };
  }, { rejectWithValue }) => {
    try {
      return await positionApi.getPositions(portfolioId, params);
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch positions');
    }
  }
);

export const fetchRiskAlerts = createAsyncThunk(
  'portfolio/fetchRiskAlerts',
  async ({ portfolioId, severity }: { portfolioId: string; severity?: string }, { rejectWithValue }) => {
    try {
      return await portfolioApi.getRiskAlerts(portfolioId, severity);
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch risk alerts');
    }
  }
);

// Portfolio state interface
interface PortfolioState extends LoadingState {
  portfolios: PaginatedResponse<Portfolio> | null;
  currentPortfolio: Portfolio | null;
  positions: PaginatedResponse<Position> | null;
  portfolioSummary: any | null;
  performance: PerformanceMetrics | null;
  riskAlerts: RiskAlert[];
  selectedPortfolioId: string | null;
}

// Initial state
const initialState: PortfolioState = {
  portfolios: null,
  currentPortfolio: null,
  positions: null,
  portfolioSummary: null,
  performance: null,
  riskAlerts: [],
  selectedPortfolioId: null,
  isLoading: false,
  error: undefined,
};

// Portfolio slice
const portfolioSlice = createSlice({
  name: 'portfolio',
  initialState,
  reducers: {
    // Clear errors
    clearError: (state) => {
      state.error = undefined;
    },
    
    // Set selected portfolio ID
    setSelectedPortfolio: (state, action: PayloadAction<string | null>) => {
      state.selectedPortfolioId = action.payload;
    },
    
    // Clear portfolio data (for cleanup)
    clearPortfolioData: (state) => {
      state.currentPortfolio = null;
      state.positions = null;
      state.portfolioSummary = null;
      state.performance = null;
      state.riskAlerts = [];
    },
    
    // Update position in real-time (for WebSocket updates)
    updatePosition: (state, action: PayloadAction<Position>) => {
      if (state.positions && state.positions.content) {
        const index = state.positions.content.findIndex(pos => pos.id === action.payload.id);
        if (index !== -1) {
          state.positions.content[index] = action.payload;
        }
      }
      
      // Also update positions in current portfolio if loaded
      if (state.currentPortfolio && state.currentPortfolio.positions) {
        const index = state.currentPortfolio.positions.findIndex(pos => pos.id === action.payload.id);
        if (index !== -1) {
          state.currentPortfolio.positions[index] = action.payload;
        }
      }
    },
    
    // Add new risk alert
    addRiskAlert: (state, action: PayloadAction<RiskAlert>) => {
      state.riskAlerts.unshift(action.payload);
    },
    
    // Remove risk alert
    removeRiskAlert: (state, action: PayloadAction<string>) => {
      state.riskAlerts = state.riskAlerts.filter(alert => alert.id !== action.payload);
    },
  },
  extraReducers: (builder) => {
    // Fetch portfolios
    builder
      .addCase(fetchPortfolios.pending, (state) => {
        state.isLoading = true;
        state.error = undefined;
      })
      .addCase(fetchPortfolios.fulfilled, (state, action) => {
        state.isLoading = false;
        state.portfolios = action.payload;
        state.error = undefined;
      })
      .addCase(fetchPortfolios.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      });

    // Fetch portfolio
    builder
      .addCase(fetchPortfolio.pending, (state) => {
        state.isLoading = true;
        state.error = undefined;
      })
      .addCase(fetchPortfolio.fulfilled, (state, action) => {
        state.isLoading = false;
        state.currentPortfolio = action.payload;
        state.selectedPortfolioId = action.payload.id;
        state.error = undefined;
      })
      .addCase(fetchPortfolio.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      });

    // Create portfolio
    builder
      .addCase(createPortfolio.pending, (state) => {
        state.isLoading = true;
        state.error = undefined;
      })
      .addCase(createPortfolio.fulfilled, (state, action) => {
        state.isLoading = false;
        // Add new portfolio to the list if portfolios are loaded
        if (state.portfolios) {
          state.portfolios.content.unshift(action.payload);
          state.portfolios.totalElements += 1;
        }
        state.error = undefined;
      })
      .addCase(createPortfolio.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      });

    // Update portfolio
    builder
      .addCase(updatePortfolio.pending, (state) => {
        state.isLoading = true;
        state.error = undefined;
      })
      .addCase(updatePortfolio.fulfilled, (state, action) => {
        state.isLoading = false;
        state.currentPortfolio = action.payload;
        
        // Update portfolio in the list if portfolios are loaded
        if (state.portfolios) {
          const index = state.portfolios.content.findIndex(p => p.id === action.payload.id);
          if (index !== -1) {
            state.portfolios.content[index] = action.payload;
          }
        }
        state.error = undefined;
      })
      .addCase(updatePortfolio.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      });

    // Delete portfolio
    builder
      .addCase(deletePortfolio.pending, (state) => {
        state.isLoading = true;
        state.error = undefined;
      })
      .addCase(deletePortfolio.fulfilled, (state, action) => {
        state.isLoading = false;
        
        // Remove portfolio from the list
        if (state.portfolios) {
          state.portfolios.content = state.portfolios.content.filter(p => p.id !== action.payload);
          state.portfolios.totalElements -= 1;
        }
        
        // Clear current portfolio if it was deleted
        if (state.currentPortfolio?.id === action.payload) {
          state.currentPortfolio = null;
          state.selectedPortfolioId = null;
        }
        state.error = undefined;
      })
      .addCase(deletePortfolio.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      });

    // Fetch portfolio summary
    builder
      .addCase(fetchPortfolioSummary.fulfilled, (state, action) => {
        state.portfolioSummary = action.payload;
      });

    // Fetch portfolio performance
    builder
      .addCase(fetchPortfolioPerformance.fulfilled, (state, action) => {
        state.performance = action.payload;
      });

    // Fetch positions
    builder
      .addCase(fetchPositions.fulfilled, (state, action) => {
        state.positions = action.payload;
      });

    // Fetch risk alerts
    builder
      .addCase(fetchRiskAlerts.fulfilled, (state, action) => {
        state.riskAlerts = action.payload;
      });
  },
});

// Export actions
export const { 
  clearError, 
  setSelectedPortfolio, 
  clearPortfolioData,
  updatePosition,
  addRiskAlert,
  removeRiskAlert
} = portfolioSlice.actions;

// Export selectors
export const selectPortfolios = (state: { portfolio: PortfolioState }) => state.portfolio.portfolios;
export const selectCurrentPortfolio = (state: { portfolio: PortfolioState }) => state.portfolio.currentPortfolio;
export const selectPositions = (state: { portfolio: PortfolioState }) => state.portfolio.positions;
export const selectPortfolioSummary = (state: { portfolio: PortfolioState }) => state.portfolio.portfolioSummary;
export const selectPortfolioPerformance = (state: { portfolio: PortfolioState }) => state.portfolio.performance;
export const selectRiskAlerts = (state: { portfolio: PortfolioState }) => state.portfolio.riskAlerts;
export const selectSelectedPortfolioId = (state: { portfolio: PortfolioState }) => state.portfolio.selectedPortfolioId;
export const selectPortfolioLoading = (state: { portfolio: PortfolioState }) => state.portfolio.isLoading;
export const selectPortfolioError = (state: { portfolio: PortfolioState }) => state.portfolio.error;

// Export reducer
export default portfolioSlice.reducer;