'use client';

import { Copy, Check } from 'lucide-react';
import { useState } from 'react';

interface CopyButtonProps {
  text: string;
  className?: string;
  title?: string;
}

export function CopyButton({ text, className = "", title = "Copy" }: CopyButtonProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Failed to copy text: ', err);
    }
  };

  return (
    <button
      onClick={handleCopy}
      className={`ml-4 p-2 rounded-md hover:bg-background/50 transition-colors ${
        copied ? 'text-green-500' : 'text-muted-foreground hover:text-foreground'
      } ${className}`}
      title={title}
    >
      {copied ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
    </button>
  );
} 