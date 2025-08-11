import type { ReactNode } from 'react';
import { DownloadButton } from '@/components/layout/header/download-button';
import { SponsorsButton } from '@/components/layout/header/sponsors-button';
import { HomeDropdown } from '@/components/layout/header/home-dropdown';
import { DocsButton } from '@/components/layout/header/docs-button';
import { RoadmapButton } from '@/components/layout/header/roadmap-button';
import { ChangelogButton } from '@/components/layout/header/changelog-button';
import { BlogButton } from '@/components/layout/header/blog-button';
import { FeedbackButton } from '@/components/layout/header/feedback-button';
import { DashboardButton } from '@/components/layout/header/dashboard-button';
import { LogoWithLink } from '@/components/layout/header/logo';
import { Footer } from '../(home)/components/footer';

function CustomNavbar() {
  return (
    <nav className="sticky top-0 z-50 w-full border-b border-border/40 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 shadow-sm">
      <div className="container flex h-16 max-w-screen-2xl items-center px-4">
        <div className="flex items-center w-32">
          <LogoWithLink />
        </div>
        <div className="flex flex-1 items-center justify-center space-x-3">
          <HomeDropdown />
          <DocsButton />
          <RoadmapButton />
          <ChangelogButton />
          <BlogButton />
          <FeedbackButton />
        </div>

        <div className="flex items-center space-x-2 w-32 justify-end">
          <DownloadButton />
          <SponsorsButton />
          <DashboardButton />
        </div>
      </div>
    </nav>
  );
}

export default function PagesLayout({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen bg-background">
      <CustomNavbar />
      <main className="flex-1">{children}</main>
      <Footer />
    </div>
  );
} 