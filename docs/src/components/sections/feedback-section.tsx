"use client";

import { useEffect, useState } from "react";
import { MessageSquare, Users, Star } from "lucide-react";
import Marquee from "@/components/ui/marquee";
import FeedbackCard from "@/components/ui/feedback-card";
import { Button } from "@/components/ui/button";
import Link from "next/link";

interface PublicFeedback {
  id: string;
  username: string;
  avatar: string;
  rating: number;
  description: string;
  createdAt: string;
  approvedAt?: string;
}

export default function FeedbackSection() {
  const [feedbacks, setFeedbacks] = useState<PublicFeedback[]>([]);
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({
    total: 0,
    averageRating: 0
  });

  useEffect(() => {
    fetchFeedbacks();
  }, []);

  const fetchFeedbacks = async () => {
    try {
      const response = await fetch('/api/public/feedback');
      const data = await response.json();
      
      if (data.feedbacks) {
        setFeedbacks(data.feedbacks);

        const total = data.feedbacks.length;
        const averageRating = total > 0 
          ? data.feedbacks.reduce((sum: number, fb: PublicFeedback) => sum + fb.rating, 0) / total
          : 0;
        
        setStats({ total, averageRating });
      }
    } catch (error) {
      console.error('Error fetching feedbacks:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <section className="relative py-24 overflow-hidden">
        <div className="container mx-auto px-6 text-center">
          <div className="animate-pulse">
            <div className="h-8 bg-muted rounded w-64 mx-auto mb-4" />
            <div className="h-4 bg-muted rounded w-96 mx-auto mb-8" />
            <div className="h-32 bg-muted rounded-lg" />
          </div>
        </div>
      </section>
    );
  }

  if (feedbacks.length === 0) {
    return (
      <section className="relative py-24 overflow-hidden">

        <div className="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.02)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.02)_1px,transparent_1px)] bg-[size:50px_50px] dark:bg-[linear-gradient(rgba(255,255,255,0.01)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.01)_1px,transparent_1px)]" />
        
        <div className="container mx-auto px-6 relative z-10">
          <div className="text-center mb-16">
            <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-primary/10 border border-primary/20 text-primary font-medium text-sm mb-6">
              <MessageSquare className="w-4 h-4" />
              Community Feedback
            </div>
            
            <h2 className="text-3xl md:text-4xl lg:text-5xl font-black mb-6 bg-gradient-to-r from-foreground via-primary to-foreground bg-clip-text text-transparent tracking-tight leading-tight">
              What Our Community Says
            </h2>
            
            <p className="text-muted-foreground text-lg max-w-2xl mx-auto mb-8">
              Be the first to share your feedback and help others make their decision.
            </p>
            
            <Link
              href="/feedback"
              className="group bg-[rgba(0,120,255,0.9)] hover:bg-[rgba(0,120,255,1)] text-white px-8 py-3 rounded-xl font-semibold text-lg transition-all duration-300 shadow-[0_0_20px_rgba(0,120,255,0.3)] hover:shadow-[0_0_25px_rgba(0,120,255,0.4)] flex items-center justify-center gap-3 relative z-10"
            >
              <MessageSquare className="w-5 h-5" />
              Share Your First Feedback
            </Link>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="relative py-24 overflow-hidden">

      <div className="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.02)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.02)_1px,transparent_1px)] bg-[size:50px_50px] dark:bg-[linear-gradient(rgba(255,255,255,0.01)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.01)_1px,transparent_1px)]" />
      
      <div className="container mx-auto px-6 relative z-10">
        <div className="text-center mb-16">
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-primary/10 border border-primary/20 text-primary font-medium text-sm mb-6">
            <MessageSquare className="w-4 h-4" />
            Community Feedback
          </div>
          
          <h2 className="text-3xl md:text-4xl lg:text-5xl font-black mb-6 bg-gradient-to-r from-foreground via-primary to-foreground bg-clip-text text-transparent tracking-tight leading-tight">
            What Our Community Says
          </h2>
          
          <p className="text-muted-foreground text-lg max-w-2xl mx-auto mb-8">
            Real reviews from real users - discover why developers love PoloCloud.
          </p>
          

          <div className="flex items-center justify-center gap-8 mb-12">
            <div className="flex items-center gap-2 px-4 py-2 rounded-full bg-muted/50 border border-border/50">
              <Users className="w-4 h-4 text-primary" />
              <span className="font-semibold">{stats.total}</span>
              <span className="text-muted-foreground text-sm">Reviews</span>
            </div>
            <div className="flex items-center gap-2 px-4 py-2 rounded-full bg-muted/50 border border-border/50">
              <Star className="w-4 h-4 text-amber-500 fill-amber-500" />
              <span className="font-semibold">{stats.averageRating.toFixed(1)}</span>
              <span className="text-muted-foreground text-sm">Average</span>
            </div>
          </div>
        </div>


        <div className="relative">

          <div className="absolute left-0 top-0 bottom-0 w-32 bg-gradient-to-r from-background to-transparent z-10 pointer-events-none" />
          <div className="absolute right-0 top-0 bottom-0 w-32 bg-gradient-to-l from-background to-transparent z-10 pointer-events-none" />
          
          <Marquee pauseOnHover className="[--duration:60s] [--gap:2rem] py-4">
            {feedbacks.map((feedback) => (
              <FeedbackCard
                key={feedback.id}
                username={feedback.username}
                avatar={feedback.avatar}
                rating={feedback.rating}
                description={feedback.description}
                createdAt={feedback.createdAt}
                className="mx-4 first:ml-8"
              />
            ))}
          </Marquee>
        </div>


        <div className="text-center mt-16">
          <Link href="/feedback">
            <Button className="group relative overflow-hidden bg-gradient-to-r from-primary to-blue-600 text-white font-semibold py-4 px-8 rounded-xl shadow-lg hover:shadow-xl transition-all duration-300 transform hover:scale-105">
              <div className="absolute inset-0 w-full h-full bg-gradient-to-r from-primary/0 via-white/20 to-primary/0 opacity-0 group-hover:opacity-100 transition-opacity duration-700 transform -skew-x-12 group-hover:animate-shimmer"></div>
              <span className="relative flex items-center gap-2">
                <MessageSquare className="w-5 h-5" />
                Share Your Feedback
              </span>
            </Button>
          </Link>
        </div>
      </div>
    </section>
  );
}
