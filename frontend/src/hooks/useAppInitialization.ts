import { useEffect } from 'react';
import { useAppDispatch } from '../store';
import { initializeErrorHandler } from '../utils/errorHandler';
import { initializeAuth } from '../store/slices/authSlice';

export const useAppInitialization = () => {
  const dispatch = useAppDispatch();

  useEffect(() => {
    // Initialize error handler
    initializeErrorHandler(dispatch);

    // Initialize authentication state
    dispatch(initializeAuth());

    // Add global event listeners
    const handleUnhandledRejection = (event: PromiseRejectionEvent) => {
      console.error('Unhandled promise rejection:', event.reason);
      // Prevent the default browser behavior
      event.preventDefault();
    };

    const handleError = (event: ErrorEvent) => {
      console.error('Global error:', event.error);
    };

    // Listen for unhandled promise rejections and errors
    window.addEventListener('unhandledrejection', handleUnhandledRejection);
    window.addEventListener('error', handleError);

    // Cleanup
    return () => {
      window.removeEventListener('unhandledrejection', handleUnhandledRejection);
      window.removeEventListener('error', handleError);
    };
  }, [dispatch]);
};

export default useAppInitialization;