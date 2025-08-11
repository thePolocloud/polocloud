import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';
import { readFile } from 'fs/promises';
import path from 'path';
import { getBlogFileFromGitHub, BLOG_REPO_CONFIG, BlogMeta } from '@/lib/github';
import matter from 'gray-matter';

let adminUsers: string[] = [];

async function loadAdminUsers() {
  try {
    const filePath = path.join(process.cwd(), 'data', 'admin-users.json');
    const data = await readFile(filePath, 'utf8');
    adminUsers = JSON.parse(data);
  } catch (error) {
    adminUsers = [];
  }
}

export async function GET(req: NextRequest) {
  try {

    await loadAdminUsers();

    const cookieStore = await cookies();
    const githubAdminCookie = cookieStore.get('github_admin_auth');
    const userCookie = cookieStore.get('user');
    const adminCookie = cookieStore.get('admin_auth');
    
    let isAuthenticated = false;
    let userData: any = null;

    if (githubAdminCookie) {
      try {
        userData = JSON.parse(githubAdminCookie.value);

        isAuthenticated = true;
      } catch {

      }
    }

    if (!isAuthenticated && userCookie) {
      try {
        userData = JSON.parse(userCookie.value);
        

        if (adminUsers.includes(userData.id)) {
          isAuthenticated = true;
        }
      } catch {

      }
    }

    if (!isAuthenticated && adminCookie) {
      try {
        const adminData = JSON.parse(adminCookie.value);

        isAuthenticated = true;
        userData = { username: adminData.username, id: 'admin' };
      } catch {

      }
    }
    
    if (!isAuthenticated) {
      return NextResponse.json({ error: 'Not authenticated' }, { status: 401 });
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

            return {
              slug,
              title: `${slug} (YAML Error)`,
              description: 'This post has invalid YAML frontmatter',
              date: new Date().toISOString().split('T')[0],
              author: 'System',
              tags: ['error'],
              pinned: false,
              contentPreview: 'This post needs to be fixed...',
              wordCount: 0,
            };
          }
          
          return {
            slug,
            title: frontmatter.title || page.title,
            description: frontmatter.description || '',
            date: frontmatter.date || '',
            author: frontmatter.author || '',
            tags: frontmatter.tags || [],
            pinned: frontmatter.pinned || false,
            contentPreview: content.substring(0, 200) + (content.length > 200 ? '...' : ''),
            wordCount: content.split(/\s+/).length,
          };
        } catch (error) {
          return null;
        }
      })
    );

    const validPosts = posts.filter(post => post !== null)
      .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());

    return NextResponse.json({ posts: validPosts });

  } catch (error) {
    return NextResponse.json({ 
      error: 'Failed to list blog posts',
      details: error instanceof Error ? error.message : 'Unknown error'
    }, { status: 500 });
  }
}
