import type { Meta, StoryObj } from '@storybook/react';
import { action } from '@storybook/addon-actions';
import { within, userEvent, expect } from '@storybook/test';

import { RegistrationForm } from './RegistrationForm';

const meta = {
  title: 'Auth/RegistrationForm',
  component: RegistrationForm,
  parameters: {
    layout: 'centered',
    docs: {
      description: {
        component: `
Mobile-first registration form for TradeMaster trading platform with:
- Real-time validation with visual feedback
- Indian phone number formatting (+91 auto-detection)
- Password strength indicator with security requirements
- WCAG 2.1 AA accessibility compliance
- Responsive design (320px to 768px width support)
- Touch-optimized controls (44px minimum targets)
        `,
      },
    },
  },
  tags: ['autodocs'],
  argTypes: {
    onSubmit: {
      description: 'Callback function called when form is submitted with valid data',
      action: 'submitted',
    },
    isLoading: {
      description: 'Shows loading state with spinner and disabled submit button',
      control: 'boolean',
    },
    showProgressIndicator: {
      description: 'Shows/hides the progress indicator at the top of the form',
      control: 'boolean',
    },
    currentStep: {
      description: 'Current step number for progress indicator',
      control: { type: 'number', min: 1, max: 10, step: 1 },
    },
    totalSteps: {
      description: 'Total number of steps for progress indicator',
      control: { type: 'number', min: 1, max: 10, step: 1 },
    },
  },
  args: {
    onSubmit: action('form-submitted'),
    isLoading: false,
    showProgressIndicator: true,
    currentStep: 1,
    totalSteps: 3,
  },
} satisfies Meta<typeof RegistrationForm>;

export default meta;
type Story = StoryObj<typeof meta>;

// Default state - empty form
export const Default: Story = {
  args: {},
  parameters: {
    docs: {
      description: {
        story: 'Default empty form with all validation states inactive. Submit button is disabled until all fields are valid.',
      },
    },
  },
};

// Loading state
export const Loading: Story = {
  args: {
    isLoading: true,
  },
  parameters: {
    docs: {
      description: {
        story: 'Form in loading state during submission. Shows spinner and disables all interactions.',
      },
    },
  },
};

// Without progress indicator
export const WithoutProgressIndicator: Story = {
  args: {
    showProgressIndicator: false,
  },
  parameters: {
    docs: {
      description: {
        story: 'Form without the progress indicator, useful when embedded in a larger flow.',
      },
    },
  },
};

// Different step configurations
export const Step2of5: Story = {
  args: {
    currentStep: 2,
    totalSteps: 5,
  },
  parameters: {
    docs: {
      description: {
        story: 'Form configured as step 2 of 5 in a multi-step registration process.',
      },
    },
  },
};

// Form with validation errors
export const WithValidationErrors: Story = {
  args: {},
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    
    // Fill form with invalid data to trigger validation
    await userEvent.type(canvas.getByLabelText(/first name/i), 'A'); // Too short
    await userEvent.type(canvas.getByLabelText(/last name/i), 'Test@123'); // Invalid characters
    await userEvent.type(canvas.getByLabelText(/email address/i), 'invalid-email'); // Invalid format
    await userEvent.type(canvas.getByLabelText(/mobile number/i), '123'); // Invalid phone
    await userEvent.type(canvas.getByLabelText(/password/i), 'weak'); // Weak password
    
    // Trigger validation by tabbing away
    await userEvent.tab();
    
    // Expect submit button to remain disabled
    expect(canvas.getByRole('button', { name: /create account/i })).toBeDisabled();
  },
  parameters: {
    docs: {
      description: {
        story: 'Form with various validation errors displayed. Shows error states and messages for each field type.',
      },
    },
  },
};

