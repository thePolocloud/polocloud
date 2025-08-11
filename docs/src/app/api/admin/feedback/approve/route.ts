import { NextRequest, NextResponse } from 'next/server';
import { updateFeedbackStatusOnGitHub } from '@/lib/github';

export async function POST(request: NextRequest) {
  const githubAdminCookie = request.cookies.get('github_admin_auth');
  const adminCookie = request.cookies.get('admin_auth');
  const userCookie = request.cookies.get('discord_user');
  
  if (!githubAdminCookie && !adminCookie && !userCookie) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }

  let adminUser = 'admin';
  if (githubAdminCookie) {
    try {
      const parsed = JSON.parse(githubAdminCookie.value);
      adminUser = parsed.username || 'github-admin';
    } catch {
      adminUser = 'github-admin';
    }
  } else if (adminCookie) {
    try {
      const parsed = JSON.parse(adminCookie.value);
      adminUser = parsed.username || 'admin';
    } catch {
      adminUser = 'admin';
    }
  } else if (userCookie) {
    try {
      const parsed = JSON.parse(userCookie.value);
      adminUser = parsed.username || 'discord-admin';
    } catch {
      adminUser = 'discord-admin';
    }
  }

  try {
    const { feedbackId } = await request.json();
    
    if (!feedbackId) {
      return NextResponse.json({ error: 'feedbackId is required' }, { status: 400 });
    }
b
    await updateFeedbackStatusOnGitHub(feedbackId, 'APPROVED', adminUser);

    return NextResponse.json({ 
      success: true, 
      message: 'Feedback approved successfully' 
    });

  } catch (error) {
    console.error('Approve feedback error:', error);
    return NextResponse.json(
      { error: 'Failed to approve feedback' },
      { status: 500 }
    );
  }
}
