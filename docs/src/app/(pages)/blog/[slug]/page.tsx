'use client';

import { notFound } from 'next/navigation';
import { PageLayout } from '@/components/layout/page-layout';
import { Calendar, User, Tag, ArrowLeft, Github, ExternalLink, FileText } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import Link from 'next/link';
import { useEffect, useState, use } from 'react';

interface BlogPost {
  title: string;
  description?: string;
  date?: string;
  author?: string;
  tags?: string[];
  content: string;
  slug: string;
}

export default function BlogPage({ params }: { params: Promise<{ slug: string }> }) {
  const resolvedParams = use(params);
  const [post, setPost] = useState<BlogPost | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    async function loadBlogPost() {
      try {
        const response = await fetch(`/api/blog/${resolvedParams.slug}`);
        if (response.ok) {
          const data = await response.json();
          setPost(data.post);
        } else {
          setError(true);
        }
      } catch (err) {
        console.error('Failed to load blog post:', err);
        setError(true);
      } finally {
        setLoading(false);
      }
    }

    loadBlogPost();
  }, [resolvedParams.slug]);

  if (loading) {
    return (
      <PageLayout>
        <div className="relative min-h-screen">
          <div className="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.02)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.02)_1px,transparent_1px)] bg-[size:50px_50px] dark:bg-[linear-gradient(rgba(255,255,255,0.01)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.01)_1px,transparent_1px)]" />
          
          <div className="container mx-auto px-6 py-12 max-w-4xl relative z-10">
            <div className="text-center py-16">
              <FileText className="w-16 h-16 text-muted-foreground mx-auto mb-4 animate-pulse" />
              <h2 className="text-2xl font-bold text-foreground mb-4">
                Loading blog post...
              </h2>
              <p className="text-muted-foreground max-w-md mx-auto">
                Please wait while we fetch the content.
              </p>
            </div>
          </div>
        </div>
      </PageLayout>
    );
  }

  if (error || !post) {
    notFound();
  }

  return (
    <PageLayout>
      <div className="relative min-h-screen">
        <div className="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.02)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.02)_1px,transparent_1px)] bg-[size:50px_50px] dark:bg-[linear-gradient(rgba(255,255,255,0.01)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.01)_1px,transparent_1px)]" />
        
        <div className="container mx-auto px-6 py-12 max-w-4xl relative z-10">
          <div className="mb-8">
            <Link
              href="/blog"
              className="inline-flex items-center gap-2 text-muted-foreground hover:text-foreground transition-colors group"
            >
              <ArrowLeft className="w-4 h-4 group-hover:-translate-x-1 transition-transform" />
              Back to Blog
            </Link>
          </div>

          <div className="mb-12">
            <div className="flex items-center gap-4 text-sm text-muted-foreground mb-6">
              {post.date && (
                <div className="flex items-center gap-2 bg-muted/50 px-3 py-1 rounded-full">
                  <Calendar className="w-4 h-4" />
                  <span>{new Date(post.date).toLocaleDateString('de-DE', {
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric'
                  })}</span>
                </div>
              )}
              {post.author && (
                <div className="flex items-center gap-2 bg-muted/50 px-3 py-1 rounded-full">
                  <User className="w-4 h-4" />
                  <span>{post.author}</span>
                </div>
              )}
            </div>
            
            <h1 className="text-4xl md:text-5xl font-black text-foreground dark:text-white mb-6 leading-tight">
              {post.title}
            </h1>
            
            {post.description && (
              <p className="text-xl text-muted-foreground mb-8 leading-relaxed max-w-3xl">
                {post.description}
              </p>
            )}
            
            {post.tags && (
              <div className="flex flex-wrap gap-2 mb-8">
                {post.tags.map((tag: string) => (
                  <span
                    key={tag}
                    className="inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs font-medium bg-primary/10 text-primary border border-primary/20 hover:bg-primary/20 transition-colors"
                  >
                    <Tag className="w-3 h-3" />
                    {tag}
                  </span>
                ))}
              </div>
            )}
          </div>

          <article className="prose prose-lg dark:prose-invert max-w-none bg-card/30 backdrop-blur-sm border border-border/50 rounded-xl p-8 mb-12">
            <ReactMarkdown
              remarkPlugins={[remarkGfm]}
              components={{
                a: ({ href, children, ...props }) => {
                  if (href?.startsWith('/')) {
                    return <Link href={href} className="text-primary hover:text-primary/80 underline decoration-primary/30 hover:decoration-primary transition-all duration-200" {...props}>{children}</Link>;
                  }
                  return <a href={href} target="_blank" rel="noopener noreferrer" className="text-primary hover:text-primary/80 underline decoration-primary/30 hover:decoration-primary transition-all duration-200 inline-flex items-center gap-1" {...props}>{children}<ExternalLink className="w-3 h-3" /></a>;
                },
                h1: ({ children, ...props }) => (
                  <h1 className="text-3xl md:text-4xl font-black text-foreground dark:text-white mb-6 mt-8 first:mt-0" {...props}>{children}</h1>
                ),
                h2: ({ children, ...props }) => (
                  <h2 className="text-2xl md:text-3xl font-bold text-foreground dark:text-white mb-4 mt-8" {...props}>{children}</h2>
                ),
                h3: ({ children, ...props }) => (
                  <h3 className="text-xl md:text-2xl font-bold text-foreground dark:text-white mb-3 mt-6" {...props}>{children}</h3>
                ),
                p: ({ children, ...props }) => (
                  <p className="text-muted-foreground leading-relaxed mb-4" {...props}>{children}</p>
                ),
                strong: ({ children, ...props }) => (
                  <strong className="font-bold text-foreground dark:text-white" {...props}>{children}</strong>
                ),
                code: ({ children, ...props }) => (
                  <code className="bg-muted text-foreground px-2 py-1 rounded text-sm font-mono" {...props}>{children}</code>
                ),
                pre: ({ children, ...props }) => (
                  <pre className="bg-muted border border-border/50 rounded-lg p-4 overflow-x-auto mb-4" {...props}>{children}</pre>
                ),
                ul: ({ children, ...props }) => (
                  <ul className="mb-4 pl-6 space-y-2" {...props}>{children}</ul>
                ),
                ol: ({ children, ...props }) => (
                  <ol className="mb-4 pl-6 space-y-2" {...props}>{children}</ol>
                ),
                li: ({ children, ...props }) => (
                  <li className="text-muted-foreground" {...props}>{children}</li>
                ),
                blockquote: ({ children, ...props }) => (
                  <blockquote className="border-l-4 border-primary/30 pl-4 italic text-muted-foreground my-6" {...props}>{children}</blockquote>
                ),
              }}
            >
              {post.content}
            </ReactMarkdown>
          </article>

          <div className="bg-card/30 backdrop-blur-sm border border-border/50 rounded-xl p-6">
            <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
              <div className="flex flex-col gap-2">
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <User className="w-4 h-4" />
                  <span>Written by <strong className="text-foreground">{post.author || 'PoloCloud Team'}</strong></span>
                </div>
                {post.date && (
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <Calendar className="w-4 h-4" />
                    <span>Published on <strong className="text-foreground">{new Date(post.date).toLocaleDateString('de-DE')}</strong></span>
                  </div>
                )}
              </div>
              <div className="flex items-center gap-4">
                <a
                  href="https://github.com/HttpMarco/polocloud"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-2 text-muted-foreground hover:text-foreground transition-colors bg-muted/50 px-3 py-2 rounded-lg hover:bg-muted"
                >
                  <Github className="w-4 h-4" />
                  View on GitHub
                </a>
              </div>
            </div>
          </div>
        </div>
      </div>
    </PageLayout>
  );
} 