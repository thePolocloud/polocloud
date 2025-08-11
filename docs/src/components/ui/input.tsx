'use client';

import { forwardRef } from 'react';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className = '', ...props }, ref) => {
    const baseClasses = 'w-full px-3 py-2 border border-border/50 rounded-lg bg-background/50 text-foreground placeholder:text-foreground/50 focus:outline-none focus:ring-2 focus:ring-blue-500/50 transition-all duration-300';

    return (
      <input
        ref={ref}
        className={`${baseClasses} ${className}`}
        {...props}
      />
    );
  }
);

Input.displayName = 'Input';
