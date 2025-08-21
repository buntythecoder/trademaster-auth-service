import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import type { MarketData, LoadingState, PriceUpdate } from '../../types';
import { marketDataApi } from '../../lib/api';

// Async thunks for market data operations
export const fetchQuote = createAsyncThunk(
  'marketData/fetchQuote',
  async (symbol: string, { rejectWithValue }) => {
    try {
      return await marketDataApi.getQuote(symbol);
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch quote');
    }
  }
);

export const fetchMultipleQuotes = createAsyncThunk(
  'marketData/fetchMultipleQuotes',
  async (symbols: string[], { rejectWithValue }) => {
    try {
      return await marketDataApi.getMultipleQuotes(symbols);
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch quotes');
    }
  }
);

export const fetchHistoricalData = createAsyncThunk(
  'marketData/fetchHistoricalData',
  async ({
    symbol,
    startDate,
    endDate,
    interval
  }: {
    symbol: string;
    startDate: string;
    endDate: string;
    interval?: string;
  }, { rejectWithValue }) => {
    try {
      const data = await marketDataApi.getHistoricalData(symbol, startDate, endDate, interval);
      return { symbol, data };
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch historical data');
    }
  }
);

export const searchSymbols = createAsyncThunk(
  'marketData/searchSymbols',
  async (query: string, { rejectWithValue }) => {
    try {
      return await marketDataApi.searchSymbols(query);
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to search symbols');
    }
  }
);

export const fetchMarketOverview = createAsyncThunk(
  'marketData/fetchMarketOverview',
  async (symbols: string[], { rejectWithValue }) => {
    try {
      return await marketDataApi.getMultipleQuotes(symbols);
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch market overview');
    }
  }
);

export const fetchMarketSummary = createAsyncThunk(
  'marketData/fetchMarketSummary',
  async (_, { rejectWithValue }) => {
    try {
      // Mock market summary data - would be replaced with real API call
      return {
        indices: ['SPY', 'QQQ', 'DIA'],
        marketStatus: 'OPEN',
        timestamp: new Date().toISOString(),
      };
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch market summary');
    }
  }
);

// Market data state interface
interface MarketDataState extends LoadingState {
  quotes: Record<string, MarketData>;
  historicalData: Record<string, any[]>;
  searchResults: any[];
  watchlist: string[];
  priceAlerts: Record<string, PriceAlert[]>;
  isConnected: boolean;
  lastUpdateTime: string | null;
  overview: any;
  summary: any;
}

interface PriceAlert {
  id: string;
  symbol: string;
  type: 'above' | 'below';
  price: number;
  isActive: boolean;
  createdAt: string;
}

// Initial state
const initialState: MarketDataState = {
  quotes: {},
  historicalData: {},
  searchResults: [],
  watchlist: [],
  priceAlerts: {},
  isConnected: false,
  lastUpdateTime: null,
  overview: null,
  summary: null,
  isLoading: false,
  error: undefined,
};

