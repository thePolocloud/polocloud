import { NextRequest, NextResponse } from 'next/server';

export async function GET(request: NextRequest) {
  const cookie = request.cookies.get('admin_auth');
  if (!cookie) {
    return NextResponse.json({ authenticated: false }, { status: 200 });
  }
  try {
    const data = JSON.parse(cookie.value);
    return NextResponse.json({ authenticated: true, user: data }, { status: 200 });
  } catch {
    return NextResponse.json({ authenticated: false }, { status: 200 });
  }
}
