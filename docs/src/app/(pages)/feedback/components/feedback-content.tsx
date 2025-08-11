'use client';

import { useEffect, useState, useRef } from 'react';
import { MessageSquare, Star, Send, CheckCircle, LogOut, User, Info, Home, Clock } from 'lucide-react';
import { motion } from 'framer-motion';
import Image from 'next/image';

interface DiscordUser {
  id: string;
  username: string;
  avatar: string;
  email?: string;
}

interface FeedbackData {
  userId: string;
  username: string;
  rating: number;
  description: string;
  timestamp: string;
  isPending?: boolean;
  avatar?: string;
}

export function FeedbackContent() {
  const [user, setUser] = useState<DiscordUser | null>(null);
  const [hasSubmittedFeedback, setHasSubmittedFeedback] = useState(false);
  const [existingFeedback, setExistingFeedback] = useState<FeedbackData | null>(null);
  const [selectedRating, setSelectedRating] = useState(0);
  const [description, setDescription] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [authError, setAuthError] = useState<string | null>(null);
  const [toast, setToast] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  
  const containerRef = useRef<HTMLDivElement>(null);

  const showToast = (type: 'success' | 'error', message: string) => {
    setToast({ type, message });
    setTimeout(() => setToast(null), 5000);
  };

  useEffect(() => {
    console.log('🔍 FeedbackContent component mounted');
    console.log('🔍 Initial state:', {
      user,
      hasSubmittedFeedback,
      existingFeedback,
      isLoading,
      authError
    });
  }, []);

  useEffect(() => {
    if (!containerRef.current) return;

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('animate-in');
          }
        });
      },
      { threshold: 0.1 }
    );

    observer.observe(containerRef.current);
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    console.log('🔍 Starting auth and feedback check...');
    
    const checkAuthAndFeedback = async () => {
      try {
        console.log('🔍 Fetching /api/feedback...');

        const authResponse = await fetch('/api/feedback', {
          credentials: 'include',
        });
        console.log('🔍 Auth response status:', authResponse.status);
        console.log('🔍 Auth response headers:', Object.fromEntries(authResponse.headers.entries()));
        
        if (authResponse.ok) {
          const data = await authResponse.json();
          console.log('🔍 Auth response data:', data);

          if (data.hasSubmitted) {
            console.log('🔍 User has already submitted feedback');
            setHasSubmittedFeedback(true);
            if (data.feedback) {

            setExistingFeedback(data.feedback);
            } else if (data.status === 'PENDING') {

              setExistingFeedback({ 
                ...data.feedback,
                isPending: true
              });
            } else if (data.status === 'REJECTED' && data.previouslyRejected) {

              setHasSubmittedFeedback(false);
              showToast('error', 'Your previous feedback was not approved. You can submit new feedback.');
            }

            if (data.feedback) {
              setUser({
                id: data.feedback.userId,
                username: data.feedback.username,
                avatar: '',
              });
            }
          } else {
            console.log('🔍 User is authenticated but hasn\'t submitted feedback');

            const userData = await getCurrentUser();
            console.log('🔍 Current user data:', userData);
            if (userData) {
              setUser(userData);
            }
          }
        } else if (authResponse.status === 401) {
          console.log('🔍 User not authenticated (401)');
          setUser(null);
        } else {
          console.log('🔍 Unexpected response status:', authResponse.status);
          const errorText = await authResponse.text();
          console.log('🔍 Error response:', errorText);
        }
      } catch (error) {
        console.error('🔍 Error checking auth status:', error);
        setAuthError('Failed to check authentication status');
      } finally {
        console.log('🔍 Setting isLoading to false');
        setIsLoading(false);
      }
    };

    checkAuthAndFeedback();
  }, []);


  const getCurrentUser = async (): Promise<DiscordUser | null> => {
    try {
      console.log('🔍 Fetching /api/auth/user...');
      const response = await fetch('/api/auth/user', {
        credentials: 'include',
      });
      console.log('🔍 User API response status:', response.status);

      if (response.ok) {
        const userData = await response.json();
        console.log('🔍 User API response data:', userData);
        return userData;
      } else {
        console.log('🔍 User API failed with status:', response.status);
        const errorText = await response.text();
        console.log('🔍 User API error:', errorText);
      }
    } catch (error) {
      console.error('🔍 Error getting user data:', error);
    }
    return null;
  };

  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search);
    const success = urlParams.get('success');
    const error = urlParams.get('error');

    console.log('🔍 URL params:', { success, error });

    if (success === 'true') {
      console.log('🔍 Success parameter detected, checking auth...');

      const checkAuth = async () => {
        try {
          const userData = await getCurrentUser();
          if (userData) {
            setUser(userData);

            const feedbackResponse = await fetch('/api/feedback', {
              credentials: 'include',
            });
            if (feedbackResponse.ok) {
              const data = await feedbackResponse.json();
              if (data.hasSubmitted) {
                setHasSubmittedFeedback(true);
            if (data.status === 'APPROVED' && data.feedback) {
                setExistingFeedback(data.feedback);
            } else if (data.status === 'PENDING' && data.feedback) {
              setExistingFeedback({ 
                ...data.feedback,
                isPending: true
              });
            } else if (data.status === 'REJECTED' && data.previouslyRejected) {
              setHasSubmittedFeedback(false);
              showToast('error', 'Your previous feedback was not approved. You can submit new feedback.');
            }
              }
            }
          }
        } catch (error) {
          console.error('🔍 Error checking auth after login:', error);
        }
      };
      checkAuth();
    } else if (error) {
      console.error('🔍 Authentication error:', error);
      setAuthError('Authentication failed. Please try again.');
    }
  }, []);

  const handleDiscordLogin = () => {
    console.log('🔍 Discord login clicked, redirecting to /api/auth/discord');
    window.location.href = '/api/auth/discord';
  };

  const handleLogout = async () => {
    try {
      console.log('🔍 Logout clicked');
      await fetch('/api/auth/logout', { method: 'POST' });
      setUser(null);
      setHasSubmittedFeedback(false);
      setExistingFeedback(null);
      window.location.href = '/feedback';
    } catch (error) {
      console.error('🔍 Logout error:', error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (selectedRating === 0 || description.length < 10) return;

          console.log('🔍 Submitting feedback:', { rating: selectedRating, description });

    setIsSubmitting(true);
    try {
      const response = await fetch('/api/feedback', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ rating: selectedRating, description }),
        credentials: 'include',
      });

      console.log('🔍 Response status:', response.status);
      console.log('🔍 Response headers:', Object.fromEntries(response.headers.entries()));

      if (response.ok) {
        const result = await response.json();
        console.log('🔍 Feedback submission successful:', result);
                        showToast('success', 'Feedback sent successfully! Thank you for your opinion.');

        setHasSubmittedFeedback(true);
        setExistingFeedback({
          ...result.feedback,
          isPending: true
        });

      } else {
        let errorData;
        try {
          errorData = await response.json();
        } catch (e) {
          errorData = { error: 'Network or server error' };
        }
        console.error('🔍 Feedback submission failed:', errorData);
        showToast('error', errorData.error || 'Unknown error while sending feedback');
      }
    } catch (error) {
      console.error('🔍 Feedback submission error:', error);
      showToast('error', 'Fehler beim Senden des Feedbacks. Bitte versuchen Sie es erneut.');
    } finally {
      setIsSubmitting(false);
    }
  };

  useEffect(() => {
    console.log('🔍 State changed:', {
      user,
      hasSubmittedFeedback,
      existingFeedback,
      isLoading,
      authError
    });
  }, [user, hasSubmittedFeedback, existingFeedback, isLoading, authError]);

  const renderStars = (rating: number) => {
    return Array.from({ length: 5 }, (_, i) => (
      <button
        key={i}
        type="button"
        onClick={() => setSelectedRating(i + 1)}
        className={`text-3xl transition-all duration-200 hover:scale-110 active:scale-95 ${
          i < rating 
            ? 'text-yellow-400 drop-shadow-md' 
            : 'text-gray-300 hover:text-yellow-200'
        } ${i < rating ? 'animate-bounce' : ''}`}
        style={{
          filter: i < rating ? 'drop-shadow(0 0 8px rgba(251, 191, 36, 0.5))' : 'none'
        }}
      >
        ★
      </button>
    ));
  };

  const getRatingText = (rating: number) => {
    const texts = ['', 'Poor', 'Unsatisfied', 'Okay', 'Good', 'Excellent'];
    return texts[rating] || '';
  };


  console.log('🔍 Rendering with state:', {
    isLoading,
    hasSubmittedFeedback,
    existingFeedback,
    user,
    authError
  });

  if (isLoading) {
    console.log('🔍 Rendering loading state');
    return (
      <div className="relative">
        <div className="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.02)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.02)_1px,transparent_1px)] bg-[size:50px_50px] dark:bg-[linear-gradient(rgba(255,255,255,0.01)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.01)_1px,transparent_1px)]" />
        
        <div className="container mx-auto px-6 py-12 max-w-4xl relative z-10 flex items-center justify-center min-h-[60vh]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-primary mx-auto mb-4"></div>
            <p className="text-lg text-muted-foreground">Loading feedback system...</p>
          </div>
        </div>
      </div>
    );
  }

  if (hasSubmittedFeedback && existingFeedback) {
    console.log('🔍 Rendering existing feedback state');
    
    const isPending = existingFeedback.isPending;
    
    return (
      <div className="relative">
        <div className="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.02)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.02)_1px,transparent_1px)] bg-[size:50px_50px] dark:bg-[linear-gradient(rgba(255,255,255,0.01)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.01)_1px,transparent_1px)]" />
        
        <div className="container mx-auto px-6 py-12 max-w-4xl relative z-10">
        <motion.div
          ref={containerRef}
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="text-center"
        >
          <div className="mb-8">
            {isPending ? (
              <>
                <Clock className="w-16 h-16 text-yellow-500 mx-auto mb-4" />
                <h1 className="text-3xl md:text-4xl lg:text-5xl font-black mb-8 bg-gradient-to-r from-foreground via-primary to-foreground bg-clip-text text-transparent tracking-tight leading-tight">Feedback under review!</h1>
                <p className="text-lg text-muted-foreground mb-6">
                  Your feedback has been submitted and is currently being reviewed by our team.
                </p>
              </>
            ) : (
              <>
            <CheckCircle className="w-16 h-16 text-green-500 mx-auto mb-4" />
                <h1 className="text-3xl md:text-4xl lg:text-5xl font-black mb-8 bg-gradient-to-r from-foreground via-primary to-foreground bg-clip-text text-transparent tracking-tight leading-tight">Feedback already submitted!</h1>
            <p className="text-lg text-muted-foreground mb-6">
                  Thank you for your valuable feedback! Here is an overview of your submission:
            </p>
              </>
            )}
          </div>

          <div className="bg-card border border-border rounded-xl p-8 mb-8 backdrop-blur-sm shadow-lg">

            <div className="flex flex-col md:flex-row items-center justify-between mb-6 gap-4">
              <div className="flex items-center gap-4">
                {existingFeedback.avatar ? (
                  <Image
                    src={existingFeedback.avatar}
                    alt={existingFeedback.username}
                    width={64}
                    height={64}
                    className="rounded-full ring-4 ring-primary/20"
                  />
                ) : (
                  <div className="w-16 h-16 bg-primary/20 rounded-full flex items-center justify-center ring-4 ring-primary/20">
                    <User className="w-8 h-8 text-primary" />
                  </div>
                )}
                <div className="text-left">
                  <h3 className="text-xl font-bold text-foreground">{existingFeedback.username}</h3>
                  <p className="text-sm text-muted-foreground">
                    Feedback submitted on {new Date(existingFeedback.timestamp).toLocaleDateString('en-US', {
                      year: 'numeric',
                      month: 'long',
                      day: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit'
                    })}
                  </p>
                </div>
              </div>
              
              <div className="text-center bg-muted/30 rounded-xl p-4 min-w-[120px]">
                <div className="flex items-center justify-center gap-1 mb-2">
                  {Array.from({ length: 5 }, (_, i) => (
                    <span
                      key={i}
                      className={`text-2xl ${
                        i < existingFeedback.rating ? 'text-yellow-400' : 'text-gray-300'
                      }`}
                    >
                      ★
                    </span>
                  ))}
                </div>
                <p className="text-sm font-medium text-foreground">{getRatingText(existingFeedback.rating)}</p>
              </div>
            </div>



            <div className="bg-muted/30 rounded-xl p-6 border border-border/50">
              <h4 className="text-sm font-medium text-muted-foreground mb-3 uppercase tracking-wide">Your Feedback</h4>
              <p className="text-foreground text-lg leading-relaxed">{existingFeedback.description}</p>
              
              {isPending && (
                <div className="mt-4 p-3 bg-yellow-500/10 border border-yellow-500/30 rounded-lg">
                  <div className="flex items-center gap-2 text-yellow-600">
                    <Clock className="w-4 h-4" />
                    <span className="text-sm font-medium">Waiting for approval</span>
                  </div>
                  <p className="text-xs text-yellow-600/80 mt-1">
                    Thanks for your feedback! We'll review it and make it public soon.
                  </p>
                </div>
              )}
            </div>

            <div className="mt-6 p-4 bg-blue-50 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-800 rounded-lg">
              <div className="flex items-center gap-2 text-blue-700 dark:text-blue-300">
                <Info className="w-4 h-4" />
                <span className="text-sm font-medium">Note</span>
              </div>
              <p className="text-sm text-blue-600 dark:text-blue-400 mt-1">
                You can only submit feedback once. If you want to change your feedback, please contact our support team.
              </p>
            </div>
          </div>

          <div className="flex flex-col sm:flex-row gap-4 justify-center">
          <button
            onClick={handleLogout}
            className="inline-flex items-center gap-2 px-6 py-3 bg-destructive text-destructive-foreground rounded-lg hover:bg-destructive/90 transition-colors"
          >
            <LogOut className="w-4 h-4" />
              Logout
            </button>
            
            <button
              onClick={() => window.location.href = '/'}
              className="inline-flex items-center gap-2 px-6 py-3 bg-secondary text-secondary-foreground rounded-lg hover:bg-secondary/80 transition-colors"
            >
              <Home className="w-4 h-4" />
              Back to Home
          </button>
          </div>
        </motion.div>
        </div>
      </div>
    );
  }

  if (!user) {
    console.log('🔍 Rendering login prompt state');
    return (
      <div className="relative">
        <div className="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.02)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.02)_1px,transparent_1px)] bg-[size:50px_50px] dark:bg-[linear-gradient(rgba(255,255,255,0.01)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.01)_1px,transparent_1px)]" />
        
        <div className="container mx-auto px-6 py-12 max-w-4xl relative z-10">
        <motion.div
          ref={containerRef}
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="text-center"
        >
          <div className="mb-8">
            <MessageSquare className="w-16 h-16 text-primary mx-auto mb-4" />
            <h1 className="text-3xl md:text-4xl lg:text-5xl font-black mb-8 bg-gradient-to-r from-foreground via-primary to-foreground bg-clip-text text-transparent tracking-tight leading-tight">Give Feedback</h1>
            <p className="text-lg text-muted-foreground mb-6">
              Sign in with your Discord account to give feedback.
            </p>
          </div>

          {authError && (
            <div className="bg-destructive/10 border border-destructive/30 rounded-lg p-4 mb-6 text-destructive">
              {authError}
            </div>
          )}

          <button
            onClick={handleDiscordLogin}
            className="inline-flex items-center gap-3 px-8 py-4 bg-[#5865F2] text-white rounded-lg hover:bg-[#4752C4] transition-colors text-lg font-medium"
          >
            <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 24 24">
              <path d="M20.317 4.37a19.791 19.791 0 0 0-4.885-1.515a.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0a12.64 12.64 0 0 0-.617-1.25a.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 0 0 .031.057a19.9 19.9 0 0 0 5.993 3.03a.078.078 0 0 0 .084-.028a14.09 14.09 0 0 0 1.226-1.994a.076.076 0 0 0-.041-.106a13.107 13.107 0 0 1-1.872-.892a.077.077 0 0 1-.008-.128a10.2 10.2 0 0 0 .372-.292a.074.074 0 0 1 .077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 0 1 .078.01c.12.098.246.198.373.292a.077.077 0 0 1-.006.127a12.299 12.299 0 0 1-1.873.892a.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028a19.839 19.839 0 0 0 6.002-3.03a.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.03zM8.02 15.33c-1.183 0-2.157-1.085-2.157-2.419c0-1.333.956-2.419 2.157-2.419c1.21 0 2.176 1.096 2.157 2.42c0 1.333-.956 2.418-2.157 2.418zm7.975 0c-1.183 0-2.157-1.085-2.157-2.419c0-1.333.955-2.419 2.157-2.419c1.21 0 2.176 1.096 2.157 2.42c0 1.333-.946 2.418-2.157 2.418z"/>
            </svg>
            Sign in with Discord
          </button>
        </motion.div>
        </div>
      </div>
    );
  }

  console.log('🔍 Rendering feedback form state');
  return (
    <div className="relative">
      <div className="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.02)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.02)_1px,transparent_1px)] bg-[size:50px_50px] dark:bg-[linear-gradient(rgba(255,255,255,0.01)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.01)_1px,transparent_1px)]" />
      
      <div className="container mx-auto px-6 py-12 max-w-4xl relative z-10">
      <motion.div
        ref={containerRef}
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
        className="text-center"
      >
        <div className="mb-8">
          <div className="text-center mb-10">
            <h1 className="text-3xl md:text-4xl lg:text-5xl font-black mb-8 bg-gradient-to-r from-foreground via-primary to-foreground bg-clip-text text-transparent tracking-tight leading-tight">
              Give Feedback
            </h1>
            <div className="w-20 h-1 bg-gradient-to-r from-transparent via-border/50 to-transparent rounded-full mx-auto mb-4" />
            <p className="text-lg text-muted-foreground max-w-2xl mx-auto leading-relaxed">
              Share your thoughts with us and help us improve PoloCloud.
            </p>
          </div>
        </div>

        <div className="bg-gradient-to-br from-card via-card/95 to-card/90 border border-border/50 rounded-xl p-6 backdrop-blur-xl shadow-lg relative overflow-hidden mb-6">

          <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-primary/5 rounded-xl"></div>
          <div className="absolute top-0 right-0 w-20 h-20 bg-primary/10 rounded-full blur-2xl"></div>
          <div className="absolute bottom-0 left-0 w-16 h-16 bg-primary/5 rounded-full blur-xl"></div>
          
          <div className="relative z-10">

            <div className="flex items-center justify-between mb-8">
              <div className="flex items-center gap-4">
              {user.avatar ? (
                  <div className="relative">
                <Image
                  src={`https://cdn.discordapp.com/avatars/${user.id}/${user.avatar}.png`}
                  alt={user.username}
                      width={56}
                      height={56}
                      className="rounded-full ring-2 ring-primary/20"
                    />
                    <div className="absolute -bottom-1 -right-1 w-5 h-5 bg-green-500 rounded-full border-2 border-card"></div>
                  </div>
                ) : (
                  <div className="w-14 h-14 bg-primary/20 rounded-full flex items-center justify-center ring-2 ring-primary/20">
                    <User className="w-7 h-7 text-primary" />
                </div>
              )}
              <div>
                  <h3 className="text-lg font-bold text-foreground">{user.username}</h3>
                  <p className="text-sm text-muted-foreground">Discord User</p>
              </div>
            </div>
            <button
              onClick={handleLogout}
                  className="inline-flex items-center gap-2 px-4 py-2 text-sm text-muted-foreground hover:text-foreground hover:bg-muted/50 rounded-lg transition-all duration-200"
            >
              <LogOut className="w-4 h-4" />
                  Logout
            </button>
          </div>

            <div className="text-center mb-8">
              <h2 className="text-2xl font-bold text-foreground mb-2">
                Your Feedback
              </h2>
              <p className="text-muted-foreground text-base mb-4">
                Share your opinion with us
              </p>
              <div className="inline-flex items-center gap-2 px-4 py-2 bg-muted/50 text-muted-foreground rounded-full text-sm border border-border/50">
                <Info className="w-4 h-4" />
                You can only submit feedback once
              </div>
            </div>

                      <form onSubmit={handleSubmit} className="space-y-6">

              <div className="space-y-3">
                <label className="block text-lg font-semibold text-foreground text-center">
                  How would you rate PoloCloud?
                </label>
                <div className="flex justify-center gap-2 mb-3">
                  {renderStars(selectedRating)}
                </div>
                {selectedRating > 0 && (
                  <div className="text-center">
                    <span className="inline-flex items-center gap-2 px-3 py-1.5 bg-primary/10 text-primary rounded-full text-sm font-medium border border-primary/20">
                      {getRatingText(selectedRating)}
                    </span>
              </div>
                )}
            </div>

              <div className="space-y-3">
                <label htmlFor="description" className="block text-lg font-semibold text-foreground">
                  Description
              </label>
                <div className="relative">
              <textarea
                id="description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                required
                minLength={10}
                rows={4}
                    className="w-full px-5 py-3 bg-background/50 border border-border/50 rounded-lg focus:ring-2 focus:ring-primary/50 focus:border-primary/50 focus:bg-background/80 transition-all duration-300 resize-none text-foreground placeholder:text-muted-foreground/60"
                    placeholder="Describe your feedback in detail... What do you like? What could be improved?"
                  />
                  <div className="absolute bottom-2 right-2">
                    <span className={`text-sm font-medium px-2 py-1 rounded-full ${
                      description.length >= 10 
                        ? 'bg-green-500/20 text-green-600 border border-green-500/30' 
                        : 'bg-muted text-muted-foreground border border-border'
                    }`}>
                      {description.length}/10
                    </span>
                  </div>
                </div>
                <p className="text-sm text-muted-foreground text-center">
                  Minimum 10 characters required
              </p>
            </div>

              <div className="pt-2">
            <button
              type="submit"
              disabled={selectedRating === 0 || description.length < 10 || isSubmitting}
                  className="w-full inline-flex items-center justify-center gap-3 px-6 py-3 bg-gradient-to-r from-primary to-primary/90 hover:from-primary/90 hover:to-primary text-primary-foreground font-semibold rounded-lg hover:shadow-lg hover:shadow-primary/25 disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-300 transform hover:scale-[1.02] active:scale-[0.98]"
            >
              {isSubmitting ? (
                <>
                      <div className="animate-spin rounded-full h-4 w-4 border-2 border-white border-t-transparent"></div>
                      <span>Sending...</span>
                </>
              ) : (
                <>
                  <Send className="w-4 h-4" />
                      <span>Send Feedback</span>
                </>
              )}
            </button>
              </div>
          </form>
          </div>
        </div>

        {toast && (
          <motion.div
            initial={{ opacity: 0, y: 50, scale: 0.9 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 50, scale: 0.9 }}
            className={`fixed bottom-4 right-4 z-50 max-w-sm ${
              toast.type === 'success' 
                ? 'bg-green-500 text-white' 
                : 'bg-red-500 text-white'
            } rounded-lg shadow-lg p-4 border-l-4 ${
              toast.type === 'success' 
                ? 'border-green-600' 
                : 'border-red-600'
            }`}
          >
            <div className="flex items-center gap-3">
              {toast.type === 'success' ? (
                <CheckCircle className="w-5 h-5" />
              ) : (
                <div className="w-5 h-5 rounded-full border-2 border-white flex items-center justify-center">
                  <span className="text-xs font-bold">!</span>
                </div>
              )}
              <p className="font-medium">{toast.message}</p>
            </div>
          </motion.div>
        )}
      </motion.div>
      </div>
    </div>
  );
}