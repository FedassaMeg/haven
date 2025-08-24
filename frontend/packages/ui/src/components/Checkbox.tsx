"use client"

import * as React from "react"
import * as CheckboxPrimitive from "@radix-ui/react-checkbox"
import { Check } from "lucide-react"

import { cn } from "../lib/utils"

const Checkbox = React.forwardRef<
  React.ElementRef<typeof CheckboxPrimitive.Root>,
  React.ComponentPropsWithoutRef<typeof CheckboxPrimitive.Root>
>(({ className, ...props }, ref) => (
  <CheckboxPrimitive.Root
    ref={ref}
    className={cn(
      "peer h-4 w-4 shrink-0 rounded-sm border border-primary ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 data-[state=checked]:bg-primary data-[state=checked]:text-primary-foreground",
      className
    )}
    {...props}
  >
    <CheckboxPrimitive.Indicator
      className={cn("flex items-center justify-center text-current")}
    >
      <Check className="h-4 w-4" />
    </CheckboxPrimitive.Indicator>
  </CheckboxPrimitive.Root>
))
Checkbox.displayName = CheckboxPrimitive.Root.displayName

// Enhanced Checkbox with label and helper text for forms
interface FormCheckboxProps extends React.ComponentPropsWithoutRef<typeof CheckboxPrimitive.Root> {
  label?: string;
  helperText?: string;
  error?: string;
}

const FormCheckbox = React.forwardRef<
  React.ElementRef<typeof CheckboxPrimitive.Root>,
  FormCheckboxProps
>(({ className, label, helperText, error, id, checked, onCheckedChange, ...props }, ref) => {
  const checkboxId = id || `checkbox-${Math.random().toString(36).substr(2, 9)}`;
  
  return (
    <div className={cn("items-top flex space-x-2", className)}>
      <Checkbox
        id={checkboxId}
        ref={ref}
        checked={checked}
        onCheckedChange={onCheckedChange}
        className={cn(
          error && "border-destructive"
        )}
        aria-invalid={error ? 'true' : 'false'}
        aria-describedby={
          error ? `${checkboxId}-error` : helperText ? `${checkboxId}-helper` : undefined
        }
        {...props}
      />
      <div className="grid gap-1.5 leading-none">
        {label && (
          <label
            htmlFor={checkboxId}
            className={cn(
              "text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70",
              error && "text-destructive"
            )}
          >
            {label}
          </label>
        )}
        {helperText && !error && (
          <p id={`${checkboxId}-helper`} className="text-xs text-muted-foreground">
            {helperText}
          </p>
        )}
        {error && (
          <p id={`${checkboxId}-error`} className="text-xs text-destructive">
            {error}
          </p>
        )}
      </div>
    </div>
  )
})
FormCheckbox.displayName = "FormCheckbox"

export { Checkbox, FormCheckbox }
export type { FormCheckboxProps }