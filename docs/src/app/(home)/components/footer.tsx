import Link from 'next/link';
import Image from 'next/image';
import { Github, BookOpen, ExternalLink, FileText, Shield, Mail, Info, Map, Newspaper, Scale, Code } from 'lucide-react';

export function Footer() {
  return (
    <footer className="bg-card/30 backdrop-blur-sm border-t border-border/50 dark:bg-black/40 dark:border-white/10 mt-32">
      <div className="container mx-auto px-6 py-12">
        <div className="flex flex-col md:flex-row justify-between items-start gap-8">
          <div className="flex flex-col items-start gap-3">
            <div className="flex items-center gap-3">
              <Image
                src="/logo.png"
                alt="PoloCloud Logo"
                width={32}
                height={32}
                className="w-8 h-8"
              />
              <span className="font-minecraft font-bold text-foreground dark:text-white text-lg">POLOCLOUD</span>
            </div>
            <div className="flex items-center gap-4">
              <a 
                href="https://discord.com/invite/mQ39S2EWNV"
                target="_blank" 
                rel="noopener noreferrer"
                className="text-muted-foreground dark:text-white/60 hover:text-foreground dark:hover:text-white transition-all duration-300 hover:scale-105 transform p-2 rounded-lg border border-border/30 dark:border-white/10 hover:border-border/50 dark:hover:border-white/20"
                title="Discord"
              >
                <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M20.317 4.3698a19.7913 19.7913 0 00-4.8851-1.5152.0741.0741 0 00-.0785.0371c-.211.3753-.4447.8648-.6083 1.2495-1.8447-.2762-3.68-.2762-5.4868 0-.1636-.3933-.4058-.8742-.6177-1.2495a.077.077 0 00-.0785-.037 19.7363 19.7363 0 00-4.8852 1.515.0699.0699 0 00-.0321.0277C.5334 9.0458-.319 13.5799.0992 18.0578a.0824.0824 0 00.0312.0561c2.0528 1.5076 4.0413 2.4228 5.9929 3.0294a.0777.0777 0 00.0842-.0276c.4616-.6304.8731-1.2952 1.226-1.9942a.076.076 0 00-.0416-.1057c-.6528-.2476-1.2743-.5495-1.8722-.8923a.077.077 0 01-.0076-.1277c.1258-.0943.2517-.1923.3718-.2914a.0743.0743 0 01.0776-.0105c3.9278 1.7933 8.18 1.7933 12.0614 0a.0739.0739 0 01.0785.0095c.1202.099.246.1981.3728.2924a.077.077 0 01-.0066.1276 12.2986 12.2986 0 01-1.873.8914.0766.0766 0 00-.0407.1067c.3604.698.7719 1.3628 1.225 1.9932a.076.076 0 00.0842.0286c1.961-.6067 3.9495-1.5219 6.0023-3.0294a.077.077 0 00.0313-.0552c.5004-5.177-.8382-9.6739-3.5485-13.6604a.061.061 0 00-.0312-.0286zM8.02 15.3312c-1.1825 0-2.1569-1.0857-2.1569-2.419 0-1.3332.9555-2.4189 2.157-2.4189 1.2108 0 2.1757 1.0952 2.1568 2.419-.019 1.3332-.9555 2.4189-2.1569 2.4189zm7.9748 0c-1.1825 0-2.1569-1.0857-2.1569-2.419 0-1.3332.9554-2.4189 2.1569-2.4189 1.2108 0 2.1757 1.0952 2.1568 2.419 0 1.3332-.9555 2.4189-2.1568 2.4189Z"/>
                </svg>
              </a>
              <a 
                href="https://github.com/HttpMarco/polocloud" 
                target="_blank" 
                rel="noopener noreferrer"
                className="text-muted-foreground dark:text-white/60 hover:text-foreground dark:hover:text-white transition-all duration-300 hover:scale-105 transform p-2 rounded-lg border border-border/30 dark:border-white/10 hover:border-border/50 dark:hover:border-white/20"
                title="GitHub"
              >
                <Github className="w-5 h-5" />
              </a>
              <Link 
                href="/docs/cloud" 
                className="text-muted-foreground dark:text-white/60 hover:text-foreground dark:hover:text-white transition-all duration-300 hover:scale-105 transform p-2 rounded-lg border border-border/30 dark:border-white/10 hover:border-border/50 dark:hover:border-white/20"
                title="Documentation"
              >
                <BookOpen className="w-5 h-5" />
              </Link>
            </div>
          </div>

          <div className="flex flex-col gap-4">
            <h3 className="font-minecraft font-bold text-foreground dark:text-white text-lg flex items-center gap-2">
              <Scale className="w-5 h-5 text-primary" />
              Legal
            </h3>
            <div className="flex flex-col gap-2">
              <Link 
                href="/imprint" 
                className="text-sm text-muted-foreground dark:text-white/60 hover:text-foreground dark:hover:text-white transition-all duration-300 hover:scale-105 font-minecraft flex items-center gap-2"
              >
                <FileText className="w-4 h-4" />
                Imprint
              </Link>
              <Link 
                href="/privacy" 
                className="text-sm text-muted-foreground dark:text-white/60 hover:text-foreground dark:hover:text-white transition-all duration-300 hover:scale-105 font-minecraft flex items-center gap-2"
              >
                <Shield className="w-4 h-4" />
                Privacy Policy
              </Link>
              <Link 
                href="/terms" 
                className="text-sm text-muted-foreground dark:text-white/60 hover:text-foreground dark:hover:text-white transition-all duration-300 hover:scale-105 font-minecraft flex items-center gap-2"
              >
                <FileText className="w-4 h-4" />
                Terms of Service
              </Link>
            </div>
          </div>

          <div className="flex flex-col gap-4">
            <h3 className="font-minecraft font-bold text-foreground dark:text-white text-lg flex items-center gap-2">
              <Code className="w-5 h-5 text-primary" />
              Documentation
            </h3>
            <div className="flex flex-col gap-2">
              <Link 
                href="/docs/cloud" 
                className="text-sm text-muted-foreground dark:text-white/60 hover:text-foreground dark:hover:text-white transition-all duration-300 hover:scale-105 font-minecraft flex items-center gap-2"
              >
                <BookOpen className="w-4 h-4" />
                Getting Started
              </Link>
              <Link 
                href="/docs/cloud/installation" 
                className="text-sm text-muted-foreground dark:text-white/60 hover:text-foreground dark:hover:text-white transition-all duration-300 hover:scale-105 font-minecraft flex items-center gap-2"
              >
                <Code className="w-4 h-4" />
                Installation
              </Link>
              <Link 
                href="/docs/cloud/configuration" 
                className="text-sm text-muted-foreground dark:text-white/60 hover:text-foreground dark:hover:text-white transition-all duration-300 hover:scale-105 font-minecraft flex items-center gap-2"
              >
                <FileText className="w-4 h-4" />
                Configuration
              </Link>
              <Link 
                href="/docs/cloud/api" 
                className="text-sm text-muted-foreground dark:text-white/60 hover:text-foreground dark:hover:text-white transition-all duration-300 hover:scale-105 font-minecraft flex items-center gap-2"
              >
                <ExternalLink className="w-4 h-4" />
                API Reference
              </Link>
            </div>
          </div>

          <div className="flex flex-col gap-4">
            <h3 className="font-minecraft font-bold text-foreground dark:text-white text-lg flex items-center gap-2">
              <Info className="w-5 h-5 text-primary" />
              More
            </h3>
            <div className="flex flex-col gap-2">
              <Link 
                href="/blog" 
                className="text-sm text-muted-foreground dark:text-white/60 hover:text-foreground dark:hover:text-white transition-all duration-300 hover:scale-105 font-minecraft flex items-center gap-2"
              >
                <Newspaper className="w-4 h-4" />
                Blog
              </Link>
              <Link 
                href="/changelogs" 
                className="text-sm text-muted-foreground dark:text-white/60 hover:text-foreground dark:hover:text-white transition-all duration-300 hover:scale-105 font-minecraft flex items-center gap-2"
              >
                <FileText className="w-4 h-4" />
                Changelogs
              </Link>
              <Link 
                href="/roadmap" 
                className="text-sm text-muted-foreground dark:text-white/60 hover:text-foreground dark:hover:text-white transition-all duration-300 hover:scale-105 font-minecraft flex items-center gap-2"
              >
                <Map className="w-4 h-4" />
                Roadmap
              </Link>
              <a 
                href="mailto:contact@polocloud.de" 
                className="text-sm text-muted-foreground dark:text-white/60 hover:text-foreground dark:hover:text-white transition-all duration-300 hover:scale-105 font-minecraft flex items-center gap-2"
              >
                <Mail className="w-4 h-4" />
                Contact
              </a>
            </div>
          </div>

          <div className="flex flex-col gap-4">
            <h3 className="font-minecraft font-bold text-foreground dark:text-white text-lg flex items-center gap-2">
              <Code className="w-5 h-5 text-primary" />
              Built with
            </h3>
          <div className="flex items-center gap-4">
            <a 
              href="https://nextjs.org" 
              target="_blank" 
              rel="noopener noreferrer"
              className="text-muted-foreground dark:text-white/60 hover:text-foreground dark:hover:text-white transition-all duration-300 hover:scale-105 transform p-2 rounded-lg border border-border/30 dark:border-white/10 hover:border-border/50 dark:hover:border-white/20"
              title="Next.js"
            >
              <svg className="w-5 h-5" viewBox="0 0 128 128" fill="currentColor">
                <path d="M64 0C28.7 0 0 28.7 0 64s28.7 64 64 64c11.2 0 21.7-2.9 30.8-7.9L48.4 55.3v36.6h-6.8V41.8h6.8l50.5 75.8C116.4 106.2 128 86.5 128 64c0-35.3-28.7-64-64-64zm22.1 84.6l-7.5-11.3V41.8h7.5v42.8z"/>
              </svg>
            </a>
            <a 
              href="https://tailwindcss.com" 
              target="_blank" 
              rel="noopener noreferrer"
              className="text-muted-foreground dark:text-white/60 hover:text-foreground dark:hover:text-white transition-all duration-300 hover:scale-105 transform p-2 rounded-lg border border-border/30 dark:border-white/10 hover:border-border/50 dark:hover:border-white/20"
              title="Tailwind CSS"
            >
              <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12.001,4.8c-3.2,0-5.2,1.6-6,4.8c1.2-1.6,2.6-2.2,4.2-1.8c0.913,0.228,1.565,0.89,2.288,1.624 C13.666,10.618,15.027,12,18.001,12c3.2,0,5.2-1.6,6-4.8c-1.2,1.6-2.6,2.2-4.2,1.8c-0.913-0.228-1.565-0.89-2.288-1.624 C16.337,6.182,14.976,4.8,12.001,4.8z M6.001,12c-3.2,0-5.2,1.6-6,4.8c1.2-1.6,2.6-2.2,4.2-1.8c0.913,0.228,1.565,0.89,2.288,1.624 c1.177,1.194,2.538,2.576,5.512,2.576c3.2,0,5.2-1.6,6-4.8c-1.2,1.6-2.6,2.2-4.2,1.8c-0.913-0.228-1.565-0.89-2.288-1.624 C10.337,13.382,8.976,12,6.001,12z"/>
              </svg>
            </a>
            <a 
              href="https://fumadocs.dev" 
              target="_blank" 
              rel="noopener noreferrer"
              className="text-muted-foreground dark:text-white/60 hover:text-foreground dark:hover:text-white transition-all duration-300 hover:scale-105 transform p-2 rounded-lg border border-border/30 dark:border-white/10 hover:border-border/50 dark:hover:border-white/20"
              title="Fumadocs"
            >
              <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
              </svg>
            </a>
            </div>
          </div>
        </div>

        <div className="mt-8 pt-8 border-t border-border/30 dark:border-white/10">
          <div className="text-center">
            <div className="flex items-center justify-center gap-2 text-muted-foreground dark:text-white/60 font-minecraft mb-2">
              <span>Made with</span>
              <svg className="w-4 h-4 text-red-500 animate-pulse" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z" clipRule="evenodd" />
              </svg>
              <span>by</span>
              <a 
                href="https://github.com/jakubbbdev" 
                target="_blank" 
                rel="noopener noreferrer"
                className="text-foreground dark:text-white hover:text-blue-500 dark:hover:text-blue-400 transition-all duration-300 font-medium hover:scale-105 transform"
              >
                jakubbbdev
              </a>
            </div>
            <p className="text-sm text-muted-foreground dark:text-white/50 font-minecraft">
              &copy; 2025 PoloCloud. All rights reserved.
            </p>
          </div>
        </div>

        <div className="mt-6 pt-6">
          <div className="text-center">
            <p className="text-xs text-muted-foreground/70 dark:text-white/40 font-minecraft">
              NOT AN OFFICIAL MINECRAFT SERVICE. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.
            </p>
          </div>
        </div>
      </div>
    </footer>
  );
} 