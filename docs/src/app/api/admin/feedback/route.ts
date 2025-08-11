import { NextRequest, NextResponse } from 'next/server';
import { getFeedbackFromGitHub, type FeedbackData } from '@/lib/github';

export async function GET(request: NextRequest) {
  const githubAdminCookie = request.cookies.get('github_admin_auth');
  const adminCookie = request.cookies.get('admin_auth');
  const userCookie = request.cookies.get('discord_user');
  
  if (!githubAdminCookie && !adminCookie && !userCookie) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  try {

    const feedbacks = await getFeedbackFromGitHub();

    feedbacks.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

    return NextResponse.json({ feedbacks });
  } catch (error) {
    console.error('Error loading feedbacks from GitHub:', error);
    return NextResponse.json({ error: 'Failed to load feedbacks' }, { status: 500 });
  }
}
