import { NextResponse } from 'next/server';
import { getFeedbackFromGitHub } from '@/lib/github';

export const revalidate = 300;

export async function GET() {
  try {

    const allFeedback = await getFeedbackFromGitHub();

    const approvedFeedback = allFeedback
      .filter(feedback => feedback.status === 'APPROVED')
      .map(feedback => ({
        id: feedback.id,
        username: feedback.username,
        avatar: feedback.avatar,
        rating: feedback.rating,
        description: feedback.description,
        createdAt: feedback.createdAt,
        approvedAt: feedback.approvedAt
      }))
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

    return NextResponse.json(
      { feedbacks: approvedFeedback },
      {
        headers: {
          'Cache-Control': 'public, s-maxage=300, stale-while-revalidate=600',
        },
      }
    );
  } catch (error) {
    console.error('Error loading public feedbacks:', error);
    return NextResponse.json(
      { feedbacks: [] },
      {
        headers: {
          'Cache-Control': 'public, s-maxage=60, stale-while-revalidate=120',
        },
      }
    );
  }
}
