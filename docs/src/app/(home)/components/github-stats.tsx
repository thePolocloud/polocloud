'use client';

import { Star, GitFork, Download, Package, GitCommit } from 'lucide-react';
import { useEffect, useState } from 'react';

interface GitHubStats {
    stars: number;
    forks: number;
    releases: number;
    downloads: number;
    commits: number;
    lastUpdated: string;
}

interface StatCardProps {
    icon: React.ReactNode;
    value: number;
    label: string;
}

const StatCard = ({ icon, value, label }: StatCardProps) => {
    const [displayValue, setDisplayValue] = useState(0);
    const [shouldStartCounting, setShouldStartCounting] = useState(false);

    useEffect(() => {
        const startTimer = setTimeout(() => {
            setShouldStartCounting(true);
        }, 1000);

        return () => clearTimeout(startTimer);
    }, []);

    useEffect(() => {
        if (!shouldStartCounting) return;

        const duration = 800;
        const steps = 30;
        const increment = value / steps;
        const stepDuration = duration / steps;

        let currentValue = 0;
        const timer = setInterval(() => {
            currentValue += increment;
            if (currentValue >= value) {
                setDisplayValue(value);
                clearInterval(timer);
            } else {
                setDisplayValue(Math.floor(currentValue));
            }
        }, stepDuration);

        return () => clearInterval(timer);
    }, [value, shouldStartCounting]);

    return (
        <div className="group flex flex-col items-center justify-center text-center transition-all duration-300 hover:scale-105 transform">
            <div className="flex items-center justify-center w-16 h-16 bg-primary/10 rounded-full border border-primary/20 mb-4 group-hover:scale-110 transition-transform duration-300 text-primary">
                {icon}
            </div>
            <div className="flex flex-col items-center">
        <span className="text-3xl font-bold text-foreground dark:text-white mb-2">
          {displayValue.toLocaleString()}
        </span>
                <span className="text-sm text-muted-foreground font-medium uppercase tracking-wide">
          {label}
        </span>
            </div>
        </div>
    );
};

export function GitHubStatsComponent() {
    const [stats, setStats] = useState<GitHubStats | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchStats = async () => {
            try {
                setLoading(true);
                setError(null);

                let response;
                try {
                    response = await fetch('/api/github-stats');
                } catch (apiError) {
                    console.log('API route failed, using fallback data');
                    throw new Error('API not available');
                }

                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }

                const data = await response.json();

                if (data.error) {
                    throw new Error(data.error);
                }

                setStats(data);
            } catch (err) {
                console.error('Failed to fetch GitHub stats:', err);
                setError('Failed to load statistics');

                setStats({
                    stars: 61,
                    forks: 22,
                    releases: 4,
                    downloads: 0,
                    commits: 3206,
                    lastUpdated: new Date().toISOString(),
                });
            } finally {
                setLoading(false);
            }
        };

        fetchStats();
    }, []);

    if (loading) {
        return (
            <div className="flex flex-wrap justify-start gap-12 mb-16">
                {[1, 2, 3, 4, 5].map((i) => (
                    <div key={i} className="flex flex-col items-center justify-center text-center animate-pulse">
                        <div className="w-16 h-16 bg-muted/50 rounded-full mb-4" />
                        <div className="flex flex-col items-center gap-2">
                            <div className="w-20 h-10 bg-muted/50 rounded" />
                            <div className="w-24 h-4 bg-muted/50 rounded" />
                        </div>
                    </div>
                ))}
            </div>
        );
    }

    if (error || !stats) {
        return (
            <div className="flex flex-wrap justify-start gap-12 mb-16">
                <div className="flex flex-col items-center justify-center text-center">
                    <div className="flex items-center justify-center w-16 h-16 bg-primary/10 rounded-full border border-primary/20 mb-4 text-primary">
                        <Star className="w-8 h-8" />
                    </div>
                    <div className="flex flex-col items-center">
                        <span className="text-3xl font-bold text-foreground dark:text-white mb-2">61</span>
                        <span className="text-sm text-muted-foreground font-medium uppercase tracking-wide">Stars</span>
                    </div>
                </div>
                <div className="flex flex-col items-center justify-center text-center">
                    <div className="flex items-center justify-center w-16 h-16 bg-primary/10 rounded-full border border-primary/20 mb-4 text-primary">
                        <GitFork className="w-8 h-8" />
                    </div>
                    <div className="flex flex-col items-center">
                        <span className="text-3xl font-bold text-foreground dark:text-white mb-2">22</span>
                        <span className="text-sm text-muted-foreground font-medium uppercase tracking-wide">Forks</span>
                    </div>
                </div>
                <div className="flex flex-col items-center justify-center text-center">
                    <div className="flex items-center justify-center w-16 h-16 bg-primary/10 rounded-full border border-primary/20 mb-4 text-primary">
                        <Package className="w-8 h-8" />
                    </div>
                    <div className="flex flex-col items-center">
                        <span className="text-3xl font-bold text-foreground dark:text-white mb-2">4</span>
                        <span className="text-sm text-muted-foreground font-medium uppercase tracking-wide">Releases</span>
                    </div>
                </div>
                <div className="flex flex-col items-center justify-center text-center">
                    <div className="flex items-center justify-center w-16 h-16 bg-primary/10 rounded-full border border-primary/20 mb-4 text-primary">
                        <Download className="w-8 h-8" />
                    </div>
                    <div className="flex flex-col items-center">
                        <span className="text-3xl font-bold text-foreground dark:text-white mb-2">0</span>
                        <span className="text-sm text-muted-foreground font-medium uppercase tracking-wide">Downloads</span>
                    </div>
                </div>
                <div className="flex flex-col items-center justify-center text-center">
                    <div className="flex items-center justify-center w-16 h-16 bg-primary/10 rounded-full border border-primary/20 mb-4 text-primary">
                        <GitCommit className="w-8 h-8" />
                    </div>
                    <div className="flex flex-col items-center">
                        <span className="text-3xl font-bold text-foreground dark:text-white mb-2">3206</span>
                        <span className="text-sm text-muted-foreground font-medium uppercase tracking-wide">Commits</span>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="flex flex-wrap justify-start gap-12 mb-16">
            <StatCard icon={<Star className="w-8 h-8" />} value={stats.stars} label="Stars" />
            <StatCard icon={<GitFork className="w-8 h-8" />} value={stats.forks} label="Forks" />
            <StatCard icon={<Package className="w-8 h-8" />} value={stats.releases} label="Releases" />
            <StatCard icon={<Download className="w-8 h-8" />} value={stats.downloads} label="Downloads" />
            <StatCard icon={<GitCommit className="w-8 h-8" />} value={stats.commits} label="Commits" />
        </div>
    );
} 