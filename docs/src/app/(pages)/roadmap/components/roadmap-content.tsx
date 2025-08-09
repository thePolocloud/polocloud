'use client';
import { useEffect, useState, useRef } from 'react';
import Link from 'next/link';
import { ArrowLeft, Calendar, Star, Package, Code, Server, Filter, X } from 'lucide-react';
import { motion } from 'framer-motion';

interface RoadmapItem {
  id: string;
  title: string;
  description: string;
  category: 'ui' | 'platforms' | 'bot' | 'addons';
  estimatedDate?: string;
  tags?: string[];
  assignees?: Array<{
    login: string;
    avatarUrl?: string;
  }>;
}

interface RoadmapColumn {
  id: string;
  title: string;
  color: string;
  items: RoadmapItem[];
}

const fallbackData: RoadmapColumn[] = [
  {
    id: 'no-status',
    title: 'No Status',
    color: 'border-red-500',
    items: []
  },
  {
    id: 'todo',
    title: 'Todo',
    color: 'border-gray-500',
    items: []
  },
  {
    id: 'in-progress',
    title: 'In Progress',
    color: 'border-amber-600',
    items: []
  },
  {
    id: 'quality-testing',
    title: 'Quality-Testing',
    color: 'border-purple-500',
    items: []
  },
  {
    id: 'done',
    title: 'Done',
    color: 'border-green-600',
    items: []
  }
];

const getCategoryIcon = (category: string) => {
  switch (category) {
    case 'ui': return <Star className="w-4 h-4" />;
    case 'platforms': return <Server className="w-4 h-4" />;
    case 'bot': return <Code className="w-4 h-4" />;
    case 'addons': return <Package className="w-4 h-4" />;
    default: return <Code className="w-4 h-4" />;
  }
};

const getCategoryColor = (category: string) => {
  switch (category) {
    case 'ui': return 'bg-blue-500/20 border-blue-500/30 text-blue-400';
    case 'platforms': return 'bg-purple-500/20 border-purple-500/30 text-purple-400';
    case 'bot': return 'bg-green-500/20 border-green-500/30 text-green-400';
    case 'addons': return 'bg-orange-500/20 border-orange-500/30 text-orange-400';
    default: return 'bg-gray-500/20 border-gray-500/30 text-gray-400';
  }
};

const getBorderColor = (columnId: string) => {
  switch (columnId) {
    case 'no-status':
      return '#dc2626';
    case 'todo':
      return '#9198a1';
    case 'in-progress':
      return '#9e6a03';
    case 'quality-testing':
      return '#8957e5';
    case 'done':
      return '#238636';
    default:
      return '#6b7280';
  }
};

