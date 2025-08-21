import { configureStore } from '@reduxjs/toolkit';
import { TypedUseSelectorHook, useDispatch, useSelector } from 'react-redux';

// Import all slices
import authSlice from './slices/authSlice';
import portfolioSlice from './slices/portfolioSlice';
import marketDataSlice from './slices/marketDataSlice';
import tradingSlice from './slices/tradingSlice';
import uiSlice from './slices/uiSlice';

// Configure the Redux store with all slices
export const store = configureStore({
  reducer: {
    auth: authSlice,
    portfolio: portfolioSlice,
    marketData: marketDataSlice,
    trading: tradingSlice,
    ui: uiSlice,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: {
        ignoredActions: ['persist/PERSIST', 'persist/REHYDRATE'],
        ignoredActionsPaths: ['meta.arg', 'payload.timestamp'],
        ignoredPaths: ['items.dates'],
      },
    }),
  devTools: process.env.NODE_ENV !== 'production',
});

// Export types for TypeScript support
export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;

// Typed hooks for use throughout the app
export const useAppDispatch = () => useDispatch<AppDispatch>();
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;

export default store;