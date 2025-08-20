import React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';

const cardVariants = cva(
  'card',
  {
    variants: {},
    defaultVariants: {},
  }
);

export interface CardProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof cardVariants> {}

export const Card = React.forwardRef<HTMLDivElement, CardProps>(
  ({ className, ...props }, ref) => (
    <div
      ref={ref}
      className={cardVariants({ className })}
      {...props}
    />
  )
);

Card.displayName = 'Card';

const cardHeaderVariants = cva(
  'card-header',
  {
    variants: {},
    defaultVariants: {},
  }
);

export interface CardHeaderProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof cardHeaderVariants> {}

export const CardHeader = React.forwardRef<HTMLDivElement, CardHeaderProps>(
  ({ className, ...props }, ref) => (
    <div
      ref={ref}
      className={cardHeaderVariants({ className })}
      {...props}
    />
  )
);

CardHeader.displayName = 'CardHeader';

const cardTitleVariants = cva(
  'text-lg font-semibold leading-none tracking-tight',
  {
    variants: {},
    defaultVariants: {},
  }
);

export interface CardTitleProps
  extends React.HTMLAttributes<HTMLHeadingElement>,
    VariantProps<typeof cardTitleVariants> {}

export const CardTitle = React.forwardRef<
  HTMLParagraphElement,
  CardTitleProps
>(({ className, ...props }, ref) => (
  <h3
    ref={ref}
    className={cardTitleVariants({ className })}
    {...props}
  />
));

CardTitle.displayName = 'CardTitle';

const cardDescriptionVariants = cva(
  'text-sm text-secondary-500 mt-1',
  {
    variants: {},
    defaultVariants: {},
  }
);

export interface CardDescriptionProps
  extends React.HTMLAttributes<HTMLParagraphElement>,
    VariantProps<typeof cardDescriptionVariants> {}

export const CardDescription = React.forwardRef<
  HTMLParagraphElement,
  CardDescriptionProps
>(({ className, ...props }, ref) => (
  <p
    ref={ref}
    className={cardDescriptionVariants({ className })}
    {...props}
  />
));

CardDescription.displayName = 'CardDescription';

const cardContentVariants = cva(
  'card-body',
  {
    variants: {},
    defaultVariants: {},
  }
);

export interface CardContentProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof cardContentVariants> {}

export const CardContent = React.forwardRef<HTMLDivElement, CardContentProps>(
  ({ className, ...props }, ref) => (
    <div ref={ref} className={cardContentVariants({ className })} {...props} />
  )
);

CardContent.displayName = 'CardContent';

const cardFooterVariants = cva(
  'px-6 py-3 border-t border-secondary-200 bg-secondary-50',
  {
    variants: {},
    defaultVariants: {},
  }
);

export interface CardFooterProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof cardFooterVariants> {}

export const CardFooter = React.forwardRef<HTMLDivElement, CardFooterProps>(
  ({ className, ...props }, ref) => (
    <div
      ref={ref}
      className={cardFooterVariants({ className })}
      {...props}
    />
  )
);

CardFooter.displayName = 'CardFooter';