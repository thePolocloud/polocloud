import Link from 'next/link';
import { ArrowLeft, Search } from 'lucide-react';
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
import { MobileNav } from '@/components/layout/header/mobile-nav';
import { Footer } from './(home)/components/footer';

function NotFoundNavbar() {
  return (
    <nav className="sticky top-0 z-50 w-full border-b border-border/40 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 shadow-sm">
      <div className="container flex h-16 max-w-screen-2xl items-center px-4">
        <div className="flex items-center w-32">
          <LogoWithLink />
        </div>
        <div className="hidden lg:flex flex-1 items-center justify-center space-x-3">
          <HomeDropdown />
          <DocsButton />
          <RoadmapButton />
          <ChangelogButton />
          <BlogButton />
          <FeedbackButton />
        </div>
        <div className="hidden lg:flex items-center space-x-2 w-32 justify-end">
          <DownloadButton />
          <SponsorsButton />
          <DashboardButton />
        </div>
        <div className="lg:hidden flex-1 flex justify-end">
          <MobileNav />
        </div>
      </div>
    </nav>
  );
}

export default function NotFound() {
  return (
    <div className="min-h-screen flex flex-col bg-background">
      <NotFoundNavbar />
      <main className="flex flex-col flex-1 items-center justify-center gap-10 px-6 py-24 md:py-32 text-center relative overflow-hidden scroll-optimized">
        
        <div className="pointer-events-none absolute inset-0 -z-10">
          <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_40%,rgba(0,120,255,0.15),transparent_70%)]" />
          <div className="absolute inset-0 bg-[linear-gradient(135deg,rgba(0,120,255,0.08),transparent)]" />
        </div>

        <div className="space-y-6 max-w-xl">
          <span className="inline-flex items-center rounded-full border border-border/60 bg-card/60 backdrop-blur px-4 py-1 text-xs font-medium tracking-wide text-muted-foreground shadow-sm">
            <span className="mr-2 inline-block h-2 w-2 rounded-full bg-primary animate-pulse" />
            404 — Page not found
          </span>

          <h1 className="text-4xl md:text-5xl font-bold bg-gradient-to-br from-primary to-primary/60 bg-clip-text text-transparent leading-tight">
            This page doesn&apos;t exist (anymore)
          </h1>

          <p className="text-muted-foreground text-base md:text-lg leading-relaxed">
            The page you&apos;re looking for was moved, deleted, never created or you mistyped the URL. Check the homepage or dive into the documentation to continue exploring PoloCloud.
          </p>

          <div className="flex flex-col sm:flex-row gap-3 justify-center pt-2">
            <Link
              href="/"
              className="inline-flex items-center justify-center gap-2 rounded-md bg-primary text-primary-foreground px-6 py-3 text-sm font-medium shadow hover:bg-primary/90 transition-colors focus:outline-none focus:ring-2 focus:ring-primary/50"
            >
              <ArrowLeft className="h-4 w-4" />
              Back to homepage
            </Link>
            <Link
              href="/docs/cloud"
              className="inline-flex items-center justify-center gap-2 rounded-md border border-border bg-card/70 backdrop-blur px-6 py-3 text-sm font-medium text-foreground shadow-sm hover:bg-muted/60 transition-colors focus:outline-none focus:ring-2 focus:ring-primary/40"
            >
              <Search className="h-4 w-4" />
              Browse documentation
            </Link>
          </div>
        </div>

        <div className="mt-8 text-xs text-muted-foreground/70">
          Error code: <code className="px-1.5 py-0.5 rounded bg-muted text-foreground text-[11px]">404_NOT_FOUND</code>
        </div>
      </main>
      <Footer />
    </div>
  );
}
