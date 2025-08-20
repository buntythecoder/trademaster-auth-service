import * as React from "react";
import { cn } from "@/lib/utils";
import {
  FormControl,
  FormDescription,
  FormItem,
  FormLabel,
  FormMessage,
  useFormField,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Progress } from "@/components/ui/progress";

// Icons for validation states
const CheckIcon = () => (
  <svg
    className="h-4 w-4 text-green-600"
    fill="none"
    stroke="currentColor"
    viewBox="0 0 24 24"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
  >
    <path
      strokeLinecap="round"
      strokeLinejoin="round"
      strokeWidth={2}
      d="M5 13l4 4L19 7"
    />
  </svg>
);

const XCircleIcon = () => (
  <svg
    className="h-4 w-4 text-red-600"
    fill="none"
    stroke="currentColor"
    viewBox="0 0 24 24"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
  >
    <path
      strokeLinecap="round"
      strokeLinejoin="round"
      strokeWidth={2}
      d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"
    />
  </svg>
);

const LoadingSpinner = () => (
  <svg
    className="h-4 w-4 animate-spin text-blue-600"
    xmlns="http://www.w3.org/2000/svg"
    fill="none"
    viewBox="0 0 24 24"
    aria-hidden="true"
  >
    <circle
      className="opacity-25"
      cx="12"
      cy="12"
      r="10"
      stroke="currentColor"
      strokeWidth="4"
    ></circle>
    <path
      className="opacity-75"
      fill="currentColor"
      d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
    ></path>
  </svg>
);

// Validation state type
export interface ValidationState {
  isValid: boolean;
  isDirty: boolean;
  isValidating?: boolean;
  message?: string;
}

// Password strength indicator component
interface PasswordStrengthProps {
  strength: {
    score: number;
    label: 'weak' | 'medium' | 'strong';
    checks: {
      length: boolean;
      lowercase: boolean;
      uppercase: boolean;
      number: boolean;
      special: boolean;
    };
  };
  className?: string;
}

