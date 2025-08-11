import { NextRequest, NextResponse } from 'next/server';
import { getPartnersFromGitHub, savePartnersToGitHub } from '@/lib/github';

export async function GET(req: NextRequest) {
  try {

    const adminAuth = req.cookies.get('admin_auth')?.value;
    const discordUser = req.cookies.get('discord_user')?.value;
    const githubAdminAuth = req.cookies.get('github_admin_auth')?.value;

    if (!adminAuth && !discordUser && !githubAdminAuth) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const partners = await getPartnersFromGitHub();
    return NextResponse.json({ partners });
  } catch (error) {
    console.error('Error fetching partners:', error);
    return NextResponse.json({ error: 'Failed to fetch partners' }, { status: 500 });
  }
}

export async function POST(req: NextRequest) {
  try {

    const githubAdminAuth = req.cookies.get('github_admin_auth')?.value;
    const adminAuth = req.cookies.get('admin_auth')?.value;
    const discordUser = req.cookies.get('discord_user')?.value;

    console.log('- github_admin_auth:', githubAdminAuth ? 'exists' : 'missing');
    console.log('- admin_auth:', adminAuth ? 'exists' : 'missing');
    console.log('- discord_user:', discordUser ? 'exists' : 'missing');

    if (githubAdminAuth) {
      const adminAuthData = JSON.parse(githubAdminAuth);


      const userLogin = adminAuthData.username;

      if (userLogin === 'jakubbbdev') {

      } else {
        return NextResponse.json({ 
          error: `Only jakubbbdev can manage partners. Your login: ${userLogin || 'unknown'}` 
        }, { status: 403 });
      }
    }
    else if (adminAuth) {
      const adminData = JSON.parse(adminAuth);

      if (adminData.username === 'jakubbbdev' || adminData.username === 'admin') {

      } else {
        return NextResponse.json({ 
          error: `Only admin can manage partners. Your login: ${adminData.username}` 
        }, { status: 403 });
      }
    }

    else {
      return NextResponse.json({ error: 'Admin authentication required' }, { status: 401 });
    }

    const { name, logo, website, description } = await req.json();

    if (!name || !logo) {
      return NextResponse.json({ error: 'Name and logo are required' }, { status: 400 });
    }

    const currentPartners = await getPartnersFromGitHub();

    const newPartner = {
      id: Date.now().toString(),
      name: name.trim(),
      logo: logo.trim(),
      website: website?.trim() || '',
      description: description?.trim() || '',
      addedAt: new Date().toISOString(),
      addedBy: 'jakubbbdev'
    };

    const updatedPartners = [...currentPartners, newPartner];

    await savePartnersToGitHub(updatedPartners);

    console.log('✅ Partner added successfully, cache will refresh automatically');

    return NextResponse.json({ 
      success: true, 
      partner: newPartner,
      message: 'Partner added successfully' 
    });
  } catch (error) {
    console.error('Error adding partner:', error);
    return NextResponse.json({ error: 'Failed to add partner' }, { status: 500 });
  }
}

export async function DELETE(req: NextRequest) {
  try {

    const githubAdminAuth = req.cookies.get('github_admin_auth')?.value;
    

    if (!githubAdminAuth) {
      return NextResponse.json({ error: 'GitHub admin authentication required' }, { status: 401 });
    }

    const adminAuthData = JSON.parse(githubAdminAuth);

    if (adminAuthData.username !== 'jakubbbdev') {
      return NextResponse.json({ 
        error: `Only jakubbbdev can manage partners. Your username: ${adminAuthData.username}` 
      }, { status: 403 });
    }

    const { searchParams } = new URL(req.url);
    const partnerId = searchParams.get('id');

    if (!partnerId) {
      return NextResponse.json({ error: 'Partner ID is required' }, { status: 400 });
    }

    const currentPartners = await getPartnersFromGitHub();

    const updatedPartners = currentPartners.filter(p => p.id !== partnerId);
    
    if (updatedPartners.length === currentPartners.length) {
      return NextResponse.json({ error: 'Partner not found' }, { status: 404 });
    }

    await savePartnersToGitHub(updatedPartners);

    console.log('✅ Partner removed successfully, cache will refresh automatically');

    return NextResponse.json({ 
      success: true, 
      message: 'Partner removed successfully' 
    });
  } catch (error) {
    console.error('Error removing partner:', error);
    return NextResponse.json({ error: 'Failed to remove partner' }, { status: 500 });
  }
}
