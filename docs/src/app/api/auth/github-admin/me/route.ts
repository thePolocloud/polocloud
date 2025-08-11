import { NextRequest, NextResponse } from 'next/server';
import { getBlogFileFromGitHub } from '@/lib/github';

async function loadAllowedAdminUsers(): Promise<string[]> {
  try {
    const adminFile = await getBlogFileFromGitHub('docs/data/github-admin-users.json');
    if (adminFile) {
      const users = JSON.parse(adminFile.content);
      return users.map((user: any) => user.username);
    }
    return ['jakubbbdev'];
  } catch (error) {
    console.error('Error loading allowed admin users:', error);

    return ['jakubbbdev'];
  }
}

export async function GET(request: NextRequest) {
  try {
    const githubAdminCookie = request.cookies.get('github_admin_auth');
    
    if (!githubAdminCookie) {
      return NextResponse.json({ authenticated: false });
    }

    let session;
    try {
      session = JSON.parse(githubAdminCookie.value);
    } catch {
      return NextResponse.json({ authenticated: false });
    }


    if (!session.username || !session.id) {
      return NextResponse.json({ authenticated: false });
    }


    const allowedUsers = await loadAllowedAdminUsers();
    if (!allowedUsers.includes(session.username)) {

      const response = NextResponse.json({ 
        authenticated: false, 
        error: 'Access revoked',
        message: 'Your admin access has been revoked'
      }, { status: 403 });
      
      response.cookies.set('github_admin_auth', '', { 
        maxAge: 0,
        httpOnly: true,
        secure: process.env.NODE_ENV === 'production',
        sameSite: 'lax'
      });
      
      return response;
    }

    return NextResponse.json({
      authenticated: true,
      user: {
        id: session.id,
        username: session.username,
        name: session.name,
        avatar: session.avatar,
        isGitHubAdmin: true
      }
    });
  } catch (error) {
    console.error('Error validating GitHub admin session:', error);
    return NextResponse.json({ authenticated: false }, { status: 500 });
  }
}