// Form with partial valid data
export const PartiallyFilled: Story = {
  args: {},
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    
    // Fill some fields with valid data
    await userEvent.type(canvas.getByLabelText(/first name/i), 'Rahul');
    await userEvent.type(canvas.getByLabelText(/last name/i), 'Sharma');
    await userEvent.type(canvas.getByLabelText(/email address/i), 'rahul@example.com');
    
    // Leave phone and password empty to show partial completion
    await userEvent.tab();
  },
  parameters: {
    docs: {
      description: {
        story: 'Form partially filled with valid data. Shows success states for completed fields and progress indicator.',
      },
    },
  },
};

// Fully valid form ready to submit
export const ReadyToSubmit: Story = {
  args: {},
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    
    // Fill all fields with valid data
    await userEvent.type(canvas.getByLabelText(/first name/i), 'Rahul');
    await userEvent.type(canvas.getByLabelText(/last name/i), 'Sharma');
    await userEvent.type(canvas.getByLabelText(/email address/i), 'rahul@example.com');
    await userEvent.type(canvas.getByLabelText(/mobile number/i), '9876543210');
    await userEvent.type(canvas.getByLabelText(/password/i), 'StrongPassword123!');
    
    // Wait for validation to complete
    await userEvent.tab();
    
    // Submit button should be enabled
    expect(canvas.getByRole('button', { name: /create account/i })).toBeEnabled();
  },
  parameters: {
    docs: {
      description: {
        story: 'Form with all valid data ready for submission. Submit button is enabled and progress shows 100%.',
      },
    },
  },
};

// Phone number formatting demonstration
export const PhoneNumberFormatting: Story = {
  args: {},
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    const phoneInput = canvas.getByLabelText(/mobile number/i);
    
    // Demonstrate different phone number input formats
    await userEvent.type(phoneInput, '9876543210');
    
    // Should automatically format to +91 98765 43210
    expect(phoneInput).toHaveValue('+91 98765 43210');
  },
  parameters: {
    docs: {
      description: {
        story: 'Demonstrates automatic phone number formatting for Indian mobile numbers. Supports various input formats.',
      },
    },
  },
};

// Password strength demonstration
export const PasswordStrengthDemo: Story = {
  args: {},
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    const passwordInput = canvas.getByLabelText(/password/i);
    
    // Type a strong password to show strength indicator
    await userEvent.type(passwordInput, 'StrongPassword123!');
    
    // Password strength indicator should show "strong"
    expect(canvas.getByText(/strong/i)).toBeInTheDocument();
    expect(canvas.getByText(/password strength/i)).toBeInTheDocument();
  },
  parameters: {
    docs: {
      description: {
        story: 'Shows the password strength indicator with real-time feedback and detailed requirements checklist.',
      },
    },
  },
};

// Mobile viewport simulation
export const MobileView: Story = {
  args: {},
  parameters: {
    viewport: {
      defaultViewport: 'mobile1',
    },
    docs: {
      description: {
        story: 'Form optimized for mobile devices (375px width). Shows single-screen layout with touch-optimized controls.',
      },
    },
  },
};

// Tablet viewport simulation
export const TabletView: Story = {
  args: {},
  parameters: {
    viewport: {
      defaultViewport: 'tablet',
    },
    docs: {
      description: {
        story: 'Form displayed on tablet viewport. Name fields show side-by-side layout on larger screens.',
      },
    },
  },
};

// Accessibility testing story
export const AccessibilityDemo: Story = {
  args: {},
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    
    // Test keyboard navigation
    await userEvent.tab(); // First name
    expect(canvas.getByLabelText(/first name/i)).toHaveFocus();
    
    await userEvent.tab(); // Last name
    expect(canvas.getByLabelText(/last name/i)).toHaveFocus();
    
    await userEvent.tab(); // Email
    expect(canvas.getByLabelText(/email address/i)).toHaveFocus();
    
    // Fill invalid email to test error announcement
    await userEvent.type(canvas.getByLabelText(/email address/i), 'invalid');
    await userEvent.tab();
    
    // Error message should be announced to screen readers
    expect(canvas.getByRole('alert')).toBeInTheDocument();
  },
  parameters: {
    docs: {
      description: {
        story: 'Demonstrates accessibility features including keyboard navigation, ARIA labels, and screen reader announcements.',
      },
    },
  },
};

