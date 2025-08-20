import { z } from 'zod';

// Indian phone number regex (+91 followed by 10 digits)
const INDIAN_PHONE_REGEX = /^\+91[6-9]\d{9}$/;

// Password strength validation
const passwordSchema = z
  .string()
  .min(8, 'Password must be at least 8 characters')
  .regex(/[a-z]/, 'Password must contain at least one lowercase letter')
  .regex(/[A-Z]/, 'Password must contain at least one uppercase letter')
  .regex(/\d/, 'Password must contain at least one number')
  .regex(/[!@#$%^&*(),.?":{}|<>]/, 'Password must contain at least one special character');

// Name validation (prevent special characters, support Indian names)
const nameSchema = z
  .string()
  .min(2, 'Name must be at least 2 characters')
  .max(50, 'Name must not exceed 50 characters')
  .regex(/^[a-zA-Z\s'-]+$/, 'Name can only contain letters, spaces, hyphens, and apostrophes')
  .refine((name) => name.trim().length > 0, 'Name cannot be empty');

// Email validation with common domain validation
const emailSchema = z
  .string()
  .email('Please enter a valid email address')
  .min(5, 'Email must be at least 5 characters')
  .max(100, 'Email must not exceed 100 characters')
  .toLowerCase();

// Indian phone validation with auto-formatting support
const phoneSchema = z
  .string()
  .transform((phone) => {
    // Remove all non-digit characters
    const digits = phone.replace(/\D/g, '');
    
    // If starts with 91, add +
    if (digits.startsWith('91') && digits.length === 12) {
      return `+${digits}`;
    }
    
    // If starts with 0, remove it (common Indian mobile format)
    if (digits.startsWith('0') && digits.length === 11) {
      return `+91${digits.substring(1)}`;
    }
    
    // If 10 digits, add +91
    if (digits.length === 10) {
      return `+91${digits}`;
    }
    
    // Return as-is for further validation
    return phone;
  })
  .refine((phone) => INDIAN_PHONE_REGEX.test(phone), {
    message: 'Please enter a valid Indian mobile number',
  });

// Main registration form schema
export const registrationSchema = z.object({
  firstName: nameSchema.refine(
    (name) => !name.includes('  '), // No double spaces
    'Name cannot contain consecutive spaces'
  ),
  lastName: nameSchema.refine(
    (name) => !name.includes('  '), // No double spaces
    'Name cannot contain consecutive spaces'
  ),
  email: emailSchema,
  phone: phoneSchema,
  password: passwordSchema,
});

// Type inference from schema
export type RegistrationFormData = z.infer<typeof registrationSchema>;

// Password strength calculation utility
export const calculatePasswordStrength = (password: string): {
  score: number;
  label: 'weak' | 'medium' | 'strong';
  checks: {
    length: boolean;
    lowercase: boolean;
    uppercase: boolean;
    number: boolean;
    special: boolean;
  };
} => {
  const checks = {
    length: password.length >= 8,
    lowercase: /[a-z]/.test(password),
    uppercase: /[A-Z]/.test(password),
    number: /\d/.test(password),
    special: /[!@#$%^&*(),.?":{}|<>]/.test(password),
  };

  const score = Object.values(checks).filter(Boolean).length;
  
  let label: 'weak' | 'medium' | 'strong';
  if (score <= 2) label = 'weak';
  else if (score <= 4) label = 'medium';
  else label = 'strong';

  return { score, label, checks };
};

// Phone number formatting utility
export const formatPhoneNumber = (phone: string): string => {
  // Remove all non-digit characters
  const digits = phone.replace(/\D/g, '');
  
  // Handle different input formats
  let formatted = '';
  
  if (digits.startsWith('91') && digits.length >= 3) {
    // Format as +91 XXXXX XXXXX
    const countryCode = '+91';
    const remaining = digits.substring(2);
    
    if (remaining.length <= 5) {
      formatted = `${countryCode} ${remaining}`;
    } else {
      const first = remaining.substring(0, 5);
      const second = remaining.substring(5, 10);
      formatted = `${countryCode} ${first} ${second}`;
    }
  } else if (digits.length <= 10) {
    // Format as +91 XXXXX XXXXX
    if (digits.length <= 5) {
      formatted = `+91 ${digits}`;
    } else {
      const first = digits.substring(0, 5);
      const second = digits.substring(5, 10);
      formatted = `+91 ${first} ${second}`;
    }
  } else {
    // Return as-is if invalid length
    formatted = phone;
  }
  
  return formatted.trim();
};

// Email validation utility (for real-time feedback)
export const validateEmail = (email: string): { isValid: boolean; message?: string } => {
  try {
    emailSchema.parse(email);
    return { isValid: true };
  } catch (error) {
    if (error instanceof z.ZodError) {
      return { isValid: false, message: error.errors[0]?.message };
    }
    return { isValid: false, message: 'Invalid email format' };
  }
};

// Name validation utility (for real-time feedback)
export const validateName = (name: string): { isValid: boolean; message?: string } => {
  try {
    nameSchema.parse(name);
    return { isValid: true };
  } catch (error) {
    if (error instanceof z.ZodError) {
      return { isValid: false, message: error.errors[0]?.message };
    }
    return { isValid: false, message: 'Invalid name format' };
  }
};

// Form validation state types
export interface ValidationState {
  isValid: boolean;
  isDirty: boolean;
  message?: string;
}

export interface FormValidationStates {
  firstName: ValidationState;
  lastName: ValidationState;
  email: ValidationState;
  phone: ValidationState;
  password: ValidationState & {
    strength: ReturnType<typeof calculatePasswordStrength>;
  };
}