'use client';

import Link from 'next/link';
import { Calendar, User, Tag, ArrowRight, Pin, MessageCircle, FileText } from 'lucide-react';
import { useEffect, useState } from 'react';

interface BlogPost {
  title: string;
  description?: string;
  date?: string;
  author?: string;
  tags?: string[];
  slug: string;
  pinned?: boolean;
  contentPreview?: string;
  wordCount?: number;
}

export default function BlogIndexPage() {
  const [blogPosts, setBlogPosts] = useState<BlogPost[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadBlogPosts() {
      try {
        const response = await fetch('/api/blog');
        if (response.ok) {
          const data = await response.json();
          setBlogPosts(data.posts || []);
        }
      } catch (error) {
        console.error('Failed to load blog posts:', error);
      } finally {
        setLoading(false);
      }
    }

    loadBlogPosts();
  }, []);

  if (loading) {
    return (
      <div className="relative min-h-screen">
        <div className="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.02)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.02)_1px,transparent_1px)] bg-[size:50px_50px] dark:bg-[linear-gradient(rgba(255,255,255,0.01)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.01)_1px,transparent_1px)]" />
        
        <div className="container mx-auto px-6 py-12 max-w-6xl relative z-10">
          <div className="text-center py-16">
            <FileText className="w-16 h-16 text-muted-foreground mx-auto mb-4 animate-pulse" />
            <h2 className="text-2xl font-bold text-foreground mb-4">
              Loading blog posts...
            </h2>
            <p className="text-muted-foreground max-w-md mx-auto">
              Please wait while we fetch the latest content.
            </p>
          </div>
        </div>
      </div>
    );
  }

  const pinnedPosts = blogPosts.filter(post => post.pinned);
  const unpinnedPosts = blogPosts.filter(post => !post.pinned);

  const sortedPinnedPosts = [...pinnedPosts].sort((a, b) => {
    if (!a.date || !b.date) return 0;
    return new Date(b.date).getTime() - new Date(a.date).getTime();
  });

  const sortedUnpinnedPosts = [...unpinnedPosts].sort((a, b) => {
    if (!a.date || !b.date) return 0;
    return new Date(b.date).getTime() - new Date(a.date).getTime();
  });

  return (
      <div className="relative min-h-screen">
        <div className="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.02)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.02)_1px,transparent_1px)] bg-[size:50px_50px] dark:bg-[linear-gradient(rgba(255,255,255,0.01)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.01)_1px,transparent_1px)]" />
        
        <div className="container mx-auto px-6 py-12 max-w-6xl relative z-10">
          <div className="text-center mb-16">
            <div className="flex items-center justify-center gap-3 mb-6">
              <div className="flex items-center justify-center w-10 h-10 bg-primary/10 text-primary rounded-full border border-primary/20">
                <FileText className="w-5 h-5" />
              </div>
            </div>
            <h1 className="text-3xl md:text-4xl lg:text-5xl font-black bg-gradient-to-r from-foreground via-primary to-foreground bg-clip-text text-transparent mb-6 leading-tight">
              PoloCloud Blog
            </h1>
            <p className="text-base md:text-lg text-muted-foreground max-w-3xl mx-auto leading-relaxed">
              Stay updated with the latest news, tutorials, and insights about Minecraft server hosting and PoloCloud development.
            </p>
          </div>

          {sortedPinnedPosts.length > 0 && (
            <div className="mb-12">
              <div className="flex items-center gap-2 mb-6">
                <Pin className="w-5 h-5 text-primary" />
                <h2 className="text-2xl font-bold text-foreground dark:text-white">
                  Pinned Posts
                </h2>
              </div>
              <div className="grid gap-8 md:grid-cols-2 lg:grid-cols-3">
                {sortedPinnedPosts.map((post, index) => (
                  <article
                    key={post.slug}
                    className="group bg-primary/5 hover:bg-primary/10 border-2 border-primary/20 hover:border-primary/30 rounded-xl p-6 transition-all duration-300 hover:shadow-lg backdrop-blur-sm hover:scale-[1.02] transform relative"
                  >
                    <div className="absolute -top-2 -right-2 bg-primary text-primary-foreground p-1 rounded-full">
                      <Pin className="w-3 h-3" />
                    </div>

                    <div className="flex items-center gap-4 text-sm text-muted-foreground mb-4">
                      {post.date && (
                        <div className="flex items-center gap-2 bg-muted/50 px-2 py-1 rounded-full">
                          <Calendar className="w-3 h-3" />
                          <span className="text-xs">{new Date(post.date).toLocaleDateString('de-DE', {
                            year: 'numeric',
                            month: 'short',
                            day: 'numeric'
                          })}</span>
                        </div>
                      )}
                      {post.author && (
                        <div className="flex items-center gap-2 bg-muted/50 px-2 py-1 rounded-full">
                          <User className="w-3 h-3" />
                          <span className="text-xs">{post.author}</span>
                        </div>
                      )}
                    </div>

                    <h2 className="text-xl font-bold text-foreground dark:text-white mb-3 group-hover:text-primary transition-colors line-clamp-2">
                      {post.title}
                    </h2>

                    {post.description && (
                      <p className="text-muted-foreground mb-4 leading-relaxed line-clamp-3">
                        {post.description}
                      </p>
                    )}

                    {post.tags && post.tags.length > 0 && (
                      <div className="flex flex-wrap gap-2 mb-6">
                        {post.tags.slice(0, 3).map((tag) => (
                          <span
                            key={tag}
                            className="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium bg-primary/20 text-primary border border-primary/30 hover:bg-primary/30 transition-colors"
                          >
                            <Tag className="w-3 h-3" />
                            {tag}
                          </span>
                        ))}
                        {post.tags.length > 3 && (
                          <span className="text-xs text-muted-foreground bg-muted/50 px-2 py-1 rounded-full">
                            +{post.tags.length - 3} more
                          </span>
                        )}
                      </div>
                    )}

                    <Link
                      href={`/blog/${post.slug}`}
                      className="inline-flex items-center gap-2 text-primary hover:text-primary/80 font-medium transition-colors group/link"
                    >
                      Read More
                      <ArrowRight className="w-4 h-4 group-hover/link:translate-x-1 transition-transform" />
                    </Link>
                  </article>
                ))}
              </div>
            </div>
          )}

          {sortedUnpinnedPosts.length > 0 && (
            <div>
              <h2 className="text-2xl font-bold text-foreground dark:text-white mb-6">
                All Posts
              </h2>
              <div className="grid gap-8 md:grid-cols-2 lg:grid-cols-3">
                {sortedUnpinnedPosts.map((post, index) => (
                  <article
                    key={post.slug}
                    className="group bg-card/50 hover:bg-card border border-border/50 hover:border-border rounded-xl p-6 transition-all duration-300 hover:shadow-lg backdrop-blur-sm hover:scale-[1.02] transform"
                  >
                    <div className="flex items-center gap-4 text-sm text-muted-foreground mb-4">
                      {post.date && (
                        <div className="flex items-center gap-2 bg-muted/50 px-2 py-1 rounded-full">
                          <Calendar className="w-3 h-3" />
                          <span className="text-xs">{new Date(post.date).toLocaleDateString('de-DE', {
                            year: 'numeric',
                            month: 'short',
                            day: 'numeric'
                          })}</span>
                        </div>
                      )}
                      {post.author && (
                        <div className="flex items-center gap-2 bg-muted/50 px-2 py-1 rounded-full">
                          <User className="w-3 h-3" />
                          <span className="text-xs">{post.author}</span>
                        </div>
                      )}
                    </div>

                    <h2 className="text-xl font-bold text-foreground dark:text-white mb-3 group-hover:text-primary transition-colors line-clamp-2">
                      {post.title}
                    </h2>

                    {post.description && (
                      <p className="text-muted-foreground mb-4 leading-relaxed line-clamp-3">
                        {post.description}
                      </p>
                    )}

                    {post.tags && post.tags.length > 0 && (
                      <div className="flex flex-wrap gap-2 mb-6">
                        {post.tags.slice(0, 3).map((tag) => (
                          <span
                            key={tag}
                            className="inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium bg-primary/10 text-primary border border-primary/20 hover:bg-primary/20 transition-colors"
                          >
                            <Tag className="w-3 h-3" />
                            {tag}
                          </span>
                        ))}
                        {post.tags.length > 3 && (
                          <span className="text-xs text-muted-foreground bg-muted/50 px-2 py-1 rounded-full">
                            +{post.tags.length - 3} more
                          </span>
                        )}
                      </div>
                    )}

                    <Link
                      href={`/blog/${post.slug}`}
                      className="inline-flex items-center gap-2 text-primary hover:text-primary/80 font-medium transition-colors group/link"
                    >
                      Read More
                      <ArrowRight className="w-4 h-4 group-hover/link:translate-x-1 transition-transform" />
                    </Link>
                  </article>
                ))}
              </div>
            </div>
          )}

          {blogPosts.length === 0 && (
            <div className="text-center py-16">
              <FileText className="w-16 h-16 text-muted-foreground mx-auto mb-4" />
              <h2 className="text-2xl font-bold text-foreground dark:text-white mb-4">
                No blog posts yet
              </h2>
              <p className="text-muted-foreground max-w-md mx-auto">
                We&apos;re working on creating amazing content for you. Check back soon for updates!
              </p>
            </div>
          )}

          <div className="mt-16 bg-gradient-to-r from-primary/10 via-primary/5 to-primary/10 border border-primary/20 rounded-xl p-6 text-center">
            <MessageCircle className="w-8 h-8 text-primary mx-auto mb-3" />
            <h3 className="text-xl font-bold text-foreground dark:text-white mb-2">
              Join Our Community
            </h3>
            <p className="text-sm text-muted-foreground mb-4 max-w-2xl mx-auto">
              Get notified about new blog posts, discuss features, and connect with other PoloCloud users. 
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