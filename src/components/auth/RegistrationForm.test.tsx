import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';

import { RegistrationForm, RegistrationFormProps } from './RegistrationForm';
import {
  calculatePasswordStrength,
  formatPhoneNumber,
  validateEmail,
  validateName,
  registrationSchema,
} from './validation';

// Mock handlers
const mockOnSubmit = vi.fn();

// Default props
const defaultProps: RegistrationFormProps = {
  onSubmit: mockOnSubmit,
  isLoading: false,
  showProgressIndicator: true,
  currentStep: 1,
  totalSteps: 3,
};

// Helper to render form with props
const renderForm = (props: Partial<RegistrationFormProps> = {}) => {
  return render(<RegistrationForm {...defaultProps} {...props} />);
};

describe('RegistrationForm', () => {
  beforeEach(() => {
    mockOnSubmit.mockClear();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('Component Rendering', () => {
    it('renders all form fields correctly', () => {
      renderForm();

      expect(screen.getByLabelText(/first name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/last name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/mobile number/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /create account/i })).toBeInTheDocument();
    });

    it('displays progress indicator when enabled', () => {
      renderForm({ showProgressIndicator: true });

      expect(screen.getByText(/step 1 of 3/i)).toBeInTheDocument();
      expect(screen.getByRole('progressbar')).toBeInTheDocument();
    });

    it('hides progress indicator when disabled', () => {
      renderForm({ showProgressIndicator: false });

      expect(screen.queryByText(/step 1 of 3/i)).not.toBeInTheDocument();
      expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
    });

    it('displays form header correctly', () => {
      renderForm();

      expect(screen.getByText(/create your account/i)).toBeInTheDocument();
      expect(screen.getByText(/join trademaster and start/i)).toBeInTheDocument();
    });
  });

  describe('Form Fields - Basic Functionality', () => {
    it('allows typing in first name field', async () => {
      const user = userEvent.setup();
      renderForm();

      const firstNameInput = screen.getByLabelText(/first name/i);
      await user.type(firstNameInput, 'Rahul');

      expect(firstNameInput).toHaveValue('Rahul');
    });

    it('allows typing in last name field', async () => {
      const user = userEvent.setup();
      renderForm();

      const lastNameInput = screen.getByLabelText(/last name/i);
      await user.type(lastNameInput, 'Sharma');

      expect(lastNameInput).toHaveValue('Sharma');
    });

    it('allows typing in email field', async () => {
      const user = userEvent.setup();
      renderForm();

      const emailInput = screen.getByLabelText(/email address/i);
      await user.type(emailInput, 'rahul@example.com');

      expect(emailInput).toHaveValue('rahul@example.com');
    });

    it('formats phone number automatically', async () => {
      const user = userEvent.setup();
      renderForm();

      const phoneInput = screen.getByLabelText(/mobile number/i);
      await user.type(phoneInput, '9876543210');

      await waitFor(() => {
        expect(phoneInput).toHaveValue('+91 98765 43210');
      });
    });

    it('allows typing in password field', async () => {
      const user = userEvent.setup();
      renderForm();

      const passwordInput = screen.getByLabelText(/password/i);
      await user.type(passwordInput, 'TestPassword123!');

      expect(passwordInput).toHaveValue('TestPassword123!');
    });
  });

  describe('Real-time Validation', () => {
    it('shows validation success for valid first name', async () => {
      const user = userEvent.setup();
      renderForm();

      const firstNameInput = screen.getByLabelText(/first name/i);
      await user.type(firstNameInput, 'Rahul');
      await user.tab(); // Trigger blur validation

      await waitFor(() => {
        expect(screen.getByText(/looks good/i)).toBeInTheDocument();
      });
    });

    it('shows validation error for invalid email format', async () => {
      const user = userEvent.setup();
      renderForm();

      const emailInput = screen.getByLabelText(/email address/i);
      await user.type(emailInput, 'invalid-email');
      await user.tab(); // Trigger blur validation

      await waitFor(() => {
        expect(screen.getByText(/please enter a valid email address/i)).toBeInTheDocument();
      });
    });

    it('shows validation error for short password', async () => {
      const user = userEvent.setup();
      renderForm();

      const passwordInput = screen.getByLabelText(/password/i);
      await user.type(passwordInput, '123');
      await user.tab(); // Trigger blur validation

      await waitFor(() => {
        expect(screen.getByText(/password must be at least 8 characters/i)).toBeInTheDocument();
      });
    });

    it('shows validation error for names with special characters', async () => {
      const user = userEvent.setup();
      renderForm();

      const firstNameInput = screen.getByLabelText(/first name/i);
      await user.type(firstNameInput, 'Test@123');
      await user.tab(); // Trigger blur validation

      await waitFor(() => {
        expect(screen.getByText(/name can only contain letters/i)).toBeInTheDocument();
      });
    });
  });

  describe('Password Strength Indicator', () => {
    it('displays password strength indicator', async () => {
      const user = userEvent.setup();
      renderForm();

      const passwordInput = screen.getByLabelText(/password/i);
      await user.type(passwordInput, 'TestPassword123!');

      await waitFor(() => {
        expect(screen.getByText(/password strength/i)).toBeInTheDocument();
        expect(screen.getByText(/strong/i)).toBeInTheDocument();
      });
    });

    it('shows individual password requirements', async () => {
      const user = userEvent.setup();
      renderForm();

      const passwordInput = screen.getByLabelText(/password/i);
      await user.type(passwordInput, 'TestPassword123!');

      await waitFor(() => {
        expect(screen.getByText(/8\+ characters/i)).toBeInTheDocument();
        expect(screen.getByText(/lowercase letter/i)).toBeInTheDocument();
        expect(screen.getByText(/uppercase letter/i)).toBeInTheDocument();
        expect(screen.getByText(/number/i)).toBeInTheDocument();
        expect(screen.getByText(/special character/i)).toBeInTheDocument();
      });
    });

    it('updates strength as user types', async () => {
      const user = userEvent.setup();
      renderForm();

      const passwordInput = screen.getByLabelText(/password/i);
      
      // Type weak password
      await user.type(passwordInput, 'weak');
      await waitFor(() => {
        expect(screen.getByText(/weak/i)).toBeInTheDocument();
      });

      // Clear and type stronger password
      await user.clear(passwordInput);
      await user.type(passwordInput, 'StrongPass123!');
      await waitFor(() => {
        expect(screen.getByText(/strong/i)).toBeInTheDocument();
      });
    });
  });

  describe('Phone Number Formatting', () => {
    it('formats 10-digit number correctly', async () => {
      const user = userEvent.setup();
      renderForm();

      const phoneInput = screen.getByLabelText(/mobile number/i);
      await user.type(phoneInput, '9876543210');

      await waitFor(() => {
        expect(phoneInput).toHaveValue('+91 98765 43210');
      });
    });

    it('handles number starting with 0', async () => {
      const user = userEvent.setup();
      renderForm();

      const phoneInput = screen.getByLabelText(/mobile number/i);
      await user.type(phoneInput, '09876543210');

      await waitFor(() => {
        expect(phoneInput).toHaveValue('+91 98765 43210');
      });
    });

    it('handles number already with +91', async () => {
      const user = userEvent.setup();
      renderForm();

      const phoneInput = screen.getByLabelText(/mobile number/i);
      await user.type(phoneInput, '+919876543210');

      await waitFor(() => {
        expect(phoneInput).toHaveValue('+91 98765 43210');
      });
    });
  });

  describe('Form Submission', () => {
    const fillValidForm = async (user: any) => {
      await user.type(screen.getByLabelText(/first name/i), 'Rahul');
      await user.type(screen.getByLabelText(/last name/i), 'Sharma');
      await user.type(screen.getByLabelText(/email address/i), 'rahul@example.com');
      await user.type(screen.getByLabelText(/mobile number/i), '9876543210');
      await user.type(screen.getByLabelText(/password/i), 'TestPassword123!');
    };

    it('enables submit button when form is valid', async () => {
      const user = userEvent.setup();
      renderForm();

      const submitButton = screen.getByRole('button', { name: /create account/i });
      expect(submitButton).toBeDisabled();

      await fillValidForm(user);

      await waitFor(() => {
        expect(submitButton).toBeEnabled();
      });
    });

    it('calls onSubmit with correct data when form is submitted', async () => {
      const user = userEvent.setup();
      renderForm();

      await fillValidForm(user);

      const submitButton = screen.getByRole('button', { name: /create account/i });
      await waitFor(() => expect(submitButton).toBeEnabled());

      await user.click(submitButton);

      await waitFor(() => {
        expect(mockOnSubmit).toHaveBeenCalledWith({
          firstName: 'Rahul',
          lastName: 'Sharma',
          email: 'rahul@example.com',
          phone: '+919876543210',
          password: 'TestPassword123!',
        });
      });
    });

    it('shows loading state during submission', async () => {
      const user = userEvent.setup();
      renderForm({ isLoading: true });

      expect(screen.getByText(/creating account/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /creating account/i })).toBeDisabled();
    });

    it('prevents submission with invalid data', async () => {
      const user = userEvent.setup();
      renderForm();

      // Fill form with invalid data
      await user.type(screen.getByLabelText(/first name/i), 'A'); // Too short
      await user.type(screen.getByLabelText(/email address/i), 'invalid-email');

      const submitButton = screen.getByRole('button', { name: /create account/i });
      expect(submitButton).toBeDisabled();

      await user.click(submitButton);
      expect(mockOnSubmit).not.toHaveBeenCalled();
    });
  });

  describe('Progress Indicator', () => {
    it('updates completion percentage as fields are filled', async () => {
      const user = userEvent.setup();
      renderForm();

      // Initially 0% complete
      expect(screen.getByText(/0% complete/i)).toBeInTheDocument();

      // Fill first name (20% complete)
      await user.type(screen.getByLabelText(/first name/i), 'Rahul');
      await user.tab();

      await waitFor(() => {
        expect(screen.getByText(/20% complete/i)).toBeInTheDocument();
      });

      // Fill more fields
      await user.type(screen.getByLabelText(/last name/i), 'Sharma');
      await user.type(screen.getByLabelText(/email address/i), 'rahul@example.com');
      await user.tab();

      await waitFor(() => {
        expect(screen.getByText(/60% complete/i)).toBeInTheDocument();
      });
    });
  });

  describe('Accessibility', () => {
    it('has proper ARIA labels and roles', () => {
      renderForm();

      expect(screen.getByRole('progressbar')).toHaveAttribute('aria-label', 'Registration progress');
      expect(screen.getByLabelText(/first name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/last name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/mobile number/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    });

    it('announces form state to screen readers', async () => {
      const user = userEvent.setup();
      renderForm();

      const statusElement = screen.getByRole('status');
      expect(statusElement).toHaveTextContent('Please complete all required fields');

      // Fill valid form
      await user.type(screen.getByLabelText(/first name/i), 'Rahul');
      await user.type(screen.getByLabelText(/last name/i), 'Sharma');
      await user.type(screen.getByLabelText(/email address/i), 'rahul@example.com');
      await user.type(screen.getByLabelText(/mobile number/i), '9876543210');
      await user.type(screen.getByLabelText(/password/i), 'TestPassword123!');

      await waitFor(() => {
        expect(statusElement).toHaveTextContent('Form is valid and ready to submit');
      });
    });

    it('has proper error message associations', async () => {
      const user = userEvent.setup();
      renderForm();

      const emailInput = screen.getByLabelText(/email address/i);
      await user.type(emailInput, 'invalid');
      await user.tab();

      await waitFor(() => {
        const errorMessage = screen.getByText(/please enter a valid email address/i);
        expect(errorMessage).toHaveAttribute('role', 'alert');
        expect(errorMessage).toHaveAttribute('aria-live', 'polite');
      });
    });

    it('has proper focus management', async () => {
      const user = userEvent.setup();
      renderForm();

      // Tab through form fields
      await user.tab();
      expect(screen.getByLabelText(/first name/i)).toHaveFocus();

      await user.tab();
      expect(screen.getByLabelText(/last name/i)).toHaveFocus();

      await user.tab();
      expect(screen.getByLabelText(/email address/i)).toHaveFocus();
    });
  });

  describe('Mobile Optimizations', () => {
    it('has proper input modes for different field types', () => {
      renderForm();

      expect(screen.getByLabelText(/email address/i)).toHaveAttribute('inputMode', 'email');
      expect(screen.getByLabelText(/mobile number/i)).toHaveAttribute('inputMode', 'tel');
    });

    it('has appropriate autocomplete attributes', () => {
      renderForm();

      expect(screen.getByLabelText(/email address/i)).toHaveAttribute('autoComplete', 'email');
      expect(screen.getByLabelText(/mobile number/i)).toHaveAttribute('autoComplete', 'tel');
      expect(screen.getByLabelText(/password/i)).toHaveAttribute('autoComplete', 'new-password');
    });

    it('has minimum touch target sizes', () => {
      renderForm();

      const submitButton = screen.getByRole('button', { name: /create account/i });
      const computedStyle = window.getComputedStyle(submitButton);
      
      // Check for minimum 44px height (converted from minHeight CSS)
      expect(submitButton).toHaveClass('min-h-[44px]');
    });
  });

  describe('Edge Cases', () => {
    it('handles rapid typing without breaking', async () => {
      const user = userEvent.setup();
      renderForm();

      const passwordInput = screen.getByLabelText(/password/i);
      
      // Type very fast
      await user.type(passwordInput, 'VeryLongPasswordWithManyCharacters123!@#', { delay: 1 });

      expect(passwordInput).toHaveValue('VeryLongPasswordWithManyCharacters123!@#');
    });

    it('handles phone number with various formats', async () => {
      const user = userEvent.setup();
      renderForm();

      const phoneInput = screen.getByLabelText(/mobile number/i);
      
      // Test with spaces and hyphens
      await user.type(phoneInput, '+91-98765-43210');

      await waitFor(() => {
        expect(phoneInput).toHaveValue('+91 98765 43210');
      });
    });

    it('handles form reset correctly', async () => {
      const user = userEvent.setup();
      renderForm();

      // Fill form
      await user.type(screen.getByLabelText(/first name/i), 'Test');
      await user.type(screen.getByLabelText(/email address/i), 'test@example.com');

      // Reset form (this would be triggered by parent component)
      act(() => {
        const form = screen.getByRole('form') as HTMLFormElement;
        form.reset();
      });

      expect(screen.getByLabelText(/first name/i)).toHaveValue('');
      expect(screen.getByLabelText(/email address/i)).toHaveValue('');
    });
  });
});

// Validation utility tests
describe('Validation Utilities', () => {
  describe('calculatePasswordStrength', () => {
    it('calculates weak password correctly', () => {
      const result = calculatePasswordStrength('weak');
      expect(result.label).toBe('weak');
      expect(result.score).toBe(1); // Only lowercase
    });

    it('calculates strong password correctly', () => {
      const result = calculatePasswordStrength('StrongPass123!');
      expect(result.label).toBe('strong');
      expect(result.score).toBe(5); // All requirements met
    });

    it('tracks individual requirements', () => {
      const result = calculatePasswordStrength('Test123!');
      expect(result.checks.length).toBe(true);
      expect(result.checks.lowercase).toBe(true);
      expect(result.checks.uppercase).toBe(true);
      expect(result.checks.number).toBe(true);
      expect(result.checks.special).toBe(true);
    });
  });

  describe('formatPhoneNumber', () => {
    it('formats 10-digit number', () => {
      expect(formatPhoneNumber('9876543210')).toBe('+91 98765 43210');
    });

    it('handles number with country code', () => {
      expect(formatPhoneNumber('919876543210')).toBe('+91 98765 43210');
    });

    it('handles number starting with 0', () => {
      expect(formatPhoneNumber('09876543210')).toBe('+91 98765 43210');
    });
  });

  describe('validateEmail', () => {
    it('validates correct email', () => {
      const result = validateEmail('test@example.com');
      expect(result.isValid).toBe(true);
    });

    it('rejects invalid email', () => {
      const result = validateEmail('invalid-email');
      expect(result.isValid).toBe(false);
      expect(result.message).toBeDefined();
    });
  });

  describe('validateName', () => {
    it('validates correct name', () => {
      const result = validateName('Rahul');
      expect(result.isValid).toBe(true);
    });

    it('rejects name with special characters', () => {
      const result = validateName('Test@123');
      expect(result.isValid).toBe(false);
      expect(result.message).toBeDefined();
    });
  });

  describe('registrationSchema', () => {
    it('validates complete valid form data', () => {
      const validData = {
        firstName: 'Rahul',
        lastName: 'Sharma',
        email: 'rahul@example.com',
        phone: '+919876543210',
        password: 'TestPassword123!',
      };

      const result = registrationSchema.safeParse(validData);
      expect(result.success).toBe(true);
    });

    it('rejects invalid form data', () => {
      const invalidData = {
        firstName: 'A', // Too short
        lastName: 'B', // Too short
        email: 'invalid-email',
        phone: '123', // Invalid format
        password: 'weak', // Doesn't meet requirements
      };

      const result = registrationSchema.safeParse(invalidData);
      expect(result.success).toBe(false);
    });
  });
});