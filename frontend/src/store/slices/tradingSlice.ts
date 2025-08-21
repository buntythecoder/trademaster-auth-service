import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import type { 
  Order, 
  PlaceOrderRequest, 
  LoadingState, 
  PaginatedResponse 
} from '../../types';
import { tradingApi } from '../../lib/api';

// Async thunks for trading operations
export const fetchOrders = createAsyncThunk(
  'trading/fetchOrders',
  async (params: {
    portfolioId?: string;
    symbol?: string;
    status?: string;
    page?: number;
    size?: number;
  } = {}, { rejectWithValue }) => {
    try {
      return await tradingApi.getOrders(params);
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch orders');
    }
  }
);

export const fetchOrder = createAsyncThunk(
  'trading/fetchOrder',
  async (orderId: string, { rejectWithValue }) => {
    try {
      return await tradingApi.getOrder(orderId);
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to fetch order');
    }
  }
);

export const placeOrder = createAsyncThunk(
  'trading/placeOrder',
  async (orderRequest: PlaceOrderRequest, { rejectWithValue }) => {
    try {
      return await tradingApi.placeOrder(orderRequest);
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to place order');
    }
  }
);

export const cancelOrder = createAsyncThunk(
  'trading/cancelOrder',
  async (orderId: string, { rejectWithValue }) => {
    try {
      return await tradingApi.cancelOrder(orderId);
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to cancel order');
    }
  }
);

export const modifyOrder = createAsyncThunk(
  'trading/modifyOrder',
  async ({ 
    orderId, 
    modifications 
  }: { 
    orderId: string; 
    modifications: Partial<PlaceOrderRequest> 
  }, { rejectWithValue }) => {
    try {
      return await tradingApi.modifyOrder(orderId, modifications);
    } catch (error: any) {
      return rejectWithValue(error.response?.data?.message || 'Failed to modify order');
    }
  }
);

// Trading state interface
interface TradingState extends LoadingState {
  orders: PaginatedResponse<Order> | null;
  currentOrder: Order | null;
  orderHistory: Order[];
  pendingOrderId: string | null;
  orderValidation: {
    isValid: boolean;
    errors: string[];
  };
}

// Initial state
const initialState: TradingState = {
  orders: null,
  currentOrder: null,
  orderHistory: [],
  pendingOrderId: null,
  orderValidation: {
    isValid: true,
    errors: [],
  },
  isLoading: false,
  error: undefined,
};

