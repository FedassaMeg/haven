import * as React from "react"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "./select"
import { Label } from "./label"
import { cn } from "../lib"

export interface SelectOption {
  value: string
  label: string
}

interface FormSelectProps {
  id?: string
  label?: string
  value: string
  onChange: (value: string) => void
  options: SelectOption[]
  placeholder?: string
  required?: boolean
  disabled?: boolean
  error?: string
  helperText?: string
  className?: string
}

function FormSelect({
  id,
  label,
  value,
  onChange,
  options,
  placeholder = "Select an option...",
  required = false,
  disabled = false,
  error,
  helperText,
  className,
}: FormSelectProps) {
  const selectId = id || React.useId()

  return (
    <div className={cn("space-y-2", className)}>
      {label && (
        <Label htmlFor={selectId} className={cn(error && "text-destructive")}>
          {label} {required && <span className="text-destructive">*</span>}
        </Label>
      )}
      <Select value={value} onValueChange={onChange} disabled={disabled}>
        <SelectTrigger
          id={selectId}
          className={cn(
            "w-full",
            error && "border-destructive focus-visible:ring-destructive/20"
          )}
          aria-invalid={!!error}
        >
          <SelectValue placeholder={placeholder} />
        </SelectTrigger>
        <SelectContent>
          {(options || []).filter(option => option.value !== '').map((option) => (
            <SelectItem key={option.value} value={option.value}>
              {option.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      {error && (
        <p className="text-sm text-destructive">{error}</p>
      )}
      {helperText && !error && (
        <p className="text-sm text-muted-foreground">{helperText}</p>
      )}
    </div>
  )
}

export { FormSelect }