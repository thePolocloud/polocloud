'use client';

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { 
  FileText, 
  Edit3, 
  Plus,
  Eye,
  Edit,
  Calendar,
  User,
  Sparkles,
  Tags
} from 'lucide-react';
import { useRouter } from 'next/navigation';

interface BlogPost {
  slug: string;
  title: string;
  description: string;
  date: string;
  author: string;
  tags: string[];
  pinned: boolean;
  contentPreview: string;
  wordCount: number;
}

export default function AdminBlogPage() {
  const router = useRouter();
  const [posts, setPosts] = useState<BlogPost[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadPosts();
  }, []);

  const loadPosts = async () => {
    try {
      const response = await fetch('/api/admin/blog/list', {
        credentials: 'include',
      });
      
      if (response.ok) {
        const data = await response.json();
        setPosts(data.posts || []);
      } else {
        console.error('Failed to load blog posts');
      }
    } catch (error) {
      console.error('Error loading blog posts:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-background via-background to-background/50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-4"></div>
          <p className="text-muted-foreground">Loading blog posts...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="relative min-h-screen">
      <div className="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.02)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.02)_1px,transparent_1px)] bg-[size:50px_50px] dark:bg-[linear-gradient(rgba(255,255,255,0.01)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.01)_1px,transparent_1px)]" />
      
      <div className="container mx-auto px-6 py-12 max-w-6xl relative z-10">
        <div className="text-center mb-10">
          <h1 className="text-3xl md:text-4xl lg:text-5xl font-black mb-8 bg-gradient-to-r from-foreground via-primary to-foreground bg-clip-text text-transparent tracking-tight leading-tight">
            Blog Management
          </h1>
          <div className="w-20 h-1 bg-gradient-to-r from-transparent via-border/50 to-transparent rounded-full mx-auto mt-4" />
          
          <div className="flex justify-center gap-4 mt-8">
            <button
              onClick={() => router.push('/admin/blog/create')}
              className="inline-flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-primary/10 to-primary/5 border border-primary/20 rounded-lg hover:from-primary/20 hover:to-primary/10 transition-all duration-300 text-foreground/80 hover:text-foreground"
            >
              <Plus className="w-4 h-4" />
              Create New Post
            </button>
            <button
              onClick={loadPosts}
              className="inline-flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-secondary/10 to-secondary/5 border border-secondary/20 rounded-lg hover:from-secondary/20 hover:to-secondary/10 transition-all duration-300 text-foreground/80 hover:text-foreground"
            >
              <FileText className="w-4 h-4" />
              Refresh Posts
            </button>
          </div>
        </div>

        <div className="relative overflow-hidden rounded-xl border border-border/50 bg-gradient-to-b from-background/50 to-background/80 p-6">
          <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-primary/5 opacity-20" />
          <div className="relative">
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-6">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-full bg-primary/10 flex items-center justify-center">
                  <Edit3 className="w-6 h-6 text-primary" />
                </div>
                <div>
                  <div className="text-sm text-muted-foreground">Blog Posts</div>
                  <div className="text-foreground font-semibold">{posts.length} Published</div>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <Button 
                  onClick={() => router.push('/admin/blog/create')} 
                  className="inline-flex items-center gap-2 px-3 py-2 rounded-lg border border-border/50 bg-background hover:bg-background/70 text-sm"
                  variant="outline"
                >
                  <Plus className="w-4 h-4" /> New Post
                </Button>
                <Button 
                  onClick={loadPosts} 
                  className="inline-flex items-center gap-2 px-3 py-2 rounded-lg border border-border/50 bg-background hover:bg-background/70 text-sm"
                  variant="outline"
                >
                  <FileText className="w-4 h-4" /> Refresh
                </Button>
              </div>
            </div>
              
            {posts.length === 0 ? (
              <div className="text-center py-12">
                <FileText className="w-16 h-16 mx-auto mb-4 text-muted-foreground/50" />
                <div className="text-muted-foreground mb-4">No blog posts found</div>
                <Button
                  onClick={() => router.push('/admin/blog/create')}
                  variant="outline"
                >
                  <Plus className="w-4 h-4 mr-2" />
                  Create your first blog post
                </Button>
              </div>
            ) : (
              <div className="grid gap-4 md:grid-cols-2">
                {posts.map((post) => (
                  <div 
                    key={post.slug} 
                    className="relative overflow-hidden rounded-xl border border-border/50 bg-gradient-to-b from-background/50 to-background/80 p-5"
                  >
                    <div className="flex items-start justify-between mb-3">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center">
                          <FileText className="w-5 h-5 text-primary" />
                        </div>
                        <div>
                          <div className="font-semibold text-foreground line-clamp-2">{post.title}</div>
                          <div className="text-xs text-muted-foreground">{new Date(post.date).toLocaleString()}</div>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        {post.pinned && (
                          <div className="bg-primary/10 text-primary px-2 py-1 rounded-full text-xs font-medium">
                            <Sparkles className="w-3 h-3 inline mr-1" />
                            Pinned
                          </div>
                        )}
                      </div>
                    </div>
                    
                    {post.description && (
                      <p className="text-muted-foreground text-sm mb-3 line-clamp-2">
                        {post.description}
                      </p>
                    )}
                    
                    <div className="flex items-center gap-2 text-xs text-muted-foreground mb-3">
                      <span className="flex items-center gap-1">
                        <User className="w-3 h-3" />
                        {post.author}
                      </span>
                      <span>•</span>
                      <span className="flex items-center gap-1">
                        <FileText className="w-3 h-3" />
                        {post.wordCount} words
                      </span>
                    </div>

                    {post.tags.length > 0 && (
                      <div className="flex flex-wrap gap-1 mb-3">
                        {post.tags.map((tag) => (
                          <span 
                            key={tag} 
                            className="px-2 py-0.5 bg-muted/50 text-muted-foreground text-xs rounded-full"
                          >
                            {tag}
                          </span>
                        ))}
                      </div>
                    )}

                    <div className="flex gap-2 mt-4">
                      <button
                        onClick={() => window.open('/blog/' + post.slug, '_blank')}
                        className="flex-1 px-3 py-2 bg-primary/10 text-primary border border-primary/20 rounded-lg hover:bg-primary/20 transition-colors text-sm font-medium"
                      >
                        <Eye className="w-3 h-3 inline mr-1" />
                        View
                      </button>
                      <button
                        onClick={() => console.log('Edit post:', post.slug)}
                        className="flex-1 px-3 py-2 bg-muted/50 border border-border/50 rounded-lg hover:bg-muted/70 transition-colors text-sm font-medium"
                      >
                        <Edit className="w-3 h-3 inline mr-1" />
                        Edit
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
