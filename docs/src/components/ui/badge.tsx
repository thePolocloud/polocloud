'use client';

import { forwardRef } from 'react';

interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  variant?: 'default' | 'secondary' | 'outline';
  children: React.ReactNode;
}

export const Badge = forwardRef<HTMLSpanElement, BadgeProps>(
  ({ className = '', variant = 'default', children, ...props }, ref) => {
    const baseClasses = 'inline-flex items-center px-2 py-1 text-xs font-medium rounded transition-all duration-300';
    
    const variantClasses = {
      default: 'bg-primary/20 border border-primary/30 text-primary-foreground',
      secondary: 'bg-secondary/20 border border-secondary/30 text-secondary-foreground',
      outline: 'border border-border/30 text-foreground/70',
    };

    const combinedClasses = `${baseClasses} ${variantClasses[variant]} ${className}`;

    return (
      <span
        ref={ref}
        className={combinedClasses}
        {...props}
      >
        {children}
      </span>
    );
  }
);

Badge.displayName = 'Badge';
