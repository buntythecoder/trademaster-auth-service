import React from 'react';

interface LoadingSpinnerProps {
  size?: 'sm' | 'md' | 'lg' | 'xl';
  color?: 'primary' | 'white' | 'gray';
  text?: string;
  fullScreen?: boolean;
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
  size = 'md',
  color = 'primary',
  text,
  fullScreen = false,
}) => {
  const getSizeClasses = () => {
    switch (size) {
      case 'sm':
        return 'h-4 w-4';
      case 'md':
        return 'h-6 w-6';
      case 'lg':
        return 'h-8 w-8';
      case 'xl':
        return 'h-12 w-12';
      default:
        return 'h-6 w-6';
    }
  };

  const getColorClasses = () => {
    switch (color) {
      case 'primary':
        return 'text-primary-600';
      case 'white':
        return 'text-white';
      case 'gray':
        return 'text-gray-400';
      default:
        return 'text-primary-600';
    }
  };

  const getTextSize = () => {
    switch (size) {
      case 'sm':
        return 'text-sm';
      case 'md':
        return 'text-base';
      case 'lg':
        return 'text-lg';
      case 'xl':
        return 'text-xl';
      default:
        return 'text-base';
    }
  };

  const spinner = (
    <div className="flex items-center justify-center">
      <svg
        className={`animate-spin ${getSizeClasses()} ${getColorClasses()}`}
        fill="none"
        viewBox="0 0 24 24"
      >
        <circle
          className="opacity-25"
          cx="12"
          cy="12"
          r="10"
          stroke="currentColor"
          strokeWidth="4"
        />
        <path
          className="opacity-75"
          fill="currentColor"
          d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
        />
      </svg>
      {text && (
        <span className={`ml-3 ${getTextSize()} text-gray-700 font-medium`}>
          {text}
        </span>
      )}
    </div>
  );

  if (fullScreen) {
    return (
      <div className="fixed inset-0 bg-white bg-opacity-75 flex items-center justify-center z-50">
        <div className="bg-white rounded-lg shadow-lg p-6">
          {spinner}
        </div>
      </div>
    );
  }

  return spinner;
};

// Skeleton loading components for better UX
export const SkeletonCard: React.FC<{ className?: string }> = ({ className = '' }) => (
  <div className={`bg-white rounded-lg shadow p-6 animate-pulse ${className}`}>
    <div className="flex items-center justify-between mb-4">
      <div className="h-4 bg-gray-200 rounded w-1/3"></div>
      <div className="h-8 w-8 bg-gray-200 rounded"></div>
    </div>
    <div className="h-8 bg-gray-200 rounded w-2/3 mb-4"></div>
    <div className="h-3 bg-gray-200 rounded w-1/2"></div>
  </div>
);

export const SkeletonTable: React.FC<{ rows?: number; columns?: number }> = ({ 
  rows = 5, 
  columns = 4 
}) => (
  <div className="bg-white rounded-lg shadow overflow-hidden animate-pulse">
    {/* Header */}
    <div className="px-6 py-4 border-b border-gray-200">
      <div className="grid grid-cols-4 gap-4">
        {Array.from({ length: columns }).map((_, i) => (
          <div key={i} className="h-4 bg-gray-200 rounded"></div>
        ))}
      </div>
    </div>
    
    {/* Rows */}
    <div className="divide-y divide-gray-200">
      {Array.from({ length: rows }).map((_, rowIndex) => (
        <div key={rowIndex} className="px-6 py-4">
          <div className="grid grid-cols-4 gap-4">
            {Array.from({ length: columns }).map((_, colIndex) => (
              <div key={colIndex} className="h-4 bg-gray-200 rounded"></div>
            ))}
          </div>
        </div>
      ))}
    </div>
  </div>
);

export const SkeletonChart: React.FC<{ className?: string }> = ({ className = '' }) => (
  <div className={`bg-white rounded-lg shadow p-6 animate-pulse ${className}`}>
    <div className="flex items-center justify-between mb-4">
      <div className="h-5 bg-gray-200 rounded w-1/4"></div>
      <div className="flex space-x-2">
        {['1D', '1W', '1M', '3M', '1Y'].map((_, i) => (
          <div key={i} className="h-6 w-8 bg-gray-200 rounded"></div>
        ))}
      </div>
    </div>
    <div className="h-64 bg-gray-200 rounded"></div>
  </div>
);

export const SkeletonList: React.FC<{ items?: number }> = ({ items = 3 }) => (
  <div className="bg-white rounded-lg shadow divide-y divide-gray-200 animate-pulse">
    {Array.from({ length: items }).map((_, i) => (
      <div key={i} className="px-6 py-4 flex items-center space-x-4">
        <div className="h-10 w-10 bg-gray-200 rounded-full"></div>
        <div className="flex-1">
          <div className="h-4 bg-gray-200 rounded w-3/4 mb-2"></div>
          <div className="h-3 bg-gray-200 rounded w-1/2"></div>
        </div>
        <div className="h-6 w-16 bg-gray-200 rounded"></div>
      </div>
    ))}
  </div>
);

export default LoadingSpinner;