const PasswordStrength: React.FC<PasswordStrengthProps> = ({ 
  strength, 
  className 
}) => {
  const { score, label, checks } = strength;
  const progress = (score / 5) * 100;

  const strengthColors = {
    weak: 'bg-red-500',
    medium: 'bg-orange-500',
    strong: 'bg-green-500',
  };

  const strengthTextColors = {
    weak: 'text-red-600',
    medium: 'text-orange-600',
    strong: 'text-green-600',
  };

  return (
    <div className={cn("mt-2 space-y-2", className)} role="group" aria-label="Password strength indicator">
      <div className="flex items-center justify-between">
        <span className="text-sm text-gray-600">Password strength:</span>
        <span 
          className={cn("text-sm font-medium capitalize", strengthTextColors[label])}
          aria-live="polite"
        >
          {label}
        </span>
      </div>
      
      <div className="relative">
        <Progress 
          value={progress} 
          className="h-2"
          aria-label={`Password strength: ${label}`}
        />
        <div 
          className={cn(
            "absolute inset-0 h-2 rounded-full transition-all duration-300",
            strengthColors[label]
          )}
          style={{ width: `${progress}%` }}
        />
      </div>

      {/* Detailed requirements */}
      <div className="grid grid-cols-1 gap-1 text-xs" role="list" aria-label="Password requirements">
        {Object.entries(checks).map(([requirement, met]) => (
          <div 
            key={requirement} 
            className={cn(
              "flex items-center gap-2",
              met ? "text-green-600" : "text-gray-500"
            )}
            role="listitem"
          >
            {met ? <CheckIcon /> : <span className="h-4 w-4" />}
            <span>
              {requirement === 'length' && '8+ characters'}
              {requirement === 'lowercase' && 'Lowercase letter'}
              {requirement === 'uppercase' && 'Uppercase letter'}
              {requirement === 'number' && 'Number'}
              {requirement === 'special' && 'Special character'}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
};

// Main FormField component props
interface FormFieldWrapperProps extends React.ComponentProps<"div"> {
  label: string;
  description?: string;
  validationState?: ValidationState;
  showPasswordStrength?: boolean;
  passwordStrength?: PasswordStrengthProps['strength'];
  children: React.ReactNode;
}

export const FormFieldWrapper: React.FC<FormFieldWrapperProps> = ({
  label,
  description,
  validationState,
  showPasswordStrength = false,
  passwordStrength,
  children,
  className,
  ...props
}) => {
  const { error, formItemId, formDescriptionId, formMessageId } = useFormField();
  const hasError = !!error;
  const isValid = validationState?.isValid && validationState?.isDirty && !hasError;
  const isValidating = validationState?.isValidating;

  return (
    <FormItem className={cn("space-y-2", className)} {...props}>
      <FormLabel className="flex items-center justify-between">
        <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
          {label}
        </span>
        
        {/* Validation state icon */}
        <div className="flex items-center" aria-hidden="true">
          {isValidating && <LoadingSpinner />}
          {!isValidating && isValid && <CheckIcon />}
          {!isValidating && hasError && <XCircleIcon />}
        </div>
      </FormLabel>

      <FormControl>
        <div className="relative">
          {children}
        </div>
      </FormControl>

      {description && (
        <FormDescription className="text-sm text-gray-600 dark:text-gray-400">
          {description}
        </FormDescription>
      )}

      {/* Error message */}
      <FormMessage 
        className="text-sm text-red-600 dark:text-red-400" 
        role="alert"
        aria-live="polite"
      />

      {/* Success message for valid fields */}
      {isValid && !hasError && (
        <p 
          className="text-sm text-green-600 dark:text-green-400"
          role="status"
          aria-live="polite"
        >
          ✓ Looks good!
        </p>
      )}

      {/* Password strength indicator */}
      {showPasswordStrength && passwordStrength && (
        <PasswordStrength strength={passwordStrength} />
      )}
    </FormItem>
  );
};

// Enhanced input component with mobile-first design
interface EnhancedInputProps extends React.ComponentProps<typeof Input> {
  validationState?: ValidationState;
  onValueChange?: (value: string) => void;
}

export const EnhancedInput: React.FC<EnhancedInputProps> = ({
  validationState,
  onValueChange,
  onChange,
  className,
  ...props
}) => {
  const { error } = useFormField();
  const hasError = !!error;
  const isValid = validationState?.isValid && validationState?.isDirty && !hasError;

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onChange?.(e);
    onValueChange?.(e.target.value);
  };

  return (
    <Input
      onChange={handleChange}
      className={cn(
        // Base mobile-first styles
        "w-full text-base", // 16px prevents zoom on iOS
        
        // Validation state styles
        hasError && [
          "border-red-500 focus-visible:border-red-500",
          "focus-visible:ring-red-500/20 dark:focus-visible:ring-red-500/40"
        ],
        
        isValid && [
          "border-green-500 focus-visible:border-green-500",
          "focus-visible:ring-green-500/20 dark:focus-visible:ring-green-500/40"
        ],
        
        // Mobile optimizations
        "touch-manipulation", // Improves touch response
        "selection:bg-blue-200", // Better text selection on mobile
        
        className
      )}
      aria-invalid={hasError}
      {...props}
    />
  );
};

// Pre-configured form fields for common use cases
export const TextFormField: React.FC<{
  name: string;
  label: string;
  placeholder?: string;
  description?: string;
  validationState?: ValidationState;
  onValueChange?: (value: string) => void;
}> = ({ name, label, placeholder, description, validationState, onValueChange }) => (
  <FormFieldWrapper
    label={label}
    description={description}
    validationState={validationState}
  >
    <EnhancedInput
      name={name}
      placeholder={placeholder}
      validationState={validationState}
      onValueChange={onValueChange}
      autoComplete="off"
      spellCheck="false"
    />
  </FormFieldWrapper>
);

export const EmailFormField: React.FC<{
  name: string;
  label: string;
  placeholder?: string;
  description?: string;
  validationState?: ValidationState;
  onValueChange?: (value: string) => void;
}> = ({ name, label, placeholder, description, validationState, onValueChange }) => (
  <FormFieldWrapper
    label={label}
    description={description}
    validationState={validationState}
  >
    <EnhancedInput
      name={name}
      type="email"
      placeholder={placeholder}
      validationState={validationState}
      onValueChange={onValueChange}
      autoComplete="email"
      inputMode="email"
    />
  </FormFieldWrapper>
);

export const PhoneFormField: React.FC<{
  name: string;
  label: string;
  placeholder?: string;
  description?: string;
  validationState?: ValidationState;
  onValueChange?: (value: string) => void;
}> = ({ name, label, placeholder, description, validationState, onValueChange }) => (
  <FormFieldWrapper
    label={label}
    description={description}
    validationState={validationState}
  >
    <EnhancedInput
      name={name}
      type="tel"
      placeholder={placeholder}
      validationState={validationState}
      onValueChange={onValueChange}
      autoComplete="tel"
      inputMode="tel"
    />
  </FormFieldWrapper>
);

export const PasswordFormField: React.FC<{
  name: string;
  label: string;
  placeholder?: string;
  description?: string;
  validationState?: ValidationState & {
    strength?: PasswordStrengthProps['strength'];
  };
  onValueChange?: (value: string) => void;
  showStrengthIndicator?: boolean;
}> = ({ 
  name, 
  label, 
  placeholder, 
  description, 
  validationState, 
  onValueChange,
  showStrengthIndicator = false 
}) => (
  <FormFieldWrapper
    label={label}
    description={description}
    validationState={validationState}
    showPasswordStrength={showStrengthIndicator}
    passwordStrength={validationState?.strength}
  >
    <EnhancedInput
      name={name}
      type="password"
      placeholder={placeholder}
      validationState={validationState}
      onValueChange={onValueChange}
      autoComplete="new-password"
    />
  </FormFieldWrapper>
);

export { PasswordStrength };