import { NextRequest, NextResponse } from 'next/server';
import { exchangeCodeForToken, getDiscordUser } from '@/lib/discord';

export async function GET(request: NextRequest) {
  const searchParams = request.nextUrl.searchParams;
  const code = searchParams.get('code');
  const error = searchParams.get('error');

  if (error) {
    return NextResponse.redirect(new URL('/feedback?error=auth_failed', request.url));
  }

  if (!code) {
    return NextResponse.redirect(new URL('/feedback?error=no_code', request.url));
  }

  try {
    const tokenResponse = await exchangeCodeForToken(code);
    const user = await getDiscordUser(tokenResponse.access_token);

    const userData = {
      id: user.id,
      username: user.username,
      avatar: user.avatar || '',
      email: user.email,
    };

    const response = NextResponse.redirect(new URL('/feedback?success=true', request.url));
    

    response.cookies.set('discord_user', JSON.stringify(userData), {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'lax',
      maxAge: 60 * 60 * 24 * 7,
    });

    return response;
  } catch (error) {
    console.error('Discord auth error:', error);
    return NextResponse.redirect(new URL('/feedback?error=auth_failed', request.url));
  }
}
