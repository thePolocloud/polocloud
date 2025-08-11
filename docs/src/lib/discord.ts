export interface DiscordUser {
  id: string;
  username: string;
  discriminator: string;
  avatar: string;
  email?: string;
  verified: boolean;
}

export interface DiscordAuthResponse {
  access_token: string;
  token_type: string;
  expires_in: number;
  refresh_token: string;
  scope: string;
}

const DISCORD_CLIENT_ID = process.env.DISCORD_CLIENT_ID;
const DISCORD_CLIENT_SECRET = process.env.DISCORD_CLIENT_SECRET;
const DISCORD_REDIRECT_URI = process.env.DISCORD_REDIRECT_URI || 'http://localhost:3000/api/auth/discord/callback';

export function getDiscordAuthUrl(): string {
  if (!DISCORD_CLIENT_ID) {
    throw new Error('Discord OAuth2 not configured');
  }

  const params = new URLSearchParams({
    client_id: DISCORD_CLIENT_ID,
    redirect_uri: DISCORD_REDIRECT_URI,
    response_type: 'code',
    scope: 'identify email',
  });
  
  return `https://discord.com/api/oauth2/authorize?${params.toString()}`;
}

export async function exchangeCodeForToken(code: string): Promise<DiscordAuthResponse> {
  if (!DISCORD_CLIENT_ID || !DISCORD_CLIENT_SECRET) {
    throw new Error('Discord OAuth2 not configured');
  }

  const tokenResponse = await fetch('https://discord.com/api/oauth2/token', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: new URLSearchParams({
      client_id: DISCORD_CLIENT_ID,
      client_secret: DISCORD_CLIENT_SECRET,
      grant_type: 'authorization_code',
      code,
      redirect_uri: DISCORD_REDIRECT_URI,
    }),
  });

  if (!tokenResponse.ok) {
    const errorText = await tokenResponse.text();
    console.error('Discord token exchange failed:', errorText);
    throw new Error('Failed to exchange code for token');
  }

  return tokenResponse.json();
}

export async function getDiscordUser(accessToken: string): Promise<DiscordUser> {
  const userResponse = await fetch('https://discord.com/api/users/@me', {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });

  if (!userResponse.ok) {
    const errorText = await userResponse.text();
    console.error('Discord user fetch failed:', errorText);
    throw new Error('Failed to fetch Discord user');
  }

  return userResponse.json();
}

export async function revokeToken(token: string): Promise<void> {
  if (!DISCORD_CLIENT_ID || !DISCORD_CLIENT_SECRET) {
    return;
  }

  await fetch('https://discord.com/api/oauth2/token/revoke', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: new URLSearchParams({
      client_id: DISCORD_CLIENT_ID,
      client_secret: DISCORD_CLIENT_SECRET,
      token,
    }),
  });
}
