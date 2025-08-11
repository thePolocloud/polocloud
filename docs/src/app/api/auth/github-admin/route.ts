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
  const { searchParams } = new URL(request.url);
  const code = searchParams.get('code');
  const state = searchParams.get('state');

  if (!code) {

    const githubClientId = process.env.GITHUB_ADMIN_CLIENT_ID;
    
    if (!githubClientId) {
      return NextResponse.json(
        { error: 'GitHub OAuth not configured' },
        { status: 500 }
      );
    }

    const params = new URLSearchParams({
      client_id: githubClientId,
      redirect_uri: `${process.env.NEXT_PUBLIC_BASE_URL || 'http://localhost:3000'}/api/auth/github-admin`,
      scope: 'user:email',
      state: 'admin-login'
    });

    return NextResponse.redirect(`https://github.com/login/oauth/authorize?${params}`);
  }

  try {
    const githubClientId = process.env.GITHUB_ADMIN_CLIENT_ID;
    const githubClientSecret = process.env.GITHUB_ADMIN_CLIENT_SECRET;

    if (!githubClientId || !githubClientSecret) {
      return NextResponse.json(
        { error: 'GitHub OAuth not configured' },
        { status: 500 }
      );
    }

    const tokenResponse = await fetch('https://github.com/login/oauth/access_token', {
      method: 'POST',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        client_id: githubClientId,
        client_secret: githubClientSecret,
        code,
      }),
    });

    const tokenData = await tokenResponse.json();

    if (!tokenData.access_token) {
      console.error('Failed to get access token:', tokenData);
      console.error('Full token response:', JSON.stringify(tokenData, null, 2));
      const baseUrl = process.env.NEXT_PUBLIC_BASE_URL || 'http://localhost:3000';
      return NextResponse.redirect(`${baseUrl}/admin?error=oauth_failed`);
    }

    const userResponse = await fetch('https://api.github.com/user', {
      headers: {
        'Authorization': `Bearer ${tokenData.access_token}`,
        'Accept': 'application/vnd.github.v3+json',
      },
    });

    const userData = await userResponse.json();

    const allowedUsers = await loadAllowedAdminUsers();
    console.log(`GitHub login attempt by: ${userData.login}`);
    console.log(`Allowed users:`, allowedUsers);
    
    if (!allowedUsers.includes(userData.login)) {

      const baseUrl = process.env.NEXT_PUBLIC_BASE_URL || 'http://localhost:3000';
      return NextResponse.redirect(`${baseUrl}/admin?error=unauthorized`);
    }


    const adminSession = {
      id: userData.id,
      username: userData.login,
      name: userData.name || userData.login,
      avatar: userData.avatar_url,
      isGitHubAdmin: true,
      loginTime: new Date().toISOString()
    };

    const baseUrl = process.env.NEXT_PUBLIC_BASE_URL || 'http://localhost:3000';
    const response = NextResponse.redirect(`${baseUrl}/admin?success=github_login`);

    response.cookies.set('github_admin_auth', JSON.stringify(adminSession), {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'lax',
      maxAge: 60 * 60 * 24 * 7,
      path: '/'
    });

    return response;

  } catch (error) {
    console.error('GitHub admin OAuth error:', error);
    const baseUrl = process.env.NEXT_PUBLIC_BASE_URL || 'http://localhost:3000';
    return NextResponse.redirect(`${baseUrl}/admin?error=oauth_error`);
  }
}
