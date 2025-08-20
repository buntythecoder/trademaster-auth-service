// Global type definitions for TradeMaster frontend components

export interface ApiResponse<T = any> {
  success: boolean;
  data?: T;
  error?: string;
  message?: string;
}

export interface ValidationError {
  field: string;
  message: string;
  code?: string;
}

export interface FormSubmissionResponse {
  success: boolean;
  userId?: string;
  token?: string;
  errors?: ValidationError[];
  message?: string;
}

// Re-export form types for easier imports
export type {
  RegistrationFormData,
  FormValidationStates,
  ValidationState,
} from '../components/auth/validation';

export type {
  RegistrationFormProps,
} from '../components/auth/RegistrationForm';