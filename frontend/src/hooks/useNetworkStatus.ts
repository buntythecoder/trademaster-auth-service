import { useState, useEffect } from 'react';
import { useAppDispatch } from '../store';
import { addNotification } from '../store/slices/uiSlice';

interface NetworkStatus {
  isOnline: boolean;
  isSlowConnection: boolean;
  connectionType: string | null;
  downlink: number | null;
  rtt: number | null;
}

export const useNetworkStatus = () => {
  const dispatch = useAppDispatch();
  const [networkStatus, setNetworkStatus] = useState<NetworkStatus>({
    isOnline: navigator.onLine,
    isSlowConnection: false,
    connectionType: null,
    downlink: null,
    rtt: null,
  });

  useEffect(() => {
    const updateNetworkStatus = () => {
      const connection = (navigator as any).connection || 
                        (navigator as any).mozConnection || 
                        (navigator as any).webkitConnection;

      const status: NetworkStatus = {
        isOnline: navigator.onLine,
        isSlowConnection: connection?.effectiveType === 'slow-2g' || connection?.effectiveType === '2g',
        connectionType: connection?.effectiveType || null,
        downlink: connection?.downlink || null,
        rtt: connection?.rtt || null,
      };

      setNetworkStatus(prevStatus => {
        // Only dispatch notifications if status actually changed
        if (prevStatus.isOnline !== status.isOnline) {
          if (status.isOnline) {
            dispatch(addNotification({
              type: 'success',
              title: 'Connection Restored',
              message: 'You\'re back online. TradeMaster is fully functional.',
              autoClose: 3000,
            }));
          } else {
            dispatch(addNotification({
              type: 'error',
              title: 'Connection Lost',
              message: 'You\'re offline. Some features may not be available.',
              autoClose: 8000,
            }));
          }
        }

        // Warn about slow connections for trading
        if (status.isSlowConnection && !prevStatus.isSlowConnection) {
          dispatch(addNotification({
            type: 'warning',
            title: 'Slow Connection',
            message: 'Your connection is slow. Real-time data may be delayed.',
            autoClose: 5000,
          }));
        }

        return status;
      });
    };

    const handleOnline = () => updateNetworkStatus();
    const handleOffline = () => updateNetworkStatus();

    // Update on mount
    updateNetworkStatus();

    // Listen for network changes
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    // Listen for connection changes if supported
    const connection = (navigator as any).connection;
    if (connection) {
      connection.addEventListener('change', updateNetworkStatus);
    }

    // Cleanup
    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
      if (connection) {
        connection.removeEventListener('change', updateNetworkStatus);
      }
    };
  }, [dispatch]);

  // Test connection quality periodically
  useEffect(() => {
    if (!networkStatus.isOnline) return;

    const testConnection = async () => {
      try {
        const start = performance.now();
        const response = await fetch('/api/health', { 
          method: 'HEAD',
          cache: 'no-cache',
          signal: AbortSignal.timeout(5000)
        });
        const end = performance.now();
        
        const latency = end - start;
        
        setNetworkStatus(prev => ({
          ...prev,
          rtt: latency,
          isSlowConnection: latency > 2000 || !response.ok,
        }));
        
      } catch (error) {
        console.warn('Connection test failed:', error);
        setNetworkStatus(prev => ({
          ...prev,
          isSlowConnection: true,
        }));
      }
    };

    // Test connection every 30 seconds
    const interval = setInterval(testConnection, 30000);
    
    // Test immediately
    testConnection();

    return () => clearInterval(interval);
  }, [networkStatus.isOnline]);

  return networkStatus;
};

// Hook for checking if user is in a good state for trading
export const useTradingConnection = () => {
  const networkStatus = useNetworkStatus();
  
  const isGoodForTrading = networkStatus.isOnline && 
                          !networkStatus.isSlowConnection &&
                          (networkStatus.rtt === null || networkStatus.rtt < 1000);
  
  const warnings = [];
  
  if (!networkStatus.isOnline) {
    warnings.push('You are offline');
  } else if (networkStatus.isSlowConnection) {
    warnings.push('Slow connection detected');
  } else if (networkStatus.rtt && networkStatus.rtt > 1000) {
    warnings.push('High latency detected');
  }

  return {
    ...networkStatus,
    isGoodForTrading,
    warnings,
  };
};

export default useNetworkStatus;