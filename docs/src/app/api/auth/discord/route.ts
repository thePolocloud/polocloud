import { NextRequest, NextResponse } from 'next/server';
import { getDiscordAuthUrl } from '@/lib/discord';

export async function GET(request: NextRequest) {
  try {
    const authUrl = getDiscordAuthUrl();
    return NextResponse.redirect(authUrl);
  } catch (error) {
    console.error('Discord auth error:', error);
    return NextResponse.redirect(new URL('/feedback?error=auth_failed', request.url));
  }
}