// Market data slice
const marketDataSlice = createSlice({
  name: 'marketData',
  initialState,
  reducers: {
    // Clear errors
    clearError: (state) => {
      state.error = undefined;
    },
    
    // WebSocket connection status
    setConnectionStatus: (state, action: PayloadAction<boolean>) => {
      state.isConnected = action.payload;
      if (action.payload) {
        state.lastUpdateTime = new Date().toISOString();
      }
    },
    
    // Real-time price update from WebSocket
    updatePrice: (state, action: PayloadAction<PriceUpdate>) => {
      const { symbol } = action.payload;
      
      // Update existing quote or create new one
      if (state.quotes[symbol]) {
        state.quotes[symbol] = {
          ...state.quotes[symbol],
          currentPrice: action.payload.price,
          price: action.payload.price,
          change: action.payload.change,
          changePercent: action.payload.changePercent,
          volume: action.payload.volume,
          timestamp: action.payload.timestamp,
        };
      } else {
        state.quotes[symbol] = {
          symbol,
          currentPrice: action.payload.price,
          price: action.payload.price,
          previousClose: action.payload.price - action.payload.change,
          change: action.payload.change,
          changePercent: action.payload.changePercent,
          volume: action.payload.volume,
          dayHigh: action.payload.price,
          dayLow: action.payload.price,
          high: action.payload.price,
          low: action.payload.price,
          open: action.payload.price,
          bid: action.payload.price,
          ask: action.payload.price,
          timestamp: action.payload.timestamp,
        };
      }
      
      state.lastUpdateTime = action.payload.timestamp;
    },
    
    // Batch update multiple prices
    updateMultiplePrices: (state, action: PayloadAction<PriceUpdate[]>) => {
      action.payload.forEach(update => {
        const { symbol } = update;
        
        if (state.quotes[symbol]) {
          state.quotes[symbol] = {
            ...state.quotes[symbol],
            currentPrice: update.price,
            price: update.price,
            change: update.change,
            changePercent: update.changePercent,
            volume: update.volume,
            timestamp: update.timestamp,
          };
        }
      });
      
      if (action.payload.length > 0) {
        state.lastUpdateTime = action.payload[0].timestamp;
      }
    },
    
    // Watchlist management
    addToWatchlist: (state, action: PayloadAction<string>) => {
      if (!state.watchlist.includes(action.payload)) {
        state.watchlist.push(action.payload);
      }
    },
    
    removeFromWatchlist: (state, action: PayloadAction<string>) => {
      state.watchlist = state.watchlist.filter(symbol => symbol !== action.payload);
    },
    
    reorderWatchlist: (state, action: PayloadAction<string[]>) => {
      state.watchlist = action.payload;
    },
    
    // Price alerts management
    addPriceAlert: (state, action: PayloadAction<PriceAlert>) => {
      const { symbol } = action.payload;
      if (!state.priceAlerts[symbol]) {
        state.priceAlerts[symbol] = [];
      }
      state.priceAlerts[symbol].push(action.payload);
    },
    
    removePriceAlert: (state, action: PayloadAction<{ symbol: string; alertId: string }>) => {
      const { symbol, alertId } = action.payload;
      if (state.priceAlerts[symbol]) {
        state.priceAlerts[symbol] = state.priceAlerts[symbol].filter(
          alert => alert.id !== alertId
        );
      }
    },
    
    togglePriceAlert: (state, action: PayloadAction<{ symbol: string; alertId: string }>) => {
      const { symbol, alertId } = action.payload;
      if (state.priceAlerts[symbol]) {
        const alert = state.priceAlerts[symbol].find(alert => alert.id === alertId);
        if (alert) {
          alert.isActive = !alert.isActive;
        }
      }
    },
    
    // Clear search results
    clearSearchResults: (state) => {
      state.searchResults = [];
    },
    
    // Clear historical data for a symbol
    clearHistoricalData: (state, action: PayloadAction<string>) => {
      delete state.historicalData[action.payload];
    },
    
    // Clear all data (for cleanup)
    clearAllData: (state) => {
      state.quotes = {};
      state.historicalData = {};
      state.searchResults = [];
      state.isConnected = false;
      state.lastUpdateTime = null;
    },
  },
  extraReducers: (builder) => {
    // Fetch quote
    builder
      .addCase(fetchQuote.pending, (state) => {
        state.isLoading = true;
        state.error = undefined;
      })
      .addCase(fetchQuote.fulfilled, (state, action) => {
        state.isLoading = false;
        state.quotes[action.payload.symbol] = action.payload;
        state.error = undefined;
      })
      .addCase(fetchQuote.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      });

    // Fetch multiple quotes
    builder
      .addCase(fetchMultipleQuotes.pending, (state) => {
        state.isLoading = true;
        state.error = undefined;
      })
      .addCase(fetchMultipleQuotes.fulfilled, (state, action) => {
        state.isLoading = false;
        action.payload.forEach(quote => {
          state.quotes[quote.symbol] = quote;
        });
        state.error = undefined;
      })
      .addCase(fetchMultipleQuotes.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      });

    // Fetch historical data
    builder
      .addCase(fetchHistoricalData.pending, (state) => {
        state.isLoading = true;
        state.error = undefined;
      })
      .addCase(fetchHistoricalData.fulfilled, (state, action) => {
        state.isLoading = false;
        state.historicalData[action.payload.symbol] = action.payload.data;
        state.error = undefined;
      })
      .addCase(fetchHistoricalData.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      });

    // Search symbols
    builder
      .addCase(searchSymbols.pending, (state) => {
        state.isLoading = true;
        state.error = undefined;
      })
      .addCase(searchSymbols.fulfilled, (state, action) => {
        state.isLoading = false;
        state.searchResults = action.payload;
        state.error = undefined;
      })
      .addCase(searchSymbols.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      });

    // Fetch market overview
    builder
      .addCase(fetchMarketOverview.pending, (state) => {
        state.isLoading = true;
        state.error = undefined;
      })
      .addCase(fetchMarketOverview.fulfilled, (state, action) => {
        state.isLoading = false;
        state.overview = action.payload;
        action.payload.forEach(quote => {
          state.quotes[quote.symbol] = quote;
        });
        state.error = undefined;
      })
      .addCase(fetchMarketOverview.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      });

    // Fetch market summary
    builder
      .addCase(fetchMarketSummary.pending, (state) => {
        state.isLoading = true;
        state.error = undefined;
      })
      .addCase(fetchMarketSummary.fulfilled, (state, action) => {
        state.isLoading = false;
        state.summary = action.payload;
        state.error = undefined;
      })
      .addCase(fetchMarketSummary.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      });
  },
});

// Export actions
export const {
  clearError,
  setConnectionStatus,
  updatePrice,
  updateMultiplePrices,
  addToWatchlist,
  removeFromWatchlist,
  reorderWatchlist,
  addPriceAlert,
  removePriceAlert,
  togglePriceAlert,
  clearSearchResults,
  clearHistoricalData,
  clearAllData,
} = marketDataSlice.actions;

// Export selectors
export const selectQuotes = (state: { marketData: MarketDataState }) => state.marketData.quotes;
export const selectQuote = (symbol: string) => (state: { marketData: MarketDataState }) => 
  state.marketData.quotes[symbol];
export const selectHistoricalData = (state: { marketData: MarketDataState }) => state.marketData.historicalData;
export const selectHistoricalDataForSymbol = (symbol: string) => (state: { marketData: MarketDataState }) => 
  state.marketData.historicalData[symbol];
export const selectSearchResults = (state: { marketData: MarketDataState }) => state.marketData.searchResults;
export const selectWatchlist = (state: { marketData: MarketDataState }) => state.marketData.watchlist;
export const selectPriceAlerts = (state: { marketData: MarketDataState }) => state.marketData.priceAlerts;
export const selectIsMarketDataConnected = (state: { marketData: MarketDataState }) => state.marketData.isConnected;
export const selectLastUpdateTime = (state: { marketData: MarketDataState }) => state.marketData.lastUpdateTime;
export const selectMarketDataLoading = (state: { marketData: MarketDataState }) => state.marketData.isLoading;
export const selectMarketDataError = (state: { marketData: MarketDataState }) => state.marketData.error;

// Export reducer
export default marketDataSlice.reducer;