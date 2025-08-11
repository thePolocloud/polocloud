"use client";

import { useEffect, useState } from 'react';
import { X, CheckCircle, AlertCircle, Info, AlertTriangle } from 'lucide-react';
import { cn } from '@/lib/utils';

export type ToastType = 'success' | 'error' | 'info' | 'warning';

interface Toast {
  id: string;
  message: string;
  type: ToastType;
  duration?: number;
}

interface ToastProps extends Toast {
  onRemove: (id: string) => void;
}

function ToastComponent({ id, message, type, duration = 5000, onRemove }: ToastProps) {
  useEffect(() => {
    const timer = setTimeout(() => {
      onRemove(id);
    }, duration);

    return () => clearTimeout(timer);
  }, [id, duration, onRemove]);

  const icons = {
    success: CheckCircle,
    error: AlertCircle,
    info: Info,
    warning: AlertTriangle,
  };

  const styles = {
    success: 'bg-green-500/10 border-green-500/30 text-green-600',
    error: 'bg-red-500/10 border-red-500/30 text-red-600',
    info: 'bg-blue-500/10 border-blue-500/30 text-blue-600',
    warning: 'bg-amber-500/10 border-amber-500/30 text-amber-600',
  };

  const Icon = icons[type];

  return (
    <div className={cn(
      'flex items-start gap-3 p-4 rounded-lg border backdrop-blur-sm shadow-lg max-w-md w-full',
      'transform transition-all duration-300 ease-in-out',
      'animate-in slide-in-from-right-full',
      styles[type]
    )}>
      <Icon className="w-5 h-5 mt-0.5 flex-shrink-0" />
      <div className="flex-1 text-sm font-medium">{message}</div>
      <button
        onClick={() => onRemove(id)}
        className="flex-shrink-0 p-1 rounded-md hover:bg-white/10 transition-colors"
      >
        <X className="w-4 h-4" />
      </button>
    </div>
  );
}

export function ToastContainer() {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const addToast = (toast: Omit<Toast, 'id'>) => {
    const id = Math.random().toString(36).substr(2, 9);
    setToasts(prev => [...prev, { ...toast, id }]);
  };

  const removeToast = (id: string) => {
    setToasts(prev => prev.filter(toast => toast.id !== id));
  };

  useEffect(() => {
    (window as any).showToast = addToast;
    return () => {
      delete (window as any).showToast;
    };
  }, []);

  return (
    <div className="fixed top-4 right-4 z-50 flex flex-col gap-2">
      {toasts.map((toast) => (
        <ToastComponent
          key={toast.id}
          {...toast}
          onRemove={removeToast}
        />
      ))}
    </div>
  );
}

export function showToast(message: string, type: ToastType = 'info', duration?: number) {
  if (typeof window !== 'undefined' && (window as any).showToast) {
    (window as any).showToast({ message, type, duration });
  }
}
