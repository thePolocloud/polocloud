import { NextResponse } from 'next/server';
import { getBlogFileFromGitHub, BLOG_REPO_CONFIG, BlogMeta } from '@/lib/github';
import matter from 'gray-matter';

let blogCache: any = null;
let blogCacheTimestamp = 0;
const BLOG_CACHE_DURATION = 5 * 60 * 1000;

export async function GET() {
  try {
    const now = Date.now();

    if (blogCache && (now - blogCacheTimestamp) < BLOG_CACHE_DURATION) {
      const response = NextResponse.json({ posts: blogCache });
      response.headers.set('Cache-Control', 'public, max-age=300');
      return response;
    }

    const metaFile = await getBlogFileFromGitHub(BLOG_REPO_CONFIG.metaFile);
    
    if (!metaFile) {
      return NextResponse.json({ posts: [] });
    }

    const meta: BlogMeta = JSON.parse(metaFile.content);
    const blogSection = meta.pages.find(p => p.title === "Blog");
    
    if (!blogSection) {
      return NextResponse.json({ posts: [] });
    }

    const posts = await Promise.all(
      blogSection.pages.map(async (page) => {
        try {
          const slug = page.url.replace('/blog/', '');
          const filePath = `${BLOG_REPO_CONFIG.blogPath}/${slug}.mdx`;
          
          const file = await getBlogFileFromGitHub(filePath);
          if (!file) return null;

          let frontmatter: any;
          let content: string;
          
          try {
            const parsed = matter(file.content);
            frontmatter = parsed.data;
            content = parsed.content;
          } catch (yamlError) {

            return null;
          }

          const preview = content.substring(0, 150);
          const wordCount = content.split(/\s+/).filter(word => word.length > 0).length;
          
          return {
            slug,
            title: frontmatter.title || page.title,
            description: frontmatter.description || '',
            date: frontmatter.date || '',
            author: frontmatter.author || '',
            tags: frontmatter.tags || [],
            pinned: frontmatter.pinned || false,
            contentPreview: preview + (content.length > 150 ? '...' : ''),
            wordCount,
          };
        } catch (error) {
          return null;
        }
      })
    );

    const validPosts = posts.filter(post => post !== null)
      .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());

    blogCache = validPosts;
    blogCacheTimestamp = now;

    const response = NextResponse.json({ posts: validPosts });
    response.headers.set('Cache-Control', 'public, max-age=300');
    return response;

  } catch (error) {

    if (blogCache) {
      return NextResponse.json({ posts: blogCache });
    }
    
    return NextResponse.json({ 
      error: 'Failed to load blog posts',
      details: error instanceof Error ? error.message : 'Unknown error'
    }, { status: 500 });
  }
}
