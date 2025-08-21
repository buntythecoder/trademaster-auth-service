import React from 'react';

interface EmptyStateProps {
  title: string;
  description: string;
  icon?: React.ReactNode;
  action?: {
    label: string;
    onClick: () => void;
    variant?: 'primary' | 'secondary';
  };
  children?: React.ReactNode;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  title,
  description,
  icon,
  action,
  children,
}) => {
  const defaultIcon = (
    <svg className="h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
    </svg>
  );

  return (
    <div className="text-center py-12 px-4">
      <div className="mx-auto mb-4">
        {icon || defaultIcon}
      </div>
      <h3 className="text-lg font-medium text-gray-900 mb-2">{title}</h3>
      <p className="text-gray-500 mb-6 max-w-md mx-auto leading-relaxed">{description}</p>
      
      {action && (
        <button
          onClick={action.onClick}
          className={
            action.variant === 'secondary'
              ? 'btn bg-gray-200 text-gray-700 hover:bg-gray-300'
              : 'btn-primary'
          }
        >
          {action.label}
        </button>
      )}
      
      {children && (
        <div className="mt-6">
          {children}
        </div>
      )}
    </div>
  );
};

// Specific empty states for TradeMaster features
export const NoPortfoliosState: React.FC<{ onCreate: () => void }> = ({ onCreate }) => (
  <EmptyState
    title="No Portfolios Yet"
    description="Create your first portfolio to start tracking your investments and trading performance."
    icon={
      <svg className="h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
      </svg>
    }
    action={{
      label: 'Create Portfolio',
      onClick: onCreate,
    }}
  />
);

export const NoPositionsState: React.FC<{ onTrade: () => void }> = ({ onTrade }) => (
  <EmptyState
    title="No Open Positions"
    description="You don't have any open positions in this portfolio. Start trading to see your positions here."
    icon={
      <svg className="h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
      </svg>
    }
    action={{
      label: 'Start Trading',
      onClick: onTrade,
    }}
  />
);

export const NoOrdersState: React.FC<{ onCreateOrder: () => void }> = ({ onCreateOrder }) => (
  <EmptyState
    title="No Orders Found"
    description="You haven't placed any orders yet. Place your first order to start trading."
    icon={
      <svg className="h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
      </svg>
    }
    action={{
      label: 'Place Order',
      onClick: onCreateOrder,
    }}
  />
);

export const NoWatchlistState: React.FC<{ onAddSymbols: () => void }> = ({ onAddSymbols }) => (
  <EmptyState
    title="Your Watchlist is Empty"
    description="Add stocks to your watchlist to monitor price movements and receive alerts."
    icon={
      <svg className="h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
      </svg>
    }
    action={{
      label: 'Add Symbols',
      onClick: onAddSymbols,
    }}
  />
);

export const NoNotificationsState: React.FC = () => (
  <EmptyState
    title="No Notifications"
    description="You're all caught up! New trading alerts and system notifications will appear here."
    icon={
      <svg className="h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-5 5v-5zM8 17H3l5 5v-5zM18 7a6 6 0 11-12 0 6 6 0 0112 0z" />
      </svg>
    }
  />
);

export const NoSearchResultsState: React.FC<{ query: string; onClear: () => void }> = ({ query, onClear }) => (
  <EmptyState
    title="No Results Found"
    description={`We couldn't find any results for "${query}". Try adjusting your search terms or browse available symbols.`}
    icon={
      <svg className="h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
      </svg>
    }
    action={{
      label: 'Clear Search',
      onClick: onClear,
      variant: 'secondary',
    }}
  />
);

export const NetworkOfflineState: React.FC<{ onRetry: () => void }> = ({ onRetry }) => (
  <EmptyState
    title="You're Offline"
    description="Check your internet connection and try again. Some features may not be available while offline."
    icon={
      <svg className="h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18.364 5.636a9 9 0 010 12.728m0 0l-2.829-2.829m2.829 2.829L21 21M15.536 8.464a5 5 0 010 7.072m0 0l-2.829-2.829m-4.243 2.829a4.978 4.978 0 01-1.414-2.83m-1.414 5.658a9 9 0 01-2.167-9.238m7.824 2.167a1 1 0 111.414 1.414m-1.414-1.414L3 3m8.293 8.293l1.414 1.414" />
      </svg>
    }
    action={{
      label: 'Try Again',
      onClick: onRetry,
    }}
  />
);

export default EmptyState;