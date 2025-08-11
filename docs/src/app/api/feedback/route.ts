import { NextRequest, NextResponse } from 'next/server';
import { 
  addFeedbackToGitHub, 
  getUserFeedbackFromGitHub,
  type FeedbackData 
} from '@/lib/github';

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const { rating, description } = body;


    const userCookie = request.cookies.get('discord_user');
    
    if (!userCookie) {
      return NextResponse.json(
        { error: 'Authentication required' },
        { status: 401 }
      );
    }

    let userData;
    try {
      userData = JSON.parse(userCookie.value);
    } catch (parseError) {
      return NextResponse.json(
        { error: 'Invalid user data' },
        { status: 400 }
      );
    }

    const userId = userData.id;

    const existingFeedback = await getUserFeedbackFromGitHub(userId);
    if (existingFeedback && existingFeedback.status !== 'REJECTED') {
      return NextResponse.json(
        { error: 'You have already submitted feedback' },
        { status: 409 }
      );
    }

    if (!rating || rating < 1 || rating > 5) {
      return NextResponse.json(
        { error: 'Invalid rating' },
        { status: 400 }
      );
    }

    if (!description || description.trim().length < 10) {
      return NextResponse.json(
        { error: 'Description must be at least 10 characters long' },
        { status: 400 }
      );
    }

    const avatarUrl = userData.avatar 
      ? `https://cdn.discordapp.com/avatars/${userId}/${userData.avatar}.png`
      : null;

    const feedback = await addFeedbackToGitHub({
      userId,
      username: userData.username,
      rating,
      description: description.trim(),
      avatar: avatarUrl || '',
    });

    return NextResponse.json({
      success: true,
      message: 'Feedback submitted successfully',
      feedback,
    });

  } catch (error) {
    console.error('Feedback submission error:', error);
    return NextResponse.json(
      { error: 'Failed to submit feedback' },
      { status: 500 }
    );
  }
}

export async function GET(request: NextRequest) {
  try {

    const userCookie = request.cookies.get('discord_user');
    
    if (!userCookie) {
      return NextResponse.json(
        { error: 'Authentication required' },
        { status: 401 }
      );
    }

    let userData;
    try {
      userData = JSON.parse(userCookie.value);
    } catch (parseError) {
      return NextResponse.json(
        { error: 'Invalid user data' },
        { status: 400 }
      );
    }

    const userId = userData.id;

    const existingFeedback = await getUserFeedbackFromGitHub(userId);

    if (existingFeedback?.status === 'APPROVED') {
      return NextResponse.json({
        hasSubmitted: true,
        feedback: existingFeedback,
        status: 'APPROVED',
      });
    } else if (existingFeedback?.status === 'PENDING') {
      return NextResponse.json({
        hasSubmitted: true,
        feedback: existingFeedback,
        status: 'PENDING',
      });
    } else if (existingFeedback?.status === 'REJECTED') {
      return NextResponse.json({
        hasSubmitted: false,
        feedback: null,
        status: 'REJECTED',
        previouslyRejected: true,
      });
    } else {
      return NextResponse.json({
        hasSubmitted: false,
        feedback: null,
        status: null,
      });
    }

  } catch (error) {
    console.error('Feedback check error:', error);
    return NextResponse.json(
      { error: 'Failed to check feedback status' },
      { status: 500 }
    );
  }
}
