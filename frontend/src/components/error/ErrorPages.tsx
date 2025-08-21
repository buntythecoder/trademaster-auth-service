import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppDispatch } from '../../store';
import { addNotification } from '../../store/slices/uiSlice';

interface ErrorPageProps {
  title?: string;
  message?: string;
  statusCode?: number;
  showBackButton?: boolean;
  showHomeButton?: boolean;
  actionButton?: {
    text: string;
    action: () => void;
  };
}

const BaseErrorPage: React.FC<ErrorPageProps> = ({
  title,
  message,
  statusCode,
  showBackButton = true,
  showHomeButton = true,
  actionButton,
}) => {
  const navigate = useNavigate();

  const handleGoBack = () => {
    if (window.history.length > 1) {
      navigate(-1);
    } else {
      navigate('/dashboard');
    }
  };

  const handleGoHome = () => {
    navigate('/dashboard');
  };

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
      <div className="max-w-lg w-full bg-white rounded-lg shadow-lg p-8 text-center">
        {statusCode && (
          <div className="mb-6">
            <h2 className="text-6xl font-bold text-primary-600">{statusCode}</h2>
          </div>
        )}
        
        <div className="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-danger-100 mb-6">
          <svg className="h-8 w-8 text-danger-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        
        <h1 className="text-2xl font-bold text-gray-900 mb-4">{title}</h1>
        <p className="text-gray-600 mb-8 leading-relaxed">{message}</p>

        <div className="flex flex-col sm:flex-row gap-3">
          {actionButton && (
            <button
              onClick={actionButton.action}
              className="btn-primary flex-1"
            >
              {actionButton.text}
            </button>
          )}
          
          {showBackButton && (
            <button
              onClick={handleGoBack}
              className="btn bg-gray-200 text-gray-700 hover:bg-gray-300 flex-1"
            >
              <svg className="h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
              </svg>
              Go Back
            </button>
          )}
          
          {showHomeButton && (
            <button
              onClick={handleGoHome}
              className="btn-primary flex-1"
            >
              <svg className="h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
              </svg>
              Dashboard
            </button>
          )}
        </div>

        <div className="mt-8 pt-6 border-t border-gray-200">
          <p className="text-sm text-gray-500">
            Need assistance?{' '}
            <a href="mailto:support@trademaster.com" className="text-primary-600 hover:text-primary-500">
              Contact Support
            </a>
          </p>
        </div>
      </div>
    </div>
  );
};

// 404 - Not Found
export const NotFoundPage: React.FC = () => (
  <BaseErrorPage
    statusCode={404}
    title="Page Not Found"
    message="The page you're looking for doesn't exist or has been moved. Please check the URL or navigate to a different section."
  />
);

// 401 - Unauthorized
export const UnauthorizedPage: React.FC = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();

  const handleLogin = () => {
    navigate('/auth');
    dispatch(addNotification({
      type: 'info',
      title: 'Authentication Required',
      message: 'Please sign in to access this page.',
    }));
  };

  return (
    <BaseErrorPage
      statusCode={401}
      title="Authentication Required"
      message="You need to sign in to access this page. Your session may have expired or you don't have the necessary permissions."
      showBackButton={false}
      actionButton={{
        text: 'Sign In',
        action: handleLogin,
      }}
    />
  );
};

// 403 - Forbidden
export const ForbiddenPage: React.FC = () => {
  const dispatch = useAppDispatch();

  const handleContactSupport = () => {
    dispatch(addNotification({
      type: 'info',
      title: 'Support Contacted',
      message: 'We will review your access request and get back to you soon.',
    }));
  };

  return (
    <BaseErrorPage
      statusCode={403}
      title="Access Denied"
      message="You don't have permission to access this resource. If you believe this is an error, please contact support for assistance."
      actionButton={{
        text: 'Request Access',
        action: handleContactSupport,
      }}
    />
  );
};

