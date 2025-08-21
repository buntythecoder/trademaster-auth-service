import React, { useEffect } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAppSelector, useAppDispatch } from '../../store';
import { 
  selectIsAuthenticated, 
  selectIsAuthInitialized, 
  selectCurrentUser,
  initializeAuth,
  getCurrentUser
} from '../../store/slices/authSlice';

// Development mode - bypass authentication when backend is not available
const isDevelopmentMode = import.meta.env.DEV;

interface ProtectedRouteProps {
  children: React.ReactNode;
  requireAuth?: boolean;
  redirectTo?: string;
  requiredRoles?: string[];
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  requireAuth = true,
  redirectTo = '/auth',
  requiredRoles = [],
}) => {
  const dispatch = useAppDispatch();
  const location = useLocation();
  const isAuthenticated = useAppSelector(selectIsAuthenticated);
  const isInitialized = useAppSelector(selectIsAuthInitialized);
  const currentUser = useAppSelector(selectCurrentUser);

  // Initialize auth state on component mount
  useEffect(() => {
    if (!isInitialized && !isDevelopmentMode) {
      dispatch(initializeAuth());
    }
  }, [dispatch, isInitialized]);

  // Fetch user data if authenticated but user data is missing
  useEffect(() => {
    if (isAuthenticated && !currentUser && isInitialized && !isDevelopmentMode) {
      dispatch(getCurrentUser());
    }
  }, [dispatch, isAuthenticated, currentUser, isInitialized]);

  // In development mode, bypass authentication checks
  if (isDevelopmentMode) {
    return <>{children}</>;
  }

  // Show loading while initializing
  if (!isInitialized) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="bg-white rounded-lg shadow-lg p-8 max-w-md w-full mx-4">
          <div className="flex items-center justify-center space-x-3">
            <svg className="animate-spin h-8 w-8 text-primary-600" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
            </svg>
            <span className="text-lg text-gray-700">Loading TradeMaster...</span>
          </div>
        </div>
      </div>
    );
  }

  // If authentication is required but user is not authenticated
  if (requireAuth && !isAuthenticated) {
    return <Navigate to={redirectTo} state={{ from: location }} replace />;
  }

  // If authentication is not required but user is authenticated (e.g., auth pages)
  if (!requireAuth && isAuthenticated) {
    // Check if we have a 'from' location to redirect back to
    const from = location.state?.from?.pathname || '/dashboard';
    return <Navigate to={from} replace />;
  }

  // Check role-based access if required
  if (requiredRoles.length > 0 && currentUser) {
    const hasRequiredRole = requiredRoles.some(role => 
      currentUser.roles?.some(userRole => userRole === role)
    );

    if (!hasRequiredRole) {
      return (
        <div className="min-h-screen bg-gray-50 flex items-center justify-center">
          <div className="bg-white rounded-lg shadow-lg p-8 max-w-md w-full mx-4 text-center">
            <div className="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-danger-100 mb-4">
              <svg className="h-6 w-6 text-danger-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z" />
              </svg>
            </div>
            <h3 className="text-lg font-medium text-gray-900 mb-2">Access Denied</h3>
            <p className="text-gray-600 mb-4">
              You don't have the required permissions to access this page.
            </p>
            <button
              onClick={() => window.history.back()}
              className="btn-primary"
            >
              Go Back
            </button>
          </div>
        </div>
      );
    }
  }

  return <>{children}</>;
};

export default ProtectedRoute;