const RoadmapCard = ({ item, index }: { item: RoadmapItem; index: number }) => {
  return (
    <div 
      className="bg-card/40 backdrop-blur-sm border border-border/40 rounded-xl p-4 mb-3 transition-all duration-500 group relative overflow-hidden"
      style={{
        animationDelay: `${index * 100}ms`
      }}
    >
      {item.tags && (
        <div className="flex flex-wrap gap-2 mb-2">
          {item.tags.map((tag, tagIndex) => (
            <span 
              key={tag} 
              className="px-2 py-0.5 rounded-full text-xs font-medium border transition-all duration-300 hover:scale-110 hover:shadow-md"
              style={{
                backgroundColor: tag === 'improvement' ? 'rgba(234, 187, 44, 0.15)' :
                              tag === 'new requirement' ? 'rgba(130, 247, 29, 0.15)' :
                              tag === 'prototype-5' ? 'rgba(136, 252, 202, 0.15)' :
                              tag === 'bug' ? 'rgba(236, 161, 168, 0.15)' :
                              'rgba(156, 163, 175, 0.15)',
                color: tag === 'improvement' ? 'rgb(234, 187, 44)' :
                       tag === 'new requirement' ? 'rgb(130, 247, 29)' :
                       tag === 'prototype-5' ? 'rgb(136, 252, 202)' :
                       tag === 'bug' ? 'rgb(236, 161, 168)' :
                       'rgb(156, 163, 175)',
                borderColor: tag === 'improvement' ? 'rgba(234, 187, 44, 0.3)' :
                            tag === 'new requirement' ? 'rgba(130, 247, 29, 0.3)' :
                            tag === 'prototype-5' ? 'rgba(136, 252, 202, 0.3)' :
                            tag === 'bug' ? 'rgba(236, 161, 168, 0.3)' :
                            'rgba(156, 163, 175, 0.3)',
                animationDelay: `${(index * 100) + (tagIndex * 50)}ms`
              }}
            >
              {tag}
            </span>
          ))}
        </div>
      )}
      <h3 className="font-bold text-foreground dark:text-white mb-2 text-sm leading-tight group-hover:text-primary transition-colors duration-300">
        {item.title}
      </h3>
      <p className="text-xs text-muted-foreground dark:text-white/70 mb-3 leading-relaxed">
        {item.description}
      </p>

      <div className="flex items-center justify-between">
        {item.estimatedDate && (
          <div className="flex items-center gap-1.5 text-xs text-muted-foreground dark:text-white/60 bg-muted/30 px-2 py-1 rounded-md transition-all duration-300 hover:bg-muted/50">
            <Calendar className="w-3 h-3" />
            <span className="font-medium">{item.estimatedDate}</span>
          </div>
        )}

        {item.assignees && item.assignees.length > 0 && (
          <div className="flex items-center gap-1">
            {item.assignees.slice(0, 3).map((assignee, assigneeIndex) => (
              <div key={assignee.login} className="flex items-center gap-1">
                <div className="relative">
                  <img 
                    src={assignee.avatarUrl || `https://github.com/${assignee.login}.png`}
                    alt={assignee.login}
                    className="w-5 h-5 rounded-full border border-border/50 transition-all duration-300 hover:scale-110 hover:border-primary/50"
                    title={assignee.login}
                    onError={(e) => {
                      const target = e.target as HTMLImageElement;
                      target.style.display = 'none';
                      const fallback = target.nextElementSibling as HTMLElement;
                      if (fallback) fallback.style.display = 'flex';
                    }}
                  />
                  <div className="w-5 h-5 rounded-full bg-muted flex items-center justify-center absolute top-0 left-0" style={{ display: 'none' }}>
                    <span className="text-xs font-medium text-muted-foreground">
                      {assignee.login.charAt(0).toUpperCase()}
                    </span>
                  </div>
                </div>
              </div>
            ))}
            <div className="flex items-center gap-1">
              <span className="text-xs text-muted-foreground font-medium transition-colors duration-300 group-hover:text-foreground">
                {item.assignees.slice(0, 2).map(assignee => assignee.login).join(', ')}
                {item.assignees.length > 2 && ` +${item.assignees.length - 2}`}
              </span>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

const RoadmapColumn = ({ column, columnIndex, totalItems }: { column: RoadmapColumn; columnIndex: number; totalItems: number }) => {
  return (
    <div 
      className="flex flex-col h-full"
      style={{
        animationDelay: `${columnIndex * 200}ms`,
        animation: 'slideInLeft 0.8s ease-out forwards'
      }}
    >
      <div
        className="mb-6 p-4 bg-card/50 backdrop-blur-sm border rounded-xl shadow-lg transition-all duration-500 hover:shadow-xl hover:scale-105 relative"
        style={{
          borderColor: getBorderColor(column.id),
          boxShadow: `inset 0 0 30px ${getBorderColor(column.id)}25, 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)`
        }}
      >
        <div
            className="absolute inset-0 rounded-xl opacity-20"
          style={{
            background: `radial-gradient(circle at center, ${getBorderColor(column.id)}30, transparent 60%)`
          }}
        />

        <div className="relative z-10">
          <div className="flex items-center justify-between mb-3">
            <h2 className="font-bold text-foreground dark:text-white text-sm uppercase tracking-wider">
              {column.title}
            </h2>
            <div className="flex items-center gap-2">
              <span className="text-lg font-bold" style={{ color: getBorderColor(column.id) }}>
                {column.items.length}
              </span>
              <span className="text-xs text-muted-foreground dark:text-white/60 font-medium">
                items
              </span>
            </div>
          </div>
          
          {/* Progress Bar */}
          <div className="mb-3">
            <div className="flex items-center justify-between text-xs mb-1">
              <span className="text-muted-foreground">Progress</span>
              <span className="font-medium" style={{ color: getBorderColor(column.id) }}>
                {totalItems > 0 ? Math.round((column.items.length / totalItems) * 100) : 0}%
              </span>
            </div>
            <div className="w-full bg-muted/30 rounded-full h-2 overflow-hidden">
              <motion.div 
                className="h-2 rounded-full"
                style={{ 
                  backgroundColor: getBorderColor(column.id),
                  width: totalItems > 0 ? `${(column.items.length / totalItems) * 100}%` : '0%'
                }}
                initial={{ width: 0 }}
                animate={{ width: totalItems > 0 ? `${(column.items.length / totalItems) * 100}%` : '0%' }}
                transition={{ duration: 1, delay: columnIndex * 0.2 }}
              />
            </div>
          </div>
        </div>
      </div>

      <div className="flex-1 space-y-3 max-h-[600px] overflow-y-auto pr-2 scrollbar-thin scrollbar-thumb-border/50 scrollbar-track-transparent">
        {column.items.map((item, index) => (
          <RoadmapCard key={item.id} item={item} index={index} />
        ))}
      </div>
    </div>
  );
};

export function RoadmapContent() {
  const [isVisible, setIsVisible] = useState(false);
  const [roadmapData, setRoadmapData] = useState<RoadmapColumn[]>(fallbackData);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeFilters, setActiveFilters] = useState<string[]>([]);
  const [isFilterModalOpen, setIsFilterModalOpen] = useState(false);
  const [showQuickStats, setShowQuickStats] = useState(false);
  const contentRef = useRef<HTMLElement>(null);

  // Calculate quick stats
  const totalItems = roadmapData.reduce((total, column) => total + column.items.length, 0);
  const completedItems = roadmapData.find(col => col.id === 'done')?.items.length || 0;
  const inProgressItems = roadmapData.find(col => col.id === 'in-progress')?.items.length || 0;
  const completionRate = totalItems > 0 ? Math.round((completedItems / totalItems) * 100) : 0;

  useEffect(() => {
    const style = document.createElement('style');
    style.textContent = `
      @keyframes slideInUp {
        from {
          opacity: 0;
          transform: translateY(30px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }
      
      @keyframes slideInLeft {
        from {
          opacity: 0;
          transform: translateX(-30px);
        }
        to {
          opacity: 1;
          transform: translateX(0);
        }
      }
      
      @keyframes fadeInScale {
        from {
          opacity: 0;
          transform: scale(0.8);
        }
        to {
          opacity: 1;
          transform: scale(1);
        }
      }
      
      @keyframes pulse {
        0%, 100% {
          opacity: 1;
        }
        50% {
          opacity: 0.5;
        }
      }
      
      @keyframes shimmer {
        0% {
          background-position: -200px 0;
        }
        100% {
          background-position: calc(200px + 100%) 0;
        }
      }
      
      .loading-shimmer {
        background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
        background-size: 200px 100%;
        animation: shimmer 1.5s infinite;
      }
      
      @keyframes modalFadeIn {
        from {
          opacity: 0;
        }
        to {
          opacity: 1;
        }
      }
      
      @keyframes modalSlideIn {
        from {
          opacity: 0;
          transform: scale(0.9) translateY(-20px);
        }
        to {
          opacity: 1;
          transform: scale(1) translateY(0);
        }
      }
    `;
    document.head.appendChild(style);
    return () => {
      if (document.head.contains(style)) {
        document.head.removeChild(style);
      }
    };
  }, []);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsVisible(true);
        }
      },
      { threshold: 0.1, rootMargin: '0px 0px -100px 0px' }
    );

    if (contentRef.current) {
      observer.observe(contentRef.current);
    }

    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    const fetchRoadmapData = async () => {
    try {
      setIsLoading(true);
      setError(null);
      
        const response = await fetch('/api/github-projects');
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      const data = await response.json();
      
      if (data.error) {
        throw new Error(data.error);
      }
      
      setRoadmapData(data);
    } catch (err) {
      console.error('Failed to fetch roadmap data:', err);
      setError(err instanceof Error ? err.message : 'Failed to load roadmap data');
      setRoadmapData(fallbackData);
    } finally {
      setIsLoading(false);
    }
  };

    fetchRoadmapData();
  }, []);

  const toggleFilter = (tag: string) => {
    setActiveFilters(prev => 
      prev.includes(tag) 
        ? prev.filter(t => t !== tag)
        : [...prev, tag]
    );
  };

  const clearAllFilters = () => {
    setActiveFilters([]);
  };

  const filteredData = roadmapData.map(column => ({
    ...column,
    items: column.items.filter(item => {
      if (activeFilters.length === 0) return true;
      return item.tags?.some(tag => activeFilters.includes(tag));
    })
  }));

  const allTags = ['improvement', 'new requirement', 'prototype-5', 'bug'];

  return (
    <>
      <section ref={contentRef} className="relative py-12 overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-b from-background via-muted/5 to-muted/5" />

        <div className="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.02)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.02)_1px,transparent_1px)] bg-[size:50px_50px] dark:bg-[linear-gradient(rgba(255,255,255,0.01)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.01)_1px,transparent_1px)]" />

        <div className="relative container mx-auto px-6">
          <div className={`mb-8 transition-all duration-1000 ease-out ${
            isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
          }`}>
            <div className="flex justify-between items-center">
              <Link 
                href="/"
                className="inline-flex items-center gap-2 px-4 py-2 bg-card/50 hover:bg-card border border-border/50 rounded-lg text-sm font-medium transition-all duration-300 hover:scale-105 backdrop-blur-sm"
              >
                <ArrowLeft className="w-4 h-4" />
                Back to Home
              </Link>
            </div>
          </div>

          <div className={`text-center mb-16 transition-all duration-1000 ease-out ${
            isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
          }`}>
            <h1 className={`text-4xl md:text-5xl font-bold mb-6 bg-gradient-to-r from-foreground to-muted-foreground bg-clip-text text-transparent transition-all duration-1000 delay-200 ${
              isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
            }`}>
              Development Roadmap
            </h1>
            <p className={`text-xl text-muted-foreground max-w-3xl mx-auto leading-relaxed transition-all duration-1000 delay-400 ${
              isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
            }`}>
              Track our progress and see what&apos;s coming next for PoloCloud
            </p>
          </div>

          {isLoading && (
            <div className={`text-center mb-12 transition-all duration-1000 delay-500 ${
              isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
            }`}>
              <div className="inline-flex items-center gap-3 px-6 py-3 bg-card/40 backdrop-blur-sm border border-border/40 rounded-xl">
                <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-foreground"></div>
                <span className="text-sm font-medium">Loading roadmap data...</span>
              </div>
            </div>
          )}

          {error && (
            <div className={`text-center mb-12 transition-all duration-1000 delay-500 ${
              isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
            }`}>
              <div className="inline-flex items-center gap-3 px-6 py-3 bg-red-500/10 backdrop-blur-sm border border-red-500/20 rounded-xl">
                <span className="text-sm font-medium text-red-400">Error: {error}</span>
              </div>
            </div>
          )}

          <div className={`mb-12 transition-all duration-1000 delay-500 ${
            isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
          }`}>
            <div className="bg-card/60 backdrop-blur-sm border border-border/50 rounded-2xl p-8 shadow-lg relative overflow-hidden">
              <div className="absolute inset-0 rounded-2xl bg-gradient-to-br from-muted/20 via-background/50 to-muted/20"></div>

              <div className="relative z-10">
                <div className="flex items-center justify-between mb-8">
                  <div>
                    <h3 className="font-bold text-foreground dark:text-white text-xl mb-2">
                      Categories
                    </h3>
                    <p className="text-sm text-muted-foreground">
                      Filter and organize roadmap items by tags
                    </p>
                  </div>
                  <button
                    onClick={() => setIsFilterModalOpen(true)}
                    className="inline-flex items-center gap-3 px-6 py-3 bg-primary/10 hover:bg-primary/20 border border-primary/30 hover:border-primary/50 rounded-xl text-sm font-medium transition-all duration-300 hover:shadow-lg hover:scale-105 backdrop-blur-sm"
                  >
                    <Filter className="w-4 h-4" />
                    <span>Filter Tags</span>
                    {activeFilters.length > 0 && (
                      <span className="bg-primary text-primary-foreground text-xs rounded-full px-2 py-1 min-w-[24px] flex items-center justify-center font-bold">
                        {activeFilters.length}
                      </span>
                    )}
                  </button>
                </div>
                
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  <div className="flex flex-col items-center p-4 rounded-xl bg-card/40 border border-border/40 hover:border-border/60 transition-all duration-300 hover:scale-105 group">
                    <span className="px-4 py-2 rounded-full text-sm font-medium border mb-2" style={{ 
                      backgroundColor: 'rgba(234, 187, 44, 0.15)',
                      color: 'rgb(234, 187, 44)',
                      borderColor: 'rgba(234, 187, 44, 0.3)'
                    }}>improvement</span>
                    <span className="text-xs text-muted-foreground text-center">Enhancements & improvements</span>
                  </div>
                  
                  <div className="flex flex-col items-center p-4 rounded-xl bg-card/40 border border-border/40 hover:border-border/60 transition-all duration-300 hover:scale-105 group">
                    <span className="px-4 py-2 rounded-full text-sm font-medium border mb-2" style={{ 
                      backgroundColor: 'rgba(130, 247, 29, 0.15)',
                      color: 'rgb(130, 247, 29)',
                      borderColor: 'rgba(130, 247, 29, 0.3)'
                    }}>new requirement</span>
                    <span className="text-xs text-muted-foreground text-center">New features & requirements</span>
                  </div>
                  
                  <div className="flex flex-col items-center p-4 rounded-xl bg-card/40 border border-border/40 hover:border-border/60 transition-all duration-300 hover:scale-105 group">
                    <span className="px-4 py-2 rounded-full text-sm font-medium border mb-2" style={{ 
                      backgroundColor: 'rgba(136, 252, 202, 0.15)',
                      color: 'rgb(136, 252, 202)',
                      borderColor: 'rgba(136, 252, 202, 0.3)'
                    }}>prototype-5</span>
                    <span className="text-xs text-muted-foreground text-center">Prototype & experimental</span>
                  </div>
                  
                  <div className="flex flex-col items-center p-4 rounded-xl bg-card/40 border border-border/40 hover:border-border/60 transition-all duration-300 hover:scale-105 group">
                    <span className="px-4 py-2 rounded-full text-sm font-medium border mb-2" style={{ 
                      backgroundColor: 'rgba(236, 161, 168, 0.15)',
                      color: 'rgb(236, 161, 168)',
                      borderColor: 'rgba(236, 161, 168, 0.3)'
                    }}>bug</span>
                    <span className="text-xs text-muted-foreground text-center">Bug fixes & issues</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div className={`transition-all duration-1000 delay-600 ${
            isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
          }`}>
            <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-5 gap-8">
              {filteredData.map((column, index) => (
                <div
                  key={column.id}
                  className={`transition-all duration-1000 delay-${(index + 1) * 100} ${
                    isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
                  }`}
                >
                  <RoadmapColumn column={column} columnIndex={index} totalItems={totalItems} />
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {isFilterModalOpen && (
        <motion.div 
          className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
        >
          <motion.div 
            className="bg-card/95 backdrop-blur-sm border border-border/40 rounded-2xl p-6 shadow-2xl max-w-md w-full max-h-[80vh] overflow-y-auto relative overflow-hidden"
            initial={{ opacity: 0, scale: 0.9, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.9, y: 20 }}
            transition={{ type: "spring", stiffness: 300, damping: 30 }}
          >
            {/* Cool Background Effect */}
            <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-primary/5 opacity-50" />
            <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-primary via-primary/50 to-primary" />
            
            <div className="relative z-10">
              <div className="flex items-center justify-between mb-6">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 bg-primary/10 rounded-lg flex items-center justify-center">
                    <Filter className="w-4 h-4 text-primary" />
                  </div>
                  <h3 className="text-lg font-bold text-foreground">Filter by Tags</h3>
                </div>
                <button
                  onClick={() => setIsFilterModalOpen(false)}
                  className="w-8 h-8 rounded-lg hover:bg-muted/50 flex items-center justify-center text-muted-foreground hover:text-foreground transition-all duration-200 hover:scale-110"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              {activeFilters.length > 0 && (
                <motion.div 
                  className="mb-6 p-4 bg-gradient-to-r from-primary/10 to-primary/5 rounded-xl border border-primary/20"
                  initial={{ opacity: 0, y: -10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.1 }}
                >
                  <div className="text-sm text-primary font-medium mb-3 flex items-center gap-2">
                    <div className="w-2 h-2 bg-primary rounded-full animate-pulse" />
                    Active filters ({activeFilters.length})
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {activeFilters.map((tag, index) => (
                      <motion.span
                        key={tag}
                        className="inline-flex items-center gap-1 px-3 py-1.5 rounded-full text-sm font-medium border backdrop-blur-sm"
                        initial={{ opacity: 0, scale: 0.8 }}
                        animate={{ opacity: 1, scale: 1 }}
                        transition={{ delay: index * 0.1 }}
                        style={{
                          backgroundColor: tag === 'improvement' ? 'rgba(234, 187, 44, 0.2)' :
                                            tag === 'new requirement' ? 'rgba(130, 247, 29, 0.2)' :
                                            tag === 'prototype-5' ? 'rgba(136, 252, 202, 0.2)' :
                                            tag === 'bug' ? 'rgba(236, 161, 168, 0.2)' :
                                            'rgba(156, 163, 175, 0.2)',
                          color: tag === 'improvement' ? 'rgb(234, 187, 44)' :
                                 tag === 'new requirement' ? 'rgb(130, 247, 29)' :
                                 tag === 'prototype-5' ? 'rgb(136, 252, 202)' :
                                 tag === 'bug' ? 'rgb(236, 161, 168)' :
                                 'rgb(156, 163, 175)',
                          borderColor: tag === 'improvement' ? 'rgba(234, 187, 44, 0.4)' :
                                        tag === 'new requirement' ? 'rgba(130, 247, 29, 0.4)' :
                                        tag === 'prototype-5' ? 'rgba(136, 252, 202, 0.4)' :
                                        tag === 'bug' ? 'rgba(236, 161, 168, 0.4)' :
                                        'rgba(156, 163, 175, 0.4)'
                        }}
                      >
                        {tag}
                        <button
                          onClick={() => toggleFilter(tag)}
                          className="hover:bg-black/10 rounded-full p-0.5 transition-all duration-200 hover:scale-110"
                        >
                          <X className="w-3 h-3" />
                        </button>
                      </motion.span>
                    ))}
                  </div>
                  <button
                    onClick={clearAllFilters}
                    className="mt-3 text-xs text-muted-foreground hover:text-foreground transition-colors duration-200 flex items-center gap-1 hover:scale-105"
                  >
                    <X className="w-3 h-3" />
                    Clear all filters
                  </button>
                </motion.div>
              )}

              <div className="space-y-2 mb-6">
                {allTags.map((tag, index) => (
                  <motion.label
                    key={tag}
                    className="flex items-center gap-3 p-4 rounded-xl hover:bg-muted/30 transition-all duration-200 cursor-pointer group border border-transparent hover:border-border/30"
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: index * 0.05 }}
                    whileHover={{ scale: 1.02 }}
                  >
                    <div className="relative">
                      <input
                        type="checkbox"
                        checked={activeFilters.includes(tag)}
                        onChange={() => toggleFilter(tag)}
                        className="w-5 h-5 rounded border-border/50 text-primary focus:ring-primary focus:ring-2 focus:ring-offset-2 transition-all duration-200"
                      />
                      {activeFilters.includes(tag) && (
                        <motion.div
                          className="absolute inset-0 flex items-center justify-center"
                          initial={{ scale: 0 }}
                          animate={{ scale: 1 }}
                          transition={{ type: "spring", stiffness: 500, damping: 30 }}
                        >
                          <div className="w-2 h-2 bg-primary rounded-full" />
                        </motion.div>
                      )}
                    </div>
                    <span
                      className="flex-1 text-sm font-medium group-hover:scale-105 transition-transform duration-200"
                      style={{
                        color: tag === 'improvement' ? 'rgb(234, 187, 44)' :
                               tag === 'new requirement' ? 'rgb(130, 247, 29)' :
                               tag === 'prototype-5' ? 'rgb(136, 252, 202)' :
                               tag === 'bug' ? 'rgb(236, 161, 168)' :
                               'rgb(156, 163, 175)'
                      }}
                    >
                      {tag}
                    </span>
                    <motion.div
                      className="w-4 h-4 rounded-full group-hover:scale-110 transition-transform duration-200"
                      style={{
                        backgroundColor: tag === 'improvement' ? 'rgb(234, 187, 44)' :
                                         tag === 'new requirement' ? 'rgb(130, 247, 29)' :
                                         tag === 'prototype-5' ? 'rgb(136, 252, 202)' :
                                         tag === 'bug' ? 'rgb(236, 161, 168)' :
                                         'rgb(156, 163, 175)'
                      }}
                      whileHover={{ rotate: 180 }}
                      transition={{ duration: 0.3 }}
                    />
                  </motion.label>
                ))}
              </div>

              <div className="pt-4 border-t border-border/30">
                <motion.div 
                  className="text-sm text-muted-foreground mb-4 flex items-center gap-2"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  transition={{ delay: 0.3 }}
                >
                  <div className="w-2 h-2 bg-primary rounded-full" />
                  Showing {filteredData.reduce((total, column) => total + column.items.length, 0)} items
                  {activeFilters.length > 0 && ` (filtered by ${activeFilters.length} tag${activeFilters.length > 1 ? 's' : ''})`}
                </motion.div>
                <motion.button
                  onClick={() => setIsFilterModalOpen(false)}
                  className="w-full px-4 py-3 bg-gradient-to-r from-primary to-primary/80 hover:from-primary/90 hover:to-primary/70 text-white rounded-xl font-medium transition-all duration-300 hover:scale-105 hover:shadow-lg"
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                >
                  Apply Filters
                </motion.button>
              </div>
            </div>
          </motion.div>
        </motion.div>
      )}
    </>
  );
} 