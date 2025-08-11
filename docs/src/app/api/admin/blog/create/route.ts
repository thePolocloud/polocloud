import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';
import { readFile } from 'fs/promises';
import path from 'path';
import { 
  createOrUpdateBlogFile, 
  updateBlogMeta, 
  createSlug, 
  generateMDXContent,
  BLOG_REPO_CONFIG,
  BlogPostMetadata 
} from '@/lib/github';

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

export async function POST(req: NextRequest) {
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

    const { title, description, content, author, tags, pinned } = await req.json();

    if (!title || !description || !content) {
      return NextResponse.json({ 
        error: 'Title, description, and content are required' 
      }, { status: 400 });
    }
    const slug = createSlug(title);

    const currentDate = new Date().toISOString().split('T')[0];

    const metadata: BlogPostMetadata = {
      title: title.trim(),
      description: description.trim(),
      date: currentDate,
      author: author?.trim() || userData.username,
      tags: Array.isArray(tags) ? tags.map((tag: string) => tag.trim()) : [],
      pinned: Boolean(pinned),
      slug
    };

    const mdxContent = generateMDXContent(metadata, content.trim());

    const filePath = `${BLOG_REPO_CONFIG.blogPath}/${slug}.mdx`;

    await createOrUpdateBlogFile(
      filePath,
      mdxContent,
      `Add blog post: ${title}`
    );

    await updateBlogMeta({ title, slug });

    return NextResponse.json({ 
      success: true, 
      slug,
      message: 'Blog post created successfully'
    });

  } catch (error) {
    console.error('Error creating blog post:', error);
    return NextResponse.json({ 
      error: 'Failed to create blog post',
      details: error instanceof Error ? error.message : 'Unknown error'
    }, { status: 500 });
  }
}
