import { NextRequest, NextResponse } from 'next/server';
import { getBlogFileFromGitHub, createOrUpdateBlogFile } from '@/lib/github';

interface GitHubAdminUser {
  username: string;
  id: string;
  name?: string;
  avatar?: string;
  role: 'SUPER_ADMIN' | 'ADMIN';
  addedBy: string;
  addedAt: string;
  isFounder?: boolean;
}

async function loadGitHubAdminUsers(): Promise<GitHubAdminUser[]> {
  try {
    const adminFile = await getBlogFileFromGitHub('docs/data/github-admin-users.json');
    if (adminFile) {
      return JSON.parse(adminFile.content);
    }

    return [
      {
        username: 'jakubbbdev',
        id: 'jakubbbdev',
        role: 'SUPER_ADMIN',
        addedBy: 'system',
        addedAt: new Date().toISOString(),
        isFounder: true
      }
    ];
  } catch (error) {
    console.error('Error loading GitHub admin users:', error);

    return [
      {
        username: 'jakubbbdev',
        id: 'jakubbbdev',
        role: 'SUPER_ADMIN',
        addedBy: 'system',
        addedAt: new Date().toISOString(),
        isFounder: true
      }
    ];
  }
}


async function saveGitHubAdminUsers(users: GitHubAdminUser[]): Promise<void> {
  const content = JSON.stringify(users, null, 2);

  let sha: string | undefined;
  try {
    const existingFile = await getBlogFileFromGitHub('docs/data/github-admin-users.json');
    if (existingFile) {
      sha = existingFile.sha;
    }
  } catch (error) {

  }
  
  await createOrUpdateBlogFile(
    'docs/data/github-admin-users.json',
    content,
    `Update GitHub admin users list`,
    sha
  );
}

function getAuthenticatedUser(request: NextRequest): GitHubAdminUser | null {
  const githubAdminCookie = request.cookies.get('github_admin_auth');
  if (!githubAdminCookie) return null;

  try {
    const session = JSON.parse(githubAdminCookie.value);
    return {
      username: session.username,
      id: session.id,
      name: session.name,
      avatar: session.avatar,
      role: session.username === 'jakubbbdev' ? 'SUPER_ADMIN' : 'ADMIN',
      addedBy: 'unknown',
      addedAt: session.loginTime || new Date().toISOString()
    };
  } catch {
    return null;
  }
}

export async function GET(request: NextRequest) {
  try {
    const currentUser = getAuthenticatedUser(request);
    if (!currentUser) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const users = await loadGitHubAdminUsers();

    const enrichedUsers = await Promise.all(users.map(async (user) => {
      try {

        const githubResponse = await fetch(`https://api.github.com/users/${user.username}`);
        if (githubResponse.ok) {
          const githubData = await githubResponse.json();
          return {
            ...user,
            avatar: githubData.avatar_url,
            name: githubData.name || user.name || user.username
          };
        }
      } catch (error) {
        console.error(`Failed to fetch GitHub data for ${user.username}:`, error);
      }
      return user;
    }));
    
    return NextResponse.json({
      users: enrichedUsers,
      currentUser: currentUser.username,
      isSuperAdmin: currentUser.username === 'jakubbbdev'
    });
  } catch (error) {
    console.error('Error loading GitHub admin users:', error);
    return NextResponse.json({ error: 'Failed to load users' }, { status: 500 });
  }
}

export async function POST(request: NextRequest) {
  try {
    const currentUser = getAuthenticatedUser(request);
    if (!currentUser || currentUser.username !== 'jakubbbdev') {
      return NextResponse.json({ error: 'Only admin can add users' }, { status: 403 });
    }

    const { githubUsername } = await request.json();
    
    if (!githubUsername || typeof githubUsername !== 'string') {
      return NextResponse.json({ error: 'GitHub username is required' }, { status: 400 });
    }
    const githubResponse = await fetch(`https://api.github.com/users/${githubUsername}`, {
      headers: {
        'Authorization': `Bearer ${process.env.GITHUB_TOKEN}`,
        'Accept': 'application/vnd.github.v3+json',
      },
    });

    if (!githubResponse.ok) {
      return NextResponse.json({ error: 'GitHub user not found' }, { status: 404 });
    }

    const githubUser = await githubResponse.json();

    const users = await loadGitHubAdminUsers();

    if (users.find(u => u.username === githubUsername)) {
      return NextResponse.json({ error: 'User already has admin access' }, { status: 409 });
    }

    const newUser: GitHubAdminUser = {
      username: githubUsername,
      id: githubUser.id.toString(),
      name: githubUser.name,
      avatar: githubUser.avatar_url,
      role: 'ADMIN',
      addedBy: currentUser.username,
      addedAt: new Date().toISOString()
    };

    users.push(newUser);
    await saveGitHubAdminUsers(users);

    return NextResponse.json({
      success: true,
      user: newUser,
      message: `${githubUsername} has been added as admin`
    });
  } catch (error) {
    console.error('Error adding GitHub admin user:', error);
    return NextResponse.json({ error: 'Failed to add user' }, { status: 500 });
  }
}

export async function DELETE(request: NextRequest) {
  try {
    const currentUser = getAuthenticatedUser(request);
    if (!currentUser || currentUser.username !== 'jakubbbdev') {
      return NextResponse.json({ error: 'Only admin can remove users' }, { status: 403 });
    }

    const { searchParams } = new URL(request.url);
    const username = searchParams.get('username');
    
    if (!username) {
      return NextResponse.json({ error: 'Username is required' }, { status: 400 });
    }

    if (username === 'jakubbbdev') {
      return NextResponse.json({ error: 'Cannot remove founder' }, { status: 403 });
    }

    const users = await loadGitHubAdminUsers();

    const userIndex = users.findIndex(u => u.username === username);
    if (userIndex === -1) {
      return NextResponse.json({ error: 'User not found' }, { status: 404 });
    }

    const removedUser = users[userIndex];
    users.splice(userIndex, 1);
    await saveGitHubAdminUsers(users);

    return NextResponse.json({
      success: true,
      message: `${username} has been removed from admin access`,
      removedUser
    });
  } catch (error) {
    console.error('Error removing GitHub admin user:', error);
    return NextResponse.json({ error: 'Failed to remove user' }, { status: 500 });
  }
}
