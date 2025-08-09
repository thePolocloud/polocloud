'use client';
import { Clock, Sparkles, ArrowRight, Puzzle, Users, BarChart3, Code, ExternalLink, MessageCircle } from 'lucide-react';
import Link from 'next/link';
import { useEffect, useState, useRef } from 'react';
import { motion } from 'framer-motion';

export function ComingSoonSection() {
  const [isVisible, setIsVisible] = useState(false);
  const [isBuilding, setIsBuilding] = useState(false);
  const sectionRef = useRef<HTMLElement>(null);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsVisible(true);
          setTimeout(() => {
            setIsBuilding(true);
          }, 500);
        }
      },
      { threshold: 0.1, rootMargin: '0px 0px -100px 0px' }
    );

    if (sectionRef.current) {
      observer.observe(sectionRef.current);
    }

    return () => observer.disconnect();
  }, []);

  const features = [
    {
      icon: <Puzzle className="w-6 h-6 text-cyan-400" />,
      title: 'Official Addons',
      description: 'Expand your server with our official addons including advanced plugins and performance boosters.',
      version: 'v3.0.0-pre-5',
    },
    {
      icon: <Users className="w-6 h-6 text-cyan-400" />,
      title: 'Partner Programm',
      description: 'Join our exclusive partner program and earn rewards while helping grow the PoloCloud community.',
      version: 'v3.0.0-pre-5',
    },
    {
      icon: <BarChart3 className="w-6 h-6 text-cyan-400" />,
      title: 'More Stats',
      description: 'Get detailed analytics, player statistics, server performance metrics and advanced reporting tools.',
      version: 'v3.0.0-pre-5',
    },
  ];

  return (
    <section className="relative py-32 overflow-hidden">
      <motion.div 
        className="absolute top-0 left-0 right-0 h-48 bg-gradient-to-b from-background via-background/95 via-background/80 to-transparent"
        initial={{ opacity: 0 }}
        whileInView={{ opacity: 1 }}
        viewport={{ once: true, amount: 0.3 }}
        transition={{ duration: 1.2 }}
      />
      <motion.div 
        className="absolute inset-0 bg-gradient-to-b from-background via-muted/5 to-muted/5"
        initial={{ opacity: 0 }}
        whileInView={{ opacity: 1 }}
        viewport={{ once: true, amount: 0.3 }}
        transition={{ duration: 1.2, delay: 0.3 }}
      />
      <motion.div 
        className="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.02)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.02)_1px,transparent_1px)] bg-[size:50px_50px] dark:bg-[linear-gradient(rgba(255,255,255,0.01)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.01)_1px,transparent_1px)]"
        initial={{ opacity: 0 }}
        whileInView={{ opacity: 1 }}
        viewport={{ once: true, amount: 0.3 }}
        transition={{ duration: 1.2, delay: 0.6 }}
      />
      
      <div className="container mx-auto px-6 max-w-6xl relative z-10">
        <div className="text-center mb-16">
          <h2 className="text-5xl md:text-6xl font-black text-cyan-400 mb-6 leading-tight">Coming Soon</h2>
          <p className="text-lg md:text-xl text-muted-foreground max-w-2xl mx-auto mb-2">
            We&apos;re working hard to bring you amazing new features. Stay tuned for exciting updates!
          </p>
        </div>

        <div className="grid gap-8 md:grid-cols-2 lg:grid-cols-3 mb-16">
          {features.map((feature, index) => (
            <div 
              key={feature.title}
              className="bg-card/30 backdrop-blur-sm border border-border/50 rounded-xl p-6 hover:bg-card/40 hover:border-border/70 transition-all duration-300 hover:shadow-lg hover:scale-[1.02] group relative overflow-hidden"
            >
              <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-primary/5 opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
              
              <div className="relative z-10">
                <div className="w-12 h-12 bg-card/60 rounded-lg flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-300">
                  {feature.icon}
                </div>
                <h3 className="font-black text-foreground dark:text-white mb-3 text-lg md:text-xl leading-tight">
                  {feature.title}
                </h3>
                <p className="text-sm md:text-base text-muted-foreground dark:text-white/70 leading-relaxed font-light">
                  {feature.description}
                </p>
              </div>
            </div>
          ))}
        </div>

        <div className="flex flex-col items-center justify-center bg-[#10131a] border border-[#23272f] rounded-2xl p-10 max-w-2xl mx-auto shadow-lg">
          <h3 className="text-white text-2xl font-bold mb-2">Stay Updated</h3>
          <p className="text-[#b0b8c1] text-base mb-6 text-center">
            Be the first to know when these features are available. Join our community for early access and exclusive updates.
            </p>
            <a
              href="https://discord.com/invite/mQ39S2EWNV"
              target="_blank"
              rel="noopener noreferrer"
            className="inline-flex items-center gap-3 px-8 py-4 bg-cyan-500 hover:bg-cyan-400 text-white rounded-xl font-semibold text-lg transition-all duration-300 shadow-[0_0_20px_rgba(0,200,255,0.15)] hover:shadow-[0_0_30px_rgba(0,200,255,0.25)] hover:scale-105 group"
            >
              <MessageCircle className="w-5 h-5 group-hover:rotate-12 transition-transform" />
              Join Discord
              <ExternalLink className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
            </a>
          </div>
        </div>
    </section>
  );
} 