// 500 - Internal Server Error
export const ServerErrorPage: React.FC = () => {
  const dispatch = useAppDispatch();

  const handleRetry = () => {
    dispatch(addNotification({
      type: 'info',
      title: 'Retrying...',
      message: 'Attempting to reconnect to our servers.',
    }));
    
    // Retry by reloading the page
    setTimeout(() => {
      window.location.reload();
    }, 1000);
  };

  return (
    <BaseErrorPage
      statusCode={500}
      title="Server Error"
      message="We're experiencing some technical difficulties on our end. Our team has been notified and is working on a fix."
      actionButton={{
        text: 'Retry',
        action: handleRetry,
      }}
    />
  );
};

// Network/Connection Error
export const NetworkErrorPage: React.FC = () => {
  const dispatch = useAppDispatch();

  const handleRetry = () => {
    dispatch(addNotification({
      type: 'info',
      title: 'Checking Connection...',
      message: 'Testing your network connection.',
    }));
    
    // Simple connectivity check
    setTimeout(() => {
      if (navigator.onLine) {
        window.location.reload();
      } else {
        dispatch(addNotification({
          type: 'error',
          title: 'Still Offline',
          message: 'Please check your internet connection and try again.',
        }));
      }
    }, 1500);
  };

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
      <div className="max-w-lg w-full bg-white rounded-lg shadow-lg p-8 text-center">
        <div className="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-warning-100 mb-6">
          <svg className="h-8 w-8 text-warning-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        
        <h1 className="text-2xl font-bold text-gray-900 mb-4">Connection Problem</h1>
        <p className="text-gray-600 mb-8 leading-relaxed">
          We're having trouble connecting to our servers. This could be due to a network issue or temporary server maintenance.
        </p>

        <div className="bg-gray-50 rounded-lg p-4 mb-6">
          <h3 className="text-sm font-medium text-gray-900 mb-2">Troubleshooting Steps:</h3>
          <ul className="text-sm text-gray-600 text-left space-y-1">
            <li>• Check your internet connection</li>
            <li>• Try refreshing the page</li>
            <li>• Clear your browser cache</li>
            <li>• Contact support if the problem persists</li>
          </ul>
        </div>

        <div className="flex flex-col sm:flex-row gap-3">
          <button
            onClick={handleRetry}
            className="btn-primary flex-1"
          >
            <svg className="h-4 w-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            Try Again
          </button>
        </div>

        <div className="mt-8 pt-6 border-t border-gray-200">
          <p className="text-sm text-gray-500">
            Connection Status: {navigator.onLine ? (
              <span className="text-success-600 font-medium">Online</span>
            ) : (
              <span className="text-danger-600 font-medium">Offline</span>
            )}
          </p>
        </div>
      </div>
    </div>
  );
};

// Maintenance Mode
export const MaintenancePage: React.FC = () => (
  <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
    <div className="max-w-lg w-full bg-white rounded-lg shadow-lg p-8 text-center">
      <div className="mx-auto flex items-center justify-center h-16 w-16 rounded-full bg-primary-100 mb-6">
        <svg className="h-8 w-8 text-primary-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
        </svg>
      </div>
      
      <h1 className="text-2xl font-bold text-gray-900 mb-4">Under Maintenance</h1>
      <p className="text-gray-600 mb-8 leading-relaxed">
        TradeMaster is currently undergoing scheduled maintenance to improve your trading experience. 
        We'll be back online shortly.
      </p>

      <div className="bg-primary-50 rounded-lg p-4 mb-6">
        <p className="text-sm text-primary-800">
          <strong>Estimated completion:</strong> 30 minutes<br />
          <strong>Services affected:</strong> Trading, Portfolio Management
        </p>
      </div>

      <div className="mt-8 pt-6 border-t border-gray-200">
        <p className="text-sm text-gray-500">
          Follow our status updates:{' '}
          <a href="https://status.trademaster.com" className="text-primary-600 hover:text-primary-500">
            status.trademaster.com
          </a>
        </p>
      </div>
    </div>
  </div>
);

export default {
  NotFoundPage,
  UnauthorizedPage,
  ForbiddenPage,
  ServerErrorPage,
  NetworkErrorPage,
  MaintenancePage,
};