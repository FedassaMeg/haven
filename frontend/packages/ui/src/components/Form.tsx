import React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';

const formVariants = cva(
  'space-y-6',
  {
    variants: {},
    defaultVariants: {},
  }
);

export interface FormProps
  extends React.FormHTMLAttributes<HTMLFormElement>,
    VariantProps<typeof formVariants> {
  onSubmit: (e: React.FormEvent) => void;
  children: React.ReactNode;
}

export const Form: React.FC<FormProps> = ({ onSubmit, children, className, ...props }) => {
  return (
    <form onSubmit={onSubmit} className={formVariants({ className })} {...props}>
      {children}
    </form>
  );
};

const formFieldVariants = cva(
  'space-y-1',
  {
    variants: {},
    defaultVariants: {},
  }
);

const labelVariants = cva(
  'label',
  {
    variants: {
      required: {
        true: 'after:content-["*"] after:text-error-500 after:ml-1',
        false: '',
      },
    },
    defaultVariants: {
      required: false,
    },
  }
);

export interface FormFieldProps extends VariantProps<typeof formFieldVariants> {
  label?: string;
  error?: string;
  required?: boolean;
  children: React.ReactNode;
  className?: string;
}

export const FormField: React.FC<FormFieldProps> = ({
  label,
  error,
  required,
  children,
  className,
}) => {
  return (
    <div className={formFieldVariants({ className })}>
      {label && (
        <label className={labelVariants({ required })}>
          {label}
        </label>
      )}
      {children}
      {error && (
        <p className="text-sm text-error-600" role="alert">
          {error}
        </p>
      )}
    </div>
  );
};

const formActionsVariants = cva(
  'flex items-center space-x-3 pt-4',
  {
    variants: {
      align: {
        left: 'justify-start',
        center: 'justify-center',
        right: 'justify-end',
      },
    },
    defaultVariants: {
      align: 'right',
    },
  }
);

export interface FormActionsProps extends VariantProps<typeof formActionsVariants> {
  children: React.ReactNode;
  className?: string;
}

export const FormActions: React.FC<FormActionsProps> = ({
  children,
  className,
  align = 'right',
}) => {
  return (
    <div
      className={formActionsVariants({ align, className })}
    >
      {children}
    </div>
  );
};

// Form validation helpers
export type ValidationRule<T> = {
  required?: boolean | string;
  minLength?: number | { value: number; message: string };
  maxLength?: number | { value: number; message: string };
  pattern?: RegExp | { value: RegExp; message: string };
  validate?: (value: T) => boolean | string;
};

export type FormErrors<T> = Partial<Record<keyof T, string>>;

export function validateField<T>(value: T, rules: ValidationRule<T>): string | undefined {
  if (rules.required) {
    const isEmpty = value === undefined || value === null || value === '';
    if (isEmpty) {
      return typeof rules.required === 'string' ? rules.required : 'This field is required';
    }
  }

  if (typeof value === 'string') {
    if (rules.minLength) {
      const minLength = typeof rules.minLength === 'number' ? rules.minLength : rules.minLength.value;
      const message = typeof rules.minLength === 'number' 
        ? `Must be at least ${minLength} characters`
        : rules.minLength.message;
      
      if (value.length < minLength) {
        return message;
      }
    }

    if (rules.maxLength) {
      const maxLength = typeof rules.maxLength === 'number' ? rules.maxLength : rules.maxLength.value;
      const message = typeof rules.maxLength === 'number'
        ? `Must be no more than ${maxLength} characters`
        : rules.maxLength.message;
      
      if (value.length > maxLength) {
        return message;
      }
    }

    if (rules.pattern) {
      const pattern = rules.pattern instanceof RegExp ? rules.pattern : rules.pattern.value;
      const message = rules.pattern instanceof RegExp
        ? 'Invalid format'
        : rules.pattern.message;
      
      if (!pattern.test(value)) {
        return message;
      }
    }
  }

  if (rules.validate) {
    const result = rules.validate(value);
    if (result !== true) {
      return typeof result === 'string' ? result : 'Invalid value';
    }
  }

  return undefined;
}

export function validateForm<T extends Record<string, any>>(
  data: T,
  rules: Partial<Record<keyof T, ValidationRule<any>>>
): FormErrors<T> {
  const errors: FormErrors<T> = {};

  for (const [field, fieldRules] of Object.entries(rules) as [keyof T, ValidationRule<any>][]) {
    const error = validateField(data[field], fieldRules);
    if (error) {
      errors[field] = error;
    }
  }

  return errors;
}

// Custom hook for form state management
export function useForm<T extends Record<string, any>>(
  initialValues: T,
  validationRules?: Partial<Record<keyof T, ValidationRule<any>>>
) {
  const [values, setValues] = React.useState<T>(initialValues);
  const [errors, setErrors] = React.useState<FormErrors<T>>({});
  const [touched, setTouched] = React.useState<Partial<Record<keyof T, boolean>>>({});

  const setValue = React.useCallback((field: keyof T, value: any) => {
    setValues(prev => ({ ...prev, [field]: value }));
    
    if (validationRules?.[field]) {
      const error = validateField(value, validationRules[field]);
      setErrors(prev => ({ ...prev, [field]: error }));
    }
  }, [validationRules]);

  const setFieldTouched = React.useCallback((field: keyof T) => {
    setTouched(prev => ({ ...prev, [field]: true }));
  }, []);

  const validate = React.useCallback(() => {
    if (!validationRules) return {};
    
    const formErrors = validateForm(values, validationRules);
    setErrors(formErrors);
    return formErrors;
  }, [values, validationRules]);

  const isValid = React.useMemo(() => {
    return Object.keys(errors).length === 0;
  }, [errors]);

  const reset = React.useCallback(() => {
    setValues(initialValues);
    setErrors({});
    setTouched({});
  }, [initialValues]);

  return {
    values,
    errors,
    touched,
    isValid,
    setValue,
    setFieldTouched,
    validate,
    reset,
  };
}