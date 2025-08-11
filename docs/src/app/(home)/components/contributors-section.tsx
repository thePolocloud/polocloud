"use client"

import { motion } from "framer-motion";
import { Users, GitBranch, Star, Heart, Award, Github } from "lucide-react";
import { useState, useEffect } from "react";
import { GitHubContributor } from "@/lib/github";

interface ContributorStats {
    totalContributors: number;
    projectStars: number;
    projectForks: number;
    projectReleases: number;
}

export function ContributorsSection() {
    const [contributors, setContributors] = useState<GitHubContributor[]>([]);
    const [stats, setStats] = useState<ContributorStats>({
        totalContributors: 0,
        projectStars: 0,
        projectForks: 0,
        projectReleases: 0
    });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchContributors = async () => {
            try {
                setLoading(true);
                setError(null);

                const contributorsResponse = await fetch('/api/github-contributors');
                if (!contributorsResponse.ok) {
                    throw new Error('Failed to fetch contributors');
                }
                const contributorsData: GitHubContributor[] = await contributorsResponse.json();

                const statsResponse = await fetch('/api/github-stats');
                let statsData = null;
                if (statsResponse.ok) {
                    statsData = await statsResponse.json();
                }

                setContributors(contributorsData);
                setStats({
                    totalContributors: contributorsData.length,
                    projectStars: statsData?.stars || 61,
                    projectForks: statsData?.forks || 22,
                    projectReleases: statsData?.releases || 4
                });

            } catch (err) {
                console.error('Failed to fetch contributors:', err);
                setError('Failed to load contributors');

                setContributors([
                    {
                        login: 'HttpMarco',
                        id: 1,
                        avatar_url: 'https://github.com/HttpMarco.png',
                        contributions: 156,
                        commits: 156,
                        html_url: 'https://github.com/HttpMarco',
                        type: 'User'
                    }
                ]);
                setStats({
                    totalContributors: 1,
                    projectStars: 61,
                    projectForks: 22,
                    projectReleases: 4
                });
            } finally {
                setLoading(false);
            }
        };

        fetchContributors();
    }, []);

    const statsData = [
        { label: "Total Contributors", value: stats.totalContributors.toString(), icon: Users },
        { label: "Project Stars", value: stats.projectStars.toString(), icon: Star },
        { label: "Project Forks", value: stats.projectForks.toString(), icon: GitBranch },
        { label: "Releases", value: stats.projectReleases.toString(), icon: Heart }
    ];

    const containerVariants = {
        hidden: { opacity: 0 },
        visible: {
            opacity: 1,
            transition: {
                staggerChildren: 0.1,
                delayChildren: 0.2
            }
        }
    };

    const itemVariants = {
        hidden: {
            opacity: 0,
            y: 30,
            scale: 0.9,
            rotateX: -15
        },
        visible: {
            opacity: 1,
            y: 0,
            scale: 1,
            rotateX: 0,
            transition: {
                type: "spring" as const,
                stiffness: 200,
                damping: 20,
                duration: 0.6
            }
        }
    };

    const statsContainerVariants = {
        hidden: { opacity: 0 },
        visible: {
            opacity: 1,
            transition: {
                staggerChildren: 0.08,
                delayChildren: 0.1
            }
        }
    };

    const statsItemVariants = {
        hidden: {
            opacity: 0,
            y: 20,
            scale: 0.8,
            rotateY: -10
        },
        visible: {
            opacity: 1,
            y: 0,
            scale: 1,
            rotateY: 0,
            transition: {
                type: "spring" as const,
                stiffness: 300,
                damping: 25,
                duration: 0.5
            }
        }
    };

    return (
        <section className="relative py-32 overflow-hidden">
            <motion.div
                className="absolute top-0 left-0 right-0 h-64 bg-gradient-to-b from-background via-background/95 via-background/80 to-transparent"
                initial={{ opacity: 0, scaleY: 0 }}
                whileInView={{ opacity: 1, scaleY: 1 }}
                viewport={{ once: true, amount: 0.3 }}
                transition={{ duration: 1.5, ease: "easeOut" }}
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
                initial={{ opacity: 0, scale: 1.1 }}
                whileInView={{ opacity: 1, scale: 1 }}
                viewport={{ once: true, amount: 0.3 }}
                transition={{ duration: 1.5, delay: 0.6, ease: "easeOut" }}
            />

            <motion.div
                className="absolute top-20 left-10 w-32 h-32 bg-primary/5 rounded-full blur-3xl"
                initial={{ opacity: 0, scale: 0, rotate: 180, x: -100 }}
                whileInView={{ opacity: 1, scale: 1, rotate: 0, x: 0 }}
                viewport={{ once: true, amount: 0.3 }}
                animate={{
                    y: [0, -20, 0],
                    rotate: [0, 5, 0],
                }}
                transition={{
                    duration: 1.8,
                    delay: 0.8,
                    type: 'spring',
                    stiffness: 100,
                    y: { duration: 4, repeat: Infinity, ease: "easeInOut" },
                    rotate: { duration: 4, repeat: Infinity, ease: "easeInOut" }
                }}
            />
            <motion.div
                className="absolute bottom-20 right-10 w-40 h-40 bg-primary/5 rounded-full blur-3xl"
                initial={{ opacity: 0, scale: 0, rotate: -180, x: 100 }}
                whileInView={{ opacity: 1, scale: 1, rotate: 0, x: 0 }}
                viewport={{ once: true, amount: 0.3 }}
                animate={{
                    y: [0, 20, 0],
                    rotate: [0, -5, 0],
                }}
                transition={{
                    duration: 1.8,
                    delay: 1.0,
                    type: 'spring',
                    stiffness: 100,
                    y: { duration: 5, repeat: Infinity, ease: "easeInOut" },
                    rotate: { duration: 5, repeat: Infinity, ease: "easeInOut" }
                }}
            />
            <motion.div
                className="absolute top-1/2 left-1/4 w-24 h-24 bg-primary/3 rounded-full blur-2xl"
                initial={{ opacity: 0, scale: 0, y: 50 }}
                whileInView={{ opacity: 1, scale: 1, y: 0 }}
                viewport={{ once: true, amount: 0.3 }}
                animate={{
                    x: [0, 15, 0],
                    y: [0, -15, 0],
                }}
                transition={{
                    duration: 1.2,
                    delay: 1.2,
                    type: 'spring',
                    stiffness: 150,
                    x: { duration: 6, repeat: Infinity, ease: "easeInOut" },
                    y: { duration: 6, repeat: Infinity, ease: "easeInOut" }
                }}
            />

            <motion.div
                className="absolute top-1/3 right-1/3 w-20 h-20 bg-primary/4 rounded-full blur-2xl"
                initial={{ opacity: 0, scale: 0 }}
                whileInView={{ opacity: 1, scale: 1 }}
                viewport={{ once: true, amount: 0.3 }}
                animate={{
                    x: [0, -10, 0],
                    y: [0, 10, 0],
                }}
                transition={{
                    duration: 1.5,
                    delay: 1.4,
                    x: { duration: 7, repeat: Infinity, ease: "easeInOut" },
                    y: { duration: 7, repeat: Infinity, ease: "easeInOut" }
                }}
            />

            <div className="relative container mx-auto px-6">
                <motion.div
                    className="text-center mb-20"
                    initial={{ opacity: 0, y: 30 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    viewport={{ once: true, amount: 0.3 }}
                    transition={{ duration: 0.8, ease: 'easeOut' }}
                >
                    <h2 className="text-3xl md:text-4xl lg:text-5xl font-black mb-8 bg-gradient-to-r from-foreground via-primary to-foreground bg-clip-text text-transparent tracking-tight leading-tight">
                        Our Amazing Contributors
                    </h2>
                    <p className="text-base md:text-lg text-muted-foreground max-w-4xl mx-auto leading-relaxed">
                        Meet the talented developers, designers, and engineers who make PoloCloud possible.
                        <span className="block mt-2 text-sm md:text-base font-normal">
              Every contribution, big or small, helps us build the future of Minecraft hosting.
            </span>
                    </p>
                </motion.div>

                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    viewport={{ once: true, amount: 0.1 }}
                    transition={{ duration: 0.6, delay: 0.2 }}
                    className="grid grid-cols-2 md:grid-cols-4 gap-6 mb-16"
                >
                    {statsData.map((stat, index) => (
                        <motion.div
                            key={stat.label}
                            whileHover={{
                                scale: 1.05,
                                rotateY: 8,
                                rotateX: 2,
                                boxShadow: "0 20px 40px rgba(0, 0, 0, 0.15)",
                                transition: { type: "spring", stiffness: 300, damping: 20 }
                            }}
                            whileTap={{ scale: 0.98 }}
                            className="bg-card/30 backdrop-blur-sm border border-border/50 rounded-xl p-6 text-center hover:bg-card/40 hover:border-border/70 transition-all duration-300 cursor-pointer"
                        >
                            <motion.div
                                className="w-12 h-12 bg-gradient-to-br from-primary/20 to-primary/10 rounded-xl flex items-center justify-center mx-auto mb-4"
                                whileHover={{
                                    scale: 1.1,
                                    rotate: 5,
                                    boxShadow: "0 0 20px rgba(99, 102, 241, 0.3)"
                                }}
                                transition={{ type: "spring", stiffness: 300 }}
                            >
                                <stat.icon className="w-6 h-6 text-primary" />
                            </motion.div>
                            <div className="text-2xl md:text-3xl font-black text-foreground dark:text-white mb-2">
                                {stat.value}
                            </div>
                            <div className="text-sm text-muted-foreground dark:text-white/70 font-medium">
                                {stat.label}
                            </div>
                        </motion.div>
                    ))}
                </motion.div>

                {loading ? (
                    <motion.div
                        initial={{ opacity: 0, y: 20 }}
                        whileInView={{ opacity: 1, y: 0 }}
                        viewport={{ once: true, amount: 0.1 }}
                        transition={{ duration: 0.6, delay: 0.3 }}
                        className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"
                    >
                        {[1, 2, 3, 4, 5, 6].map((i) => (
                            <div
                                key={i}
                                className="bg-card/30 backdrop-blur-sm border border-border/50 rounded-xl p-6 animate-pulse"
                            >
                                <div className="flex items-center mb-4">
                                    <div className="w-16 h-16 bg-muted/50 rounded-full mr-4" />
                                    <div>
                                        <div className="w-24 h-4 bg-muted/50 rounded mb-2" />
                                        <div className="w-20 h-3 bg-muted/50 rounded" />
                                    </div>
                                </div>
                                <div className="text-center">
                                    <div className="w-16 h-4 bg-muted/50 rounded mb-1" />
                                    <div className="w-20 h-3 bg-muted/50 rounded" />
                                </div>
                            </div>
                        ))}
                    </motion.div>
                ) : (
                    <motion.div
                        initial={{ opacity: 0, y: 20 }}
                        whileInView={{ opacity: 1, y: 0 }}
                        viewport={{ once: true, amount: 0.1 }}
                        transition={{ duration: 0.6, delay: 0.3 }}
                        className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"
                    >
                        {contributors.map((contributor, index) => (
                            <motion.div
                                key={contributor.login}
                                className="bg-card/30 backdrop-blur-sm border border-border/50 rounded-xl p-6 hover:bg-card/40 hover:border-border/70 transition-all duration-300 group cursor-pointer relative"
                            >
                                <motion.div
                                    className="flex items-center mb-4"
                                    transition={{ type: "spring", stiffness: 400 }}
                                >
                                    <motion.div
                                        className="w-16 h-16 rounded-full overflow-hidden mr-4 ring-2 ring-primary/20 group-hover:ring-primary/40 transition-all duration-200 relative"
                                        transition={{ type: "spring", stiffness: 300 }}
                                    >
                                        <img
                                            src={contributor.avatar_url}
                                            alt={contributor.login}
                                            className="w-full h-full object-cover"
                                            onError={(e) => {
                                                const target = e.target as HTMLImageElement;
                                                target.src = `https://ui-avatars.com/api/?name=${contributor.login}&background=6366f1&color=fff&size=64`;
                                            }}
                                        />
                                    </motion.div>
                                    

                                    {index < 3 && (
                                        <div className="absolute -top-1 -right-1 w-6 h-5 rounded-full flex items-center justify-center text-xs font-bold text-white shadow-lg z-10">
                                            {index === 0 && (
                                                <div className="w-full h-full rounded-full bg-gradient-to-br from-yellow-400 to-yellow-600 flex items-center justify-center shadow-lg px-1 relative overflow-hidden">

                                                    <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/30 to-transparent animate-pulse" style={{
                                                        animationDuration: '2s',
                                                        animationIterationCount: 'infinite',
                                                        transform: 'translateX(-100%)',
                                                        animationName: 'shimmer'
                                                    }}></div>
                                                    <span className="text-white font-bold text-[10px] relative z-10">#1</span>
                                                </div>
                                            )}
                                            {index === 1 && (
                                                <div className="w-full h-full rounded-full bg-gradient-to-br from-gray-300 to-gray-500 flex items-center justify-center shadow-lg px-1 relative overflow-hidden">

                                                    <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/30 to-transparent animate-pulse" style={{
                                                        animationDuration: '2s',
                                                        animationIterationCount: 'infinite',
                                                        transform: 'translateX(-100%)',
                                                        animationName: 'shimmer'
                                                    }}></div>
                                                    <span className="text-white font-bold text-[10px] relative z-10">#2</span>
                                                </div>
                                            )}
                                            {index === 2 && (
                                                <div className="w-full h-full rounded-full bg-gradient-to-br from-amber-600 to-amber-800 flex items-center justify-center shadow-lg px-1 relative overflow-hidden">

                                                    <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/30 to-transparent animate-pulse" style={{
                                                        animationDuration: '2s',
                                                        animationIterationCount: 'infinite',
                                                        transform: 'translateX(-100%)',
                                                        animationName: 'shimmer'
                                                    }}></div>
                                                    <span className="text-white font-bold text-[10px] relative z-10">#3</span>
                                                </div>
                                            )}
                                        </div>
                                    )}
                                    <div>
                                        <h3 className="text-lg font-bold text-foreground dark:text-white group-hover:text-primary transition-colors duration-200">
                                            {contributor.login}
                                        </h3>
                                        <p className="text-sm text-muted-foreground dark:text-white/70">
                                            {contributor.commits} commits
                                        </p>
                                    </div>
                                </motion.div>

                                <div className="text-center">
                                    <motion.a
                                        href={contributor.html_url}
                                        target="_blank"
                                        rel="noopener noreferrer"
                                        className="inline-flex items-center gap-2 text-sm text-primary hover:text-primary/80 transition-colors duration-200"
                                        whileHover={{
                                            scale: 1.05,
                                            x: 5,
                                            textShadow: "0 0 8px rgba(99, 102, 241, 0.5)"
                                        }}
                                        whileTap={{ scale: 0.95 }}
                                        transition={{ type: "spring", stiffness: 400 }}
                                    >
                                        <Github className="w-4 h-4" />
                                        View Profile
                                    </motion.a>
                                </div>
                            </motion.div>
                        ))}
                    </motion.div>
                )}

                <motion.div
                    initial={{ opacity: 0, y: 30 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    viewport={{ once: true }}
                    transition={{ duration: 0.8, delay: 0.4 }}
                    className="text-center mt-16"
                >
                    <motion.div
                        whileHover={{
                            scale: 1.05,
                            boxShadow: "0 20px 40px rgba(99, 102, 241, 0.3)",
                            y: -2
                        }}
                        whileTap={{ scale: 0.95 }}
                        className="inline-flex items-center gap-3 px-8 py-4 bg-gradient-to-r from-primary to-primary/80 text-white rounded-xl font-semibold text-lg hover:from-primary/90 hover:to-primary/70 transition-all duration-300 shadow-lg hover:shadow-xl"
                    >
                        <motion.div
                            animate={{ rotate: [0, 10, -10, 0] }}
                            transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
                        >
                            <Github className="w-5 h-5" />
                        </motion.div>
                        Join Our Team
                    </motion.div>
                    <p className="text-sm text-muted-foreground dark:text-white/70 mt-4">
                        Want to contribute? We&apos;d love to have you on board!
                    </p>
                </motion.div>
            </div>
        </section>
    );
}