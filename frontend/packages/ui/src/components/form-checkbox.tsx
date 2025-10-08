import * as React from "react"
import { Checkbox } from "./checkbox"
import { Label } from "./label"
import { cn } from "../lib"

interface FormCheckboxProps {
  id?: string
  label?: string
  checked: boolean
  onCheckedChange: (checked: boolean) => void
  disabled?: boolean
  error?: string
  helperText?: string
  className?: string
}

function FormCheckbox({
  id,
  label,
  checked,
  onCheckedChange,
  disabled = false,
  error,
  helperText,
  className,
}: FormCheckboxProps) {
  const checkboxId = id || React.useId()

  return (
    <div className={cn("space-y-2", className)}>
      <div className="flex items-center space-x-2">
        <Checkbox
          id={checkboxId}
          checked={checked}
          onCheckedChange={onCheckedChange}
          disabled={disabled}
          aria-invalid={!!error}
        />
        {label && (
          <Label 
            htmlFor={checkboxId} 
            className={cn(
              "text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70",
              error && "text-destructive"
            )}
          >
            {label}
          </Label>
        )}
      </div>
      {error && (
        <p className="text-sm text-destructive">{error}</p>
      )}
      {helperText && !error && (
        <p className="text-sm text-muted-foreground">{helperText}</p>
      )}
    </div>
  )
}

export { FormCheckbox }