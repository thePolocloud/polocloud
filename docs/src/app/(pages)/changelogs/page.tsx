import { readdirSync, readFileSync } from 'fs';
import { join } from 'path';
import matter from 'gray-matter';
import Link from 'next/link';
import { Calendar, Tag, GitBranch, ArrowRight, Download, Star, MessageCircle } from 'lucide-react';

interface ChangelogPost {
  title: string;
  description?: string;
  date?: string;
  version?: string;
  type?: 'major' | 'minor' | 'patch';
  author?: string;
  tags?: string[];
  slug: string;
}

function getChangelogPosts(): ChangelogPost[] {
  try {
    const changelogDir = join(process.cwd(), 'content', 'changelogs');
    const files = readdirSync(changelogDir).filter(file => file.endsWith('.mdx'));
    
    const posts = files.map(file => {
      const filePath = join(changelogDir, file);
      const fileContent = readFileSync(filePath, 'utf8');
      const { data } = matter(fileContent);
      const slug = file.replace('.mdx', '');
      
      return {
        title: data.title || 'Untitled',
        description: data.description,
        date: data.date,
        version: data.version,
        type: data.type || 'patch',
        author: data.author,
        tags: data.tags || [],
        slug,
      };
    });

    return posts.sort((a, b) => {
      if (!a.date || !b.date) return 0;
      return new Date(b.date).getTime() - new Date(a.date).getTime();
    });
  } catch (error) {
    console.error('Error reading changelog posts:', error);
    return [];
  }
}

function getTypeColor(type: string) {
  switch (type) {
    case 'major':
      return 'bg-red-500/10 text-red-500 border-red-500/20';
    case 'minor':
      return 'bg-blue-500/10 text-blue-500 border-blue-500/20';
    case 'patch':
      return 'bg-green-500/10 text-green-500 border-green-500/20';
    default:
      return 'bg-gray-500/10 text-gray-500 border-gray-500/20';
  }
}

function getTypeIcon(type: string) {
  switch (type) {
    case 'major':
      return '🚀';
    case 'minor':
      return '✨';
    case 'patch':
      return '🐛';
    default:
      return '📝';
  }
}

export default function ChangelogsPage() {
  const posts = getChangelogPosts();

  return (
    <div className="relative min-h-screen">
      <div className="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.02)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.02)_1px,transparent_1px)] bg-[size:50px_50px] dark:bg-[linear-gradient(rgba(255,255,255,0.01)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.01)_1px,transparent_1px)]" />
      
      <div className="container mx-auto px-6 py-12 max-w-6xl relative z-10">
        <div className="text-center mb-16">
          <div className="flex items-center justify-center gap-3 mb-6">
            <div className="flex items-center justify-center w-10 h-10 bg-primary/10 text-primary rounded-full border border-primary/20">
              <GitBranch className="w-5 h-5" />
            </div>
          </div>
          
          <h1 className="text-3xl md:text-4xl lg:text-5xl font-black bg-gradient-to-r from-foreground via-primary to-foreground bg-clip-text text-transparent mb-6 leading-tight">
            Release Notes
          </h1>
          
          <p className="text-base md:text-lg text-muted-foreground max-w-3xl mx-auto leading-relaxed">
            Stay updated with the latest features, improvements, and bug fixes in PoloCloud. 
            Track our development progress and see what&apos;s new in each release.
          </p>
        </div>

        <div className="space-y-4">
          {posts.length === 0 ? (
            <div className="text-center py-12">
              <GitBranch className="w-16 h-16 text-muted-foreground mx-auto mb-4" />
              <h3 className="text-xl font-semibold text-foreground mb-2">No changelogs yet</h3>
              <p className="text-muted-foreground">We&apos;ll post release notes here as we ship new versions.</p>
            </div>
          ) : (
            posts.map((post) => (
              <article
                key={post.slug}
                className="group bg-card/30 backdrop-blur-sm border border-border/50 rounded-lg p-4 hover:bg-card/40 hover:border-border/70 transition-all duration-300 relative"
              >
                <div className="absolute top-3 right-3 flex flex-col gap-2 items-end">
                  <span className={`inline-flex items-center gap-1 px-1.5 py-0.5 rounded-full text-xs font-medium border ${getTypeColor(post.type || 'patch')}`}>
                    {post.type?.toUpperCase() || 'PATCH'}
                  </span>

                  {post.tags && post.tags.length > 0 && (
                    <div className="flex flex-wrap gap-1 justify-end">
                      {post.tags.map((tag: string) => (
                        <span
                          key={tag}
                          className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded-full text-xs font-medium bg-muted/50 text-muted-foreground border border-border/50"
                        >
                          <Tag className="w-2.5 h-2.5" />
                          {tag}
                        </span>
                      ))}
                    </div>
                  )}
                </div>

                <div className="flex flex-col lg:flex-row lg:items-start gap-4">
                  <div className="flex-shrink-0">
                    <div className="flex items-center gap-2 mb-2">
                      <span className="text-lg">{getTypeIcon(post.type || 'patch')}</span>
                      <div className="flex flex-col">
                        <span className="text-xs font-mono text-muted-foreground">
                          v{post.version || '0.0.0'}
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-3 text-xs text-muted-foreground mb-3">
                      {post.date && (
                        <div className="flex items-center gap-1 bg-muted/50 px-2 py-1 rounded-full">
                          <Calendar className="w-3 h-3" />
                          <span>{new Date(post.date).toLocaleDateString('de-DE', {
                            year: 'numeric',
                            month: 'long',
                            day: 'numeric'
                          })}</span>
                        </div>
                      )}
                      {post.author && (
                        <div className="flex items-center gap-1 bg-muted/50 px-2 py-1 rounded-full">
                          <span>{post.author}</span>
                        </div>
                      )}
                    </div>

                    <h2 className="text-lg md:text-xl font-bold text-foreground dark:text-white mb-2 group-hover:text-primary transition-colors">
                      {post.title}
                    </h2>

                    {post.description && (
                      <p className="text-sm text-muted-foreground mb-3 leading-relaxed">
                        {post.description}
                      </p>
                    )}

                    <Link
                      href={`/changelogs/${post.slug}`}
                      className="inline-flex items-center gap-1 text-primary hover:text-primary/80 font-medium text-sm group/link"
                    >
                      Read Release Notes
                      <ArrowRight className="w-3 h-3 group-hover/link:translate-x-1 transition-transform" />
                    </Link>
                  </div>
                </div>
              </article>
            ))
          )}
        </div>

        <div className="mt-16 bg-gradient-to-r from-primary/10 via-primary/5 to-primary/10 border border-primary/20 rounded-xl p-6 text-center">
          <MessageCircle className="w-8 h-8 text-primary mx-auto mb-3" />
          <h3 className="text-xl font-bold text-foreground dark:text-white mb-2">
            Join Our Community
          </h3>
          <p className="text-sm text-muted-foreground mb-4 max-w-2xl mx-auto">
            Get notified about new releases, discuss features, and connect with other PoloCloud users. 
            Never miss an update again!
          </p>
          <a
            href="https://discord.com/invite/mQ39S2EWNV"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-2 bg-primary text-primary-foreground font-medium px-4 py-2 rounded-lg hover:bg-primary/90 transition-colors text-sm"
          >
            <MessageCircle className="w-4 h-4" />
            Join Discord Server
          </a>
        </div>
      </div>
    </div>
  );
} 