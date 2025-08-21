import { addNotification } from '../store/slices/uiSlice';
import type { AppDispatch } from '../store';

export interface ApiError {
  message: string;
  code?: string;
  status?: number;
  details?: any;
}

export class ErrorHandler {
  private dispatch: AppDispatch;

  constructor(dispatch: AppDispatch) {
    this.dispatch = dispatch;
  }

  // Handle different types of errors
  handleError(error: any, context?: string) {
    console.error(`Error in ${context || 'unknown context'}:`, error);

    if (this.isNetworkError(error)) {
      this.handleNetworkError(error, context);
    } else if (this.isApiError(error)) {
      this.handleApiError(error, context);
    } else if (this.isValidationError(error)) {
      this.handleValidationError(error, context);
    } else {
      this.handleGenericError(error, context);
    }
  }

  private isNetworkError(error: any): boolean {
    return !navigator.onLine || 
           error.code === 'NETWORK_ERROR' || 
           error.message?.includes('Network Error') ||
           !error.response;
  }

  private isApiError(error: any): boolean {
    return error.response && error.response.status;
  }

  private isValidationError(error: any): boolean {
    return error.response && error.response.status === 400;
  }

  private handleNetworkError(error: any, context?: string) {
    this.dispatch(addNotification({
      type: 'error',
      title: 'Connection Problem',
      message: navigator.onLine 
        ? 'Unable to reach TradeMaster servers. Please try again.'
        : 'You appear to be offline. Check your internet connection.',
      autoClose: 8000,
    }));
  }

  private handleApiError(error: any, context?: string) {
    const status = error.response?.status;
    const message = error.response?.data?.message || error.message;

    switch (status) {
      case 401:
        this.dispatch(addNotification({
          type: 'error',
          title: 'Authentication Required',
          message: 'Please sign in to continue.',
          autoClose: 5000,
        }));
        // Redirect to auth page
        setTimeout(() => {
          window.location.href = '/auth';
        }, 2000);
        break;

      case 403:
        this.dispatch(addNotification({
          type: 'error',
          title: 'Access Denied',
          message: 'You don\'t have permission to perform this action.',
          autoClose: 5000,
        }));
        break;

      case 404:
        this.dispatch(addNotification({
          type: 'error',
          title: 'Not Found',
          message: 'The requested resource could not be found.',
          autoClose: 5000,
        }));
        break;

      case 429:
        this.dispatch(addNotification({
          type: 'warning',
          title: 'Rate Limited',
          message: 'Too many requests. Please slow down and try again.',
          autoClose: 6000,
        }));
        break;

      case 500:
      case 502:
      case 503:
      case 504:
        this.dispatch(addNotification({
          type: 'error',
          title: 'Server Error',
          message: 'Our servers are experiencing issues. Please try again later.',
          autoClose: 8000,
        }));
        break;

      default:
        this.dispatch(addNotification({
          type: 'error',
          title: 'Request Failed',
          message: message || 'An unexpected error occurred.',
          autoClose: 5000,
        }));
    }
  }

  private handleValidationError(error: any, context?: string) {
    const errors = error.response?.data?.errors || [];
    const message = Array.isArray(errors) && errors.length > 0 
      ? errors[0] 
      : error.response?.data?.message || 'Please check your input and try again.';

    this.dispatch(addNotification({
      type: 'warning',
      title: 'Validation Error',
      message,
      autoClose: 6000,
    }));
  }

  private handleGenericError(error: any, context?: string) {
    this.dispatch(addNotification({
      type: 'error',
      title: 'Unexpected Error',
      message: 'Something went wrong. Please try again.',
      autoClose: 5000,
    }));
  }

  // Handle trading-specific errors
  handleTradingError(error: any, operation: string) {
    const context = `Trading - ${operation}`;
    
    if (error.response?.status === 400) {
      const message = error.response?.data?.message || 'Invalid trading parameters.';
      this.dispatch(addNotification({
        type: 'error',
        title: 'Trading Error',
        message,
        autoClose: 6000,
      }));
      return;
    }

    if (error.response?.status === 409) {
      this.dispatch(addNotification({
        type: 'warning',
        title: 'Order Conflict',
        message: 'This order conflicts with existing positions or market conditions.',
        autoClose: 6000,
      }));
      return;
    }

    this.handleError(error, context);
  }

  // Handle portfolio-specific errors
  handlePortfolioError(error: any, operation: string) {
    const context = `Portfolio - ${operation}`;
    
    if (error.response?.status === 403) {
      this.dispatch(addNotification({
        type: 'error',
        title: 'Portfolio Access Denied',
        message: 'You don\'t have permission to modify this portfolio.',
        autoClose: 5000,
      }));
      return;
    }

    this.handleError(error, context);
  }

  // Handle market data errors
  handleMarketDataError(error: any, symbol?: string) {
    const context = `Market Data${symbol ? ` - ${symbol}` : ''}`;
    
    if (error.response?.status === 404) {
      this.dispatch(addNotification({
        type: 'warning',
        title: 'Symbol Not Found',
        message: symbol 
          ? `Market data for ${symbol} is not available.`
          : 'The requested market data is not available.',
        autoClose: 5000,
      }));
      return;
    }

    this.handleError(error, context);
  }
}

// Singleton instance
let errorHandler: ErrorHandler | null = null;

export const initializeErrorHandler = (dispatch: AppDispatch) => {
  errorHandler = new ErrorHandler(dispatch);
  return errorHandler;
};

export const getErrorHandler = (): ErrorHandler | null => {
  return errorHandler;
};

// Utility functions for common error scenarios
export const handleNetworkFailure = (dispatch: AppDispatch) => {
  dispatch(addNotification({
    type: 'error',
    title: 'Connection Lost',
    message: 'Unable to connect to TradeMaster. Retrying automatically...',
    autoClose: 5000,
  }));
};

export const handleMaintenanceMode = (dispatch: AppDispatch) => {
  dispatch(addNotification({
    type: 'info',
    title: 'Maintenance Mode',
    message: 'TradeMaster is undergoing maintenance. Some features may be unavailable.',
    autoClose: 8000,
  }));
};

export const handleSessionExpired = (dispatch: AppDispatch) => {
  dispatch(addNotification({
    type: 'warning',
    title: 'Session Expired',
    message: 'Your session has expired. Please sign in again.',
    autoClose: 5000,
  }));
  
  // Clear local storage and redirect after delay
  setTimeout(() => {
    localStorage.clear();
    window.location.href = '/auth';
  }, 2000);
};

export default ErrorHandler;