// Dark mode demonstration
export const DarkMode: Story = {
  args: {},
  parameters: {
    backgrounds: {
      default: 'dark',
    },
    docs: {
      description: {
        story: 'Form displayed in dark mode with appropriate color contrast ratios.',
      },
    },
  },
  decorators: [
    (Story) => (
      <div className="dark">
        <div className="bg-gray-900 min-h-screen p-4">
          <Story />
        </div>
      </div>
    ),
  ],
};

// Error handling demonstration
export const SubmissionError: Story = {
  args: {
    onSubmit: async () => {
      // Simulate API error
      throw new Error('Registration failed: Email already exists');
    },
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    
    // Fill valid form
    await userEvent.type(canvas.getByLabelText(/first name/i), 'Rahul');
    await userEvent.type(canvas.getByLabelText(/last name/i), 'Sharma');
    await userEvent.type(canvas.getByLabelText(/email address/i), 'rahul@example.com');
    await userEvent.type(canvas.getByLabelText(/mobile number/i), '9876543210');
    await userEvent.type(canvas.getByLabelText(/password/i), 'StrongPassword123!');
    
    // Attempt submission
    await userEvent.click(canvas.getByRole('button', { name: /create account/i }));
  },
  parameters: {
    docs: {
      description: {
        story: 'Demonstrates error handling during form submission. Parent component should handle errors appropriately.',
      },
    },
  },
};

// Performance testing story
export const PerformanceTest: Story = {
  args: {},
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    
    // Measure form rendering time
    const startTime = performance.now();
    
    // Rapid input to test debouncing
    const passwordInput = canvas.getByLabelText(/password/i);
    await userEvent.type(passwordInput, 'VeryLongPasswordToTestPerformance123!@#$%^&*()');
    
    const endTime = performance.now();
    const renderTime = endTime - startTime;
    
    // Should render within performance budget (<50ms)
    console.log(`Form interaction time: ${renderTime}ms`);
    expect(renderTime).toBeLessThan(100); // Allow some margin for test environment
  },
  parameters: {
    docs: {
      description: {
        story: 'Performance testing story that measures form responsiveness and validates performance targets.',
      },
    },
  },
};

// Validation states showcase
export const ValidationStatesShowcase: Story = {
  args: {},
  render: () => (
    <div className="space-y-8 max-w-4xl">
      <div>
        <h3 className="text-lg font-semibold mb-4">Empty State</h3>
        <RegistrationForm
          onSubmit={action('submitted')}
          showProgressIndicator={false}
        />
      </div>
      
      <div>
        <h3 className="text-lg font-semibold mb-4">Loading State</h3>
        <RegistrationForm
          onSubmit={action('submitted')}
          isLoading={true}
          showProgressIndicator={false}
        />
      </div>
    </div>
  ),
  parameters: {
    docs: {
      description: {
        story: 'Side-by-side comparison of different form states for design review and testing.',
      },
    },
  },
};

// Indian localization context
export const IndianMarketDemo: Story = {
  args: {},
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    
    // Fill with typical Indian user data
    await userEvent.type(canvas.getByLabelText(/first name/i), 'Arjun');
    await userEvent.type(canvas.getByLabelText(/last name/i), 'Patel');
    await userEvent.type(canvas.getByLabelText(/email address/i), 'arjun.patel@gmail.com');
    await userEvent.type(canvas.getByLabelText(/mobile number/i), '9876543210');
    await userEvent.type(canvas.getByLabelText(/password/i), 'Trading@123!');
    
    // Verify Indian phone formatting
    expect(canvas.getByLabelText(/mobile number/i)).toHaveValue('+91 98765 43210');
  },
  parameters: {
    docs: {
      description: {
        story: 'Demonstrates form with typical Indian user data and localized formatting (phone numbers, cultural names).',
      },
    },
  },
};