import React from 'react';
import { OrderStatus, ExecutionStatus } from '../../types/trading';

interface StatusBadgeProps {
  status: OrderStatus | ExecutionStatus | string;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

const statusStyles = {
  // Order Status
  PENDING: { bg: 'bg-yellow-100', text: 'text-yellow-800', border: 'border-yellow-200' },
  SUBMITTED: { bg: 'bg-blue-100', text: 'text-blue-800', border: 'border-blue-200' },
  ACKNOWLEDGED: { bg: 'bg-indigo-100', text: 'text-indigo-800', border: 'border-indigo-200' },
  PARTIALLY_FILLED: { bg: 'bg-orange-100', text: 'text-orange-800', border: 'border-orange-200' },
  FILLED: { bg: 'bg-green-100', text: 'text-green-800', border: 'border-green-200' },
  CANCELLED: { bg: 'bg-gray-100', text: 'text-gray-800', border: 'border-gray-200' },
  REJECTED: { bg: 'bg-red-100', text: 'text-red-800', border: 'border-red-200' },
  EXPIRED: { bg: 'bg-gray-100', text: 'text-gray-600', border: 'border-gray-300' },
  
  // Execution Status
  NEW: { bg: 'bg-blue-100', text: 'text-blue-800', border: 'border-blue-200' },
  TRADE: { bg: 'bg-green-100', text: 'text-green-800', border: 'border-green-200' },
  CANCEL: { bg: 'bg-red-100', text: 'text-red-800', border: 'border-red-200' },
  REPLACE: { bg: 'bg-yellow-100', text: 'text-yellow-800', border: 'border-yellow-200' },
  
  // Connection Status
  CONNECTED: { bg: 'bg-green-100', text: 'text-green-800', border: 'border-green-200' },
  DISCONNECTED: { bg: 'bg-red-100', text: 'text-red-800', border: 'border-red-200' },
  CONNECTING: { bg: 'bg-yellow-100', text: 'text-yellow-800', border: 'border-yellow-200' },
  
  // Default
  UNKNOWN: { bg: 'bg-gray-100', text: 'text-gray-600', border: 'border-gray-300' }
};

const sizeStyles = {
  sm: 'px-2 py-1 text-xs',
  md: 'px-3 py-1.5 text-sm',
  lg: 'px-4 py-2 text-base'
};

export const StatusBadge: React.FC<StatusBadgeProps> = ({
  status,
  size = 'md',
  className = ''
}) => {
  const statusKey = status.toUpperCase() as keyof typeof statusStyles;
  const style = statusStyles[statusKey] || statusStyles.UNKNOWN;
  
  const displayText = status.replace(/_/g, ' ').toLowerCase()
    .split(' ')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');

  return (
    <span
      className={`
        inline-flex items-center rounded-full font-medium border
        ${style.bg} ${style.text} ${style.border}
        ${sizeStyles[size]}
        ${className}
      `}
    >
      {displayText}
    </span>
  );
};

export default StatusBadge;