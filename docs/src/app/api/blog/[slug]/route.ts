import { NextRequest, NextResponse } from 'next/server';
import { getBlogFileFromGitHub, BLOG_REPO_CONFIG } from '@/lib/github';
import matter from 'gray-matter';

const postCache = new Map<string, { data: any; timestamp: number }>();
const POST_CACHE_DURATION = 10 * 60 * 1000;

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ slug: string }> }
) {
  try {
    const { slug } = await params;
    
    if (!slug) {
      return NextResponse.json({ error: 'Slug is required' }, { status: 400 });
    }

    const now = Date.now();

    const cached = postCache.get(slug);
    if (cached && (now - cached.timestamp) < POST_CACHE_DURATION) {
      const response = NextResponse.json({ post: cached.data });
      response.headers.set('Cache-Control', 'public, max-age=600');
      return response;
    }

    const filePath = `${BLOG_REPO_CONFIG.blogPath}/${slug}.mdx`;
    const file = await getBlogFileFromGitHub(filePath);
    
    if (!file) {
      return NextResponse.json({ error: 'Blog post not found' }, { status: 404 });
    }

    let frontmatter: any;
    let content: string;
    
    try {
      const parsed = matter(file.content);
      frontmatter = parsed.data;
      content = parsed.content;
    } catch (yamlError) {
      return NextResponse.json({ 
        error: 'Invalid blog post format',
        details: 'YAML frontmatter could not be parsed'
      }, { status: 400 });
    }
    
    const post = {
      slug,
      title: frontmatter.title || 'Untitled',
      description: frontmatter.description || '',
      date: frontmatter.date || '',
      author: frontmatter.author || '',
      tags: frontmatter.tags || [],
      pinned: frontmatter.pinned || false,
      content: content.trim(),
    };

    postCache.set(slug, { data: post, timestamp: now });

    const response = NextResponse.json({ post });
    response.headers.set('Cache-Control', 'public, max-age=600');
    return response;

  } catch (error) {
    return NextResponse.json({ 
      error: 'Failed to load blog post',
      details: error instanceof Error ? error.message : 'Unknown error'
    }, { status: 500 });
  }
}