// Trading slice
const tradingSlice = createSlice({
  name: 'trading',
  initialState,
  reducers: {
    // Clear errors
    clearError: (state) => {
      state.error = undefined;
    },
    
    // Set current order
    setCurrentOrder: (state, action: PayloadAction<Order | null>) => {
      state.currentOrder = action.payload;
    },
    
    // Update order in real-time (for WebSocket updates)
    updateOrder: (state, action: PayloadAction<Order>) => {
      const updatedOrder = action.payload;
      
      // Update in orders list if loaded
      if (state.orders && state.orders.content) {
        const index = state.orders.content.findIndex(order => order.id === updatedOrder.id);
        if (index !== -1) {
          state.orders.content[index] = updatedOrder;
        } else if (updatedOrder.status === 'PENDING') {
          // Add new pending order to the list
          state.orders.content.unshift(updatedOrder);
          state.orders.totalElements += 1;
        }
      }
      
      // Update current order if it matches
      if (state.currentOrder?.id === updatedOrder.id) {
        state.currentOrder = updatedOrder;
      }
      
      // Add to order history if completed or cancelled
      if (updatedOrder.status === 'FILLED' || updatedOrder.status === 'CANCELLED') {
        const existingIndex = state.orderHistory.findIndex(order => order.id === updatedOrder.id);
        if (existingIndex !== -1) {
          state.orderHistory[existingIndex] = updatedOrder;
        } else {
          state.orderHistory.unshift(updatedOrder);
        }
      }
    },
    
    // Set pending order (for tracking order placement)
    setPendingOrderId: (state, action: PayloadAction<string | null>) => {
      state.pendingOrderId = action.payload;
    },
    
    // Validate order parameters
    validateOrder: (state, action: PayloadAction<PlaceOrderRequest>) => {
      const order = action.payload;
      const errors: string[] = [];
      
      // Basic validation
      if (!order.symbol) {
        errors.push('Symbol is required');
      }
      if (!order.quantity || order.quantity <= 0) {
        errors.push('Quantity must be greater than 0');
      }
      if (order.type === 'LIMIT' && (!order.price || order.price <= 0)) {
        errors.push('Price is required for limit orders');
      }
      if (order.type === 'STOP' && (!order.stopPrice || order.stopPrice <= 0)) {
        errors.push('Stop price is required for stop orders');
      }
      
      state.orderValidation = {
        isValid: errors.length === 0,
        errors,
      };
    },
    
    // Clear order validation
    clearOrderValidation: (state) => {
      state.orderValidation = {
        isValid: true,
        errors: [],
      };
    },
    
    // Clear trading data (for cleanup)
    clearTradingData: (state) => {
      state.orders = null;
      state.currentOrder = null;
      state.pendingOrderId = null;
      state.orderValidation = {
        isValid: true,
        errors: [],
      };
    },
  },
  extraReducers: (builder) => {
    // Fetch orders
    builder
      .addCase(fetchOrders.pending, (state) => {
        state.isLoading = true;
        state.error = undefined;
      })
      .addCase(fetchOrders.fulfilled, (state, action) => {
        state.isLoading = false;
        state.orders = action.payload;
        state.error = undefined;
      })
      .addCase(fetchOrders.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      });

    // Fetch order
    builder
      .addCase(fetchOrder.pending, (state) => {
        state.isLoading = true;
        state.error = undefined;
      })
      .addCase(fetchOrder.fulfilled, (state, action) => {
        state.isLoading = false;
        state.currentOrder = action.payload;
        state.error = undefined;
      })
      .addCase(fetchOrder.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      });

    // Place order
    builder
      .addCase(placeOrder.pending, (state) => {
        state.isLoading = true;
        state.error = undefined;
        // Store the pending order details for tracking
        state.pendingOrderId = 'pending';
      })
      .addCase(placeOrder.fulfilled, (state, action) => {
        state.isLoading = false;
        state.pendingOrderId = null;
        
        // Add new order to orders list if loaded
        if (state.orders) {
          state.orders.content.unshift(action.payload);
          state.orders.totalElements += 1;
        }
        
        // Clear order validation after successful placement
        state.orderValidation = {
          isValid: true,
          errors: [],
        };
        
        state.error = undefined;
      })
      .addCase(placeOrder.rejected, (state, action) => {
        state.isLoading = false;
        state.pendingOrderId = null;
        state.error = action.payload as string;
      });

    // Cancel order
    builder
      .addCase(cancelOrder.pending, (state) => {
        state.isLoading = true;
        state.error = undefined;
      })
      .addCase(cancelOrder.fulfilled, (state, action) => {
        state.isLoading = false;
        
        // Update order in orders list
        if (state.orders && state.orders.content) {
          const index = state.orders.content.findIndex(order => order.id === action.payload.id);
          if (index !== -1) {
            state.orders.content[index] = action.payload;
          }
        }
        
        // Update current order if it matches
        if (state.currentOrder?.id === action.payload.id) {
          state.currentOrder = action.payload;
        }
        
        state.error = undefined;
      })
      .addCase(cancelOrder.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      });

    // Modify order
    builder
      .addCase(modifyOrder.pending, (state) => {
        state.isLoading = true;
        state.error = undefined;
      })
      .addCase(modifyOrder.fulfilled, (state, action) => {
        state.isLoading = false;
        
        // Update order in orders list
        if (state.orders && state.orders.content) {
          const index = state.orders.content.findIndex(order => order.id === action.payload.id);
          if (index !== -1) {
            state.orders.content[index] = action.payload;
          }
        }
        
        // Update current order if it matches
        if (state.currentOrder?.id === action.payload.id) {
          state.currentOrder = action.payload;
        }
        
        state.error = undefined;
      })
      .addCase(modifyOrder.rejected, (state, action) => {
        state.isLoading = false;
        state.error = action.payload as string;
      });
  },
});

// Export actions
export const {
  clearError,
  setCurrentOrder,
  updateOrder,
  setPendingOrderId,
  validateOrder,
  clearOrderValidation,
  clearTradingData,
} = tradingSlice.actions;

// Export selectors
export const selectOrders = (state: { trading: TradingState }) => state.trading.orders;
export const selectCurrentOrder = (state: { trading: TradingState }) => state.trading.currentOrder;
export const selectOrderHistory = (state: { trading: TradingState }) => state.trading.orderHistory;
export const selectPendingOrderId = (state: { trading: TradingState }) => state.trading.pendingOrderId;
export const selectOrderValidation = (state: { trading: TradingState }) => state.trading.orderValidation;
export const selectTradingLoading = (state: { trading: TradingState }) => state.trading.isLoading;
export const selectTradingError = (state: { trading: TradingState }) => state.trading.error;

// Helper selectors
export const selectOrdersByStatus = (status: string) => (state: { trading: TradingState }) => 
  state.trading.orders?.content.filter(order => order.status === status) || [];

export const selectOrdersBySymbol = (symbol: string) => (state: { trading: TradingState }) => 
  state.trading.orders?.content.filter(order => order.symbol === symbol) || [];

// Export reducer
export default tradingSlice.reducer;