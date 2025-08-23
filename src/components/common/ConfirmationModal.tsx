import React from 'react';
import { X, AlertTriangle, CheckCircle, Info, XCircle } from 'lucide-react';

interface ConfirmationModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  variant?: 'danger' | 'warning' | 'success' | 'info';
  confirmButtonClass?: string;
  loading?: boolean;
}

const variantStyles = {
  danger: {
    icon: XCircle,
    iconColor: 'text-red-600',
    confirmButton: 'bg-red-600 hover:bg-red-700 text-white',
    borderColor: 'border-red-200'
  },
  warning: {
    icon: AlertTriangle,
    iconColor: 'text-yellow-600',
    confirmButton: 'bg-yellow-600 hover:bg-yellow-700 text-white',
    borderColor: 'border-yellow-200'
  },
  success: {
    icon: CheckCircle,
    iconColor: 'text-green-600',
    confirmButton: 'bg-green-600 hover:bg-green-700 text-white',
    borderColor: 'border-green-200'
  },
  info: {
    icon: Info,
    iconColor: 'text-blue-600',
    confirmButton: 'bg-blue-600 hover:bg-blue-700 text-white',
    borderColor: 'border-blue-200'
  }
};

export const ConfirmationModal: React.FC<ConfirmationModalProps> = ({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  confirmText = 'Confirm',
  cancelText = 'Cancel',
  variant = 'info',
  confirmButtonClass,
  loading = false
}) => {
  if (!isOpen) return null;

  const variantStyle = variantStyles[variant];
  const IconComponent = variantStyle.icon;

  const handleConfirm = () => {
    if (!loading) {
      onConfirm();
    }
  };

  const handleBackdropClick = (e: React.MouseEvent) => {
    if (e.target === e.currentTarget && !loading) {
      onClose();
    }
  };

  return (
    <div
      className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
      onClick={handleBackdropClick}
    >
      <div className={`bg-white rounded-lg shadow-xl max-w-md w-full mx-4 border-2 ${variantStyle.borderColor}`}>
        <div className="p-6">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-3">
              <IconComponent className={`w-6 h-6 ${variantStyle.iconColor}`} />
              <h3 className="text-lg font-semibold text-gray-900">{title}</h3>
            </div>
            {!loading && (
              <button
                onClick={onClose}
                className="text-gray-400 hover:text-gray-600 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            )}
          </div>
          
          <div className="mb-6">
            <p className="text-gray-700 leading-relaxed">{message}</p>
          </div>
          
          <div className="flex justify-end space-x-3">
            <button
              onClick={onClose}
              disabled={loading}
              className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {cancelText}
            </button>
            <button
              onClick={handleConfirm}
              disabled={loading}
              className={`px-4 py-2 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center space-x-2 ${
                confirmButtonClass || variantStyle.confirmButton
              }`}
            >
              {loading ? (
                <>
                  <svg className="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
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
                  <span>Processing...</span>
                </>
              ) : (
                <span>{confirmText}</span>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ConfirmationModal;