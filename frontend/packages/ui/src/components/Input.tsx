import React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';

const inputVariants = cva(
  'block w-full px-4 py-2.5 text-sm text-secondary-900 bg-white border rounded-lg shadow-sm transition-all duration-200 placeholder:text-secondary-400 focus:outline-none focus:ring-2 focus:ring-offset-0 disabled:opacity-50 disabled:cursor-not-allowed',
  {
    variants: {
      error: {
        true: 'border-error-500 focus:border-error-500 focus:ring-error-500/20',
        false: 'border-secondary-200 hover:border-secondary-300 focus:border-primary-500 focus:ring-primary-500/20',
      },
    },
    defaultVariants: {
      error: false,
    },
  }
);

export interface InputProps
  extends React.InputHTMLAttributes<HTMLInputElement>,
    Omit<VariantProps<typeof inputVariants>, 'error'> {
  label?: string;
  error?: string;
  helperText?: string;
}

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, label, error, helperText, id, ...props }, ref) => {
    const inputId = id || `input-${Math.random().toString(36).substr(2, 9)}`;

    return (
      <div className="space-y-1.5">
        {label && (
          <label htmlFor={inputId} className="block text-sm font-medium text-secondary-700">
            {label}
            {props.required && <span className="ml-0.5 text-error-500">*</span>}
          </label>
        )}
        <div className="relative">
          <input
            id={inputId}
            className={inputVariants({ error: !!error, className })}
            ref={ref}
            {...props}
          />
        </div>
        {error && (
          <p className="flex items-center gap-1 text-xs text-error-600 mt-1" role="alert">
            <svg className="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
            </svg>
            {error}
          </p>
        )}
        {helperText && !error && (
          <p className="text-xs text-secondary-500 mt-1">
            {helperText}
          </p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';

const textareaVariants = cva(
  'block w-full px-4 py-2.5 text-sm text-secondary-900 bg-white border rounded-lg shadow-sm transition-all duration-200 placeholder:text-secondary-400 focus:outline-none focus:ring-2 focus:ring-offset-0 disabled:opacity-50 disabled:cursor-not-allowed min-h-[80px] resize-y',
  {
    variants: {
      error: {
        true: 'border-error-500 focus:border-error-500 focus:ring-error-500/20',
        false: 'border-secondary-200 hover:border-secondary-300 focus:border-primary-500 focus:ring-primary-500/20',
      },
    },
    defaultVariants: {
      error: false,
    },
  }
);

export interface TextareaProps
  extends React.TextareaHTMLAttributes<HTMLTextAreaElement>,
    Omit<VariantProps<typeof textareaVariants>, 'error'> {
  label?: string;
  error?: string;
  helperText?: string;
}

export const Textarea = React.forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ className, label, error, helperText, id, ...props }, ref) => {
    const inputId = id || `textarea-${Math.random().toString(36).substr(2, 9)}`;

    return (
      <div className="space-y-1.5">
        {label && (
          <label htmlFor={inputId} className="block text-sm font-medium text-secondary-700">
            {label}
            {props.required && <span className="ml-0.5 text-error-500">*</span>}
          </label>
        )}
        <textarea
          id={inputId}
          className={textareaVariants({ error: !!error, className })}
          ref={ref}
          {...props}
        />
        {error && (
          <p className="flex items-center gap-1 text-xs text-error-600 mt-1" role="alert">
            <svg className="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
            </svg>
            {error}
          </p>
        )}
        {helperText && !error && (
          <p className="text-xs text-secondary-500 mt-1">
            {helperText}
          </p>
        )}
      </div>
    );
  }
);

Textarea.displayName = 'Textarea';

const selectVariants = cva(
  'block w-full px-4 py-2.5 pr-10 text-sm text-secondary-900 bg-white border rounded-lg shadow-sm transition-all duration-200 appearance-none cursor-pointer focus:outline-none focus:ring-2 focus:ring-offset-0 disabled:opacity-50 disabled:cursor-not-allowed',
  {
    variants: {
      error: {
        true: 'border-error-500 focus:border-error-500 focus:ring-error-500/20',
        false: 'border-secondary-200 hover:border-secondary-300 focus:border-primary-500 focus:ring-primary-500/20',
      },
    },
    defaultVariants: {
      error: false,
    },
  }
);

export interface SelectProps
  extends Omit<React.SelectHTMLAttributes<HTMLSelectElement>, 'onChange'>,
    Omit<VariantProps<typeof selectVariants>, 'error'> {
  label?: string;
  error?: string;
  helperText?: string;
  options: Array<{ value: string; label: string; disabled?: boolean }>;
  onChange?: (value: string) => void;
}

export const Select = React.forwardRef<HTMLSelectElement, SelectProps>(
  ({ className, label, error, helperText, options, id, onChange, ...props }, ref) => {
    const selectId = id || `select-${Math.random().toString(36).substr(2, 9)}`;

    const handleChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
      if (onChange) {
        onChange(e.target.value);
      }
    };

    return (
      <div className="space-y-1.5">
        {label && (
          <label htmlFor={selectId} className="block text-sm font-medium text-secondary-700">
            {label}
            {props.required && <span className="ml-0.5 text-error-500">*</span>}
          </label>
        )}
        <div className="relative">
          <select
            id={selectId}
            className={selectVariants({ error: !!error, className })}
            ref={ref}
            onChange={handleChange}
            {...props}
          >
            {options.map((option) => (
              <option 
                key={option.value} 
                value={option.value}
                disabled={option.disabled}
              >
                {option.label}
              </option>
            ))}
          </select>
          <div className="absolute inset-y-0 right-0 flex items-center pr-3 pointer-events-none">
            <svg className="w-4 h-4 text-secondary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
            </svg>
          </div>
        </div>
        {error && (
          <p className="flex items-center gap-1 text-xs text-error-600 mt-1" role="alert">
            <svg className="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
            </svg>
            {error}
          </p>
        )}
        {helperText && !error && (
          <p className="text-xs text-secondary-500 mt-1">
            {helperText}
          </p>
        )}
      </div>
    );
  }
);

Select.displayName = 'Select';