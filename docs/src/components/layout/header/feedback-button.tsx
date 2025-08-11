'use client';

import { MessageSquare } from 'lucide-react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';

export function FeedbackButton() {
  const pathname = usePathname();
  const isActive = pathname === '/feedback';


  if (pathname === '/feedback' || pathname.startsWith('/docs')) {
    return null;
  }

  return (
    <Link
      href="/feedback"
      className={`flex items-center gap-2 px-4 py-2 text-sm font-medium transition-all duration-300 hover:scale-105 rounded-lg backdrop-blur-sm border group ${
        isActive
          ? 'text-primary bg-primary/10 border-primary/30 shadow-lg'
          : 'text-muted-foreground hover:text-foreground hover:bg-card/50 border-transparent hover:border-border/30'
      }`}
    >
      <MessageSquare className="w-4 h-4 group-hover:scale-110 transition-transform duration-300" />
      <span>Feedback</span>
    </Link>
  );
}
