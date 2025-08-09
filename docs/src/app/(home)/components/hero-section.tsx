'use client';

import Link from 'next/link';
import { ArrowRight, Github, BookOpen } from 'lucide-react';
import { GitHubStatsComponent } from './github-stats';
import { useEffect, useState } from 'react';
import { TextGenerateEffect } from '@/utils/text-generate-effect';
import { motion } from 'framer-motion';

interface TerminalLine {
    time: string;
    level: string;
    message: string;
    color: string;
    highlight?: string;
    highlightColor?: string;
}

export function HeroSection() {
    const [isVisible, setIsVisible] = useState(false);
    const [showCommand, setShowCommand] = useState(false);
    const [showLogsPhase1, setShowLogsPhase1] = useState(false);
    const [showLogsPhase2, setShowLogsPhase2] = useState(false);
    const [showLogsPhase3, setShowLogsPhase3] = useState(false);
    const [typedText, setTypedText] = useState('');
    const [currentIndex, setCurrentIndex] = useState(0);
    const [latestVersion, setLatestVersion] = useState('v3.0.0-pre.4-SNAPSHOT');

    const commandText = "java -jar polocloud-launcher.jar";

    // Helper function to render highlighted text
    const renderMessage = (message: string, highlight?: string, highlightColor?: string) => {
        if (!highlight || !highlightColor) {
            return <span>{message}</span>;
        }
        
        const parts = message.split(highlight);
        return (
            <>
                {parts.map((part, index) => (
                    <span key={index}>
                        {part}
                        {index < parts.length - 1 && (
                            <span className={highlightColor}>{highlight}</span>
                        )}
                    </span>
                ))}
            </>
        );
    };

    useEffect(() => {
        const timer = setTimeout(() => {
            setIsVisible(true);
        }, 100);

        const commandTimer = setTimeout(() => {
            setShowCommand(true);
        }, 1500);

        return () => {
            clearTimeout(timer);
            clearTimeout(commandTimer);
        };
    }, []);

    // Fetch latest release version from GitHub
    useEffect(() => {
        const fetchLatestVersion = async () => {
            try {
                const response = await fetch('/api/github-stats');
                if (response.ok) {
                    const data = await response.json();
                    if (data.releases && data.releases > 0) {
                        // Fetch the latest release details
                        const releasesResponse = await fetch('https://api.github.com/repos/HttpMarco/polocloud/releases/latest');
                        if (releasesResponse.ok) {
                            const releaseData = await releasesResponse.json();
                            setLatestVersion(`v${releaseData.tag_name}`);
                        }
                    }
                }
            } catch (error) {
                console.log('Failed to fetch latest version, using fallback');
                // Keep the fallback version
            }
        };

        fetchLatestVersion();
    }, []);

    useEffect(() => {
        if (showCommand && currentIndex < commandText.length) {
            const timeout = setTimeout(() => {
                setTypedText(commandText.slice(0, currentIndex + 1));
                setCurrentIndex(currentIndex + 1);
            }, 100);
            return () => clearTimeout(timeout);
        } else if (showCommand && currentIndex >= commandText.length) {
            const logsTimer = setTimeout(() => {
                setShowLogsPhase1(true);
            }, 500);
            return () => clearTimeout(logsTimer);
        }
    }, [showCommand, currentIndex, commandText]);

    useEffect(() => {
        if (showLogsPhase1) {
            const phase2Timer = setTimeout(() => {
                setShowLogsPhase2(true);
            }, 2000);
            return () => clearTimeout(phase2Timer);
        }
    }, [showLogsPhase1]);

    useEffect(() => {
        if (showLogsPhase2) {
            const phase3Timer = setTimeout(() => {
                setShowLogsPhase3(true);
            }, 2000);
            return () => clearTimeout(phase3Timer);
        }
    }, [showLogsPhase2]);

    const terminalLinesPhase1: TerminalLine[] = [
        { time: "16:42:01", level: "INFO", message: `Starting PoloCloud ${latestVersion.replace('v', '')} Agent...`, color: "text-gray-300" },
        { time: "16:42:01", level: "WARN", message: "You are using a snapshot version of polocloud. This version is not recommended for production use!", color: "text-yellow-400" }
    ];

    const terminalLinesPhase2: TerminalLine[] = [
        { time: "16:42:01", level: "INFO", message: "You are running the latest version of the agent.", color: "text-gray-300" },
        { time: "16:42:04", level: "INFO", message: "Successfully started gRPC server on port 8932", color: "text-gray-300" },
        { time: "16:42:04", level: "INFO", message: "Using runtime: LocalRuntime", color: "text-gray-300" },
        { time: "16:42:04", level: "INFO", message: "Load groups (2): lobby, proxy", color: "text-gray-300" },
        { time: "16:42:04", level: "INFO", message: "Load 12 platforms with 207 versions.", color: "text-gray-300" },
        { time: "16:42:04", level: "INFO", message: "The agent is now successfully started and ready to use!", color: "text-gray-300", highlight: "successfully", highlightColor: "text-cyan-400" },
        { time: "16:42:04", level: "INFO", message: "The service lobby-1 is now starting...", color: "text-gray-300", highlight: "lobby-1", highlightColor: "text-cyan-400" },
        { time: "16:42:04", level: "INFO", message: "The service proxy-1 is now starting...", color: "text-gray-300", highlight: "proxy-1", highlightColor: "text-cyan-400" }
    ];

    const terminalLinesPhase3: TerminalLine[] = [
        { time: "16:42:08", level: "INFO", message: "The service proxy-1 is now online.", color: "text-gray-300", highlight: "proxy-1", highlightColor: "text-cyan-400" },
        { time: "16:42:30", level: "INFO", message: "The service lobby-1 is now online.", color: "text-gray-300", highlight: "lobby-1", highlightColor: "text-cyan-400" }
    ];

    return (
        <section className="relative overflow-hidden bg-gradient-to-br from-background via-background to-muted/20 min-h-screen flex items-center justify-center">
            <div className="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.02)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.02)_1px,transparent_1px)] bg-[size:50px_50px] dark:bg-[linear-gradient(rgba(255,255,255,0.01)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.01)_1px,transparent_1px)]" />

            {/* Floating Particles */}
            <div className="absolute inset-0 overflow-hidden pointer-events-none">
                {/* Particle 1 */}
                <motion.div
                    className="absolute w-2 h-2 bg-primary/20 rounded-full"
                    animate={{
                        x: [0, 100, 0],
                        y: [0, -50, 0],
                        opacity: [0.3, 0.8, 0.3],
                    }}
                    transition={{
                        duration: 8,
                        repeat: Infinity,
                        ease: "easeInOut",
                    }}
                    style={{
                        left: "10%",
                        top: "20%",
                    }}
                />
                
                {/* Particle 2 */}
                <motion.div
                    className="absolute w-1 h-1 bg-primary/30 rounded-full"
                    animate={{
                        x: [0, -80, 0],
                        y: [0, 60, 0],
                        opacity: [0.2, 0.6, 0.2],
                    }}
                    transition={{
                        duration: 12,
                        repeat: Infinity,
                        ease: "easeInOut",
                        delay: 2,
                    }}
                    style={{
                        left: "80%",
                        top: "30%",
                    }}
                />
                
                {/* Particle 3 */}
                <motion.div
                    className="absolute w-1.5 h-1.5 bg-primary/25 rounded-full"
                    animate={{
                        x: [0, 60, 0],
                        y: [0, -40, 0],
                        opacity: [0.4, 0.7, 0.4],
                    }}
                    transition={{
                        duration: 10,
                        repeat: Infinity,
                        ease: "easeInOut",
                        delay: 4,
                    }}
                    style={{
                        left: "20%",
                        top: "70%",
                    }}
                />
                
                {/* Particle 4 */}
                <motion.div
                    className="absolute w-1 h-1 bg-primary/15 rounded-full"
                    animate={{
                        x: [0, -40, 0],
                        y: [0, 80, 0],
                        opacity: [0.3, 0.5, 0.3],
                    }}
                    transition={{
                        duration: 15,
                        repeat: Infinity,
                        ease: "easeInOut",
                        delay: 1,
                    }}
                    style={{
                        left: "70%",
                        top: "60%",
                    }}
                />
                
                {/* Particle 5 */}
                <motion.div
                    className="absolute w-2 h-2 bg-primary/20 rounded-full"
                    animate={{
                        x: [0, 120, 0],
                        y: [0, -30, 0],
                        opacity: [0.2, 0.8, 0.2],
                    }}
                    transition={{
                        duration: 14,
                        repeat: Infinity,
                        ease: "easeInOut",
                        delay: 3,
                    }}
                    style={{
                        left: "40%",
                        top: "10%",
                    }}
                />
                
                {/* Particle 6 */}
                <motion.div
                    className="absolute w-1 h-1 bg-primary/25 rounded-full"
                    animate={{
                        x: [0, -60, 0],
                        y: [0, 50, 0],
                        opacity: [0.3, 0.6, 0.3],
                    }}
                    transition={{
                        duration: 11,
                        repeat: Infinity,
                        ease: "easeInOut",
                        delay: 5,
                    }}
                    style={{
                        left: "90%",
                        top: "80%",
                    }}
                />
                
                {/* Particle 7 */}
                <motion.div
                    className="absolute w-1.5 h-1.5 bg-primary/20 rounded-full"
                    animate={{
                        x: [0, 80, 0],
                        y: [0, -60, 0],
                        opacity: [0.4, 0.7, 0.4],
                    }}
                    transition={{
                        duration: 13,
                        repeat: Infinity,
                        ease: "easeInOut",
                        delay: 2.5,
                    }}
                    style={{
                        left: "15%",
                        top: "40%",
                    }}
                />
                
                {/* Particle 8 */}
                <motion.div
                    className="absolute w-1 h-1 bg-primary/30 rounded-full"
                    animate={{
                        x: [0, -100, 0],
                        y: [0, 40, 0],
                        opacity: [0.2, 0.5, 0.2],
                    }}
                    transition={{
                        duration: 16,
                        repeat: Infinity,
                        ease: "easeInOut",
                        delay: 6,
                    }}
                    style={{
                        left: "60%",
                        top: "90%",
                    }}
                />

                {/* Particle 9 */}
                <motion.div
                    className="absolute w-1.5 h-1.5 bg-primary/25 rounded-full"
                    animate={{
                        x: [0, 70, 0],
                        y: [0, -40, 0],
                        opacity: [0.3, 0.6, 0.3],
                    }}
                    transition={{
                        duration: 12,
                        repeat: Infinity,
                        ease: "easeInOut",
                        delay: 1.5,
                    }}
                    style={{
                        left: "85%",
                        top: "30%",
                    }}
                />

                {/* Particle 10 */}
                <motion.div
                    className="absolute w-1 h-1 bg-primary/20 rounded-full"
                    animate={{
                        x: [0, -50, 0],
                        y: [0, 90, 0],
                        opacity: [0.2, 0.4, 0.2],
                    }}
                    transition={{
                        duration: 18,
                        repeat: Infinity,
                        ease: "easeInOut",
                        delay: 3.5,
                    }}
                    style={{
                        left: "25%",
                        top: "85%",
                    }}
                />

                {/* Particle 11 */}
                <motion.div
                    className="absolute w-2 h-2 bg-primary/15 rounded-full"
                    animate={{
                        x: [0, 90, 0],
                        y: [0, -70, 0],
                        opacity: [0.1, 0.3, 0.1],
                    }}
                    transition={{
                        duration: 20,
                        repeat: Infinity,
                        ease: "easeInOut",
                        delay: 4.5,
                    }}
                    style={{
                        left: "75%",
                        top: "15%",
                    }}
                />

                {/* Particle 12 */}
                <motion.div
                    className="absolute w-1 h-1 bg-primary/35 rounded-full"
                    animate={{
                        x: [0, -80, 0],
                        y: [0, 60, 0],
                        opacity: [0.4, 0.7, 0.4],
                    }}
                    transition={{
                        duration: 14,
                        repeat: Infinity,
                        ease: "easeInOut",
                        delay: 2,
                    }}
                    style={{
                        left: "45%",
                        top: "75%",
                    }}
                />

                {/* Particle 13 */}
                <motion.div
                    className="absolute w-1.5 h-1.5 bg-primary/20 rounded-full"
                    animate={{
                        x: [0, 60, 0],
                        y: [0, -50, 0],
                        opacity: [0.2, 0.5, 0.2],
                    }}
                    transition={{
                        duration: 16,
                        repeat: Infinity,
                        ease: "easeInOut",
                        delay: 5.5,
                    }}
                    style={{
                        left: "5%",
                        top: "25%",
                    }}
                />

                {/* Particle 14 */}
                <motion.div
                    className="absolute w-1 h-1 bg-primary/30 rounded-full"
                    animate={{
                        x: [0, -70, 0],
                        y: [0, 30, 0],
                        opacity: [0.3, 0.6, 0.3],
                    }}
                    transition={{
                        duration: 13,
                        repeat: Infinity,
                        ease: "easeInOut",
                        delay: 1,
                    }}
                    style={{
                        left: "95%",
                        top: "50%",
                    }}
                />

                {/* Particle 15 */}
                <motion.div
                    className="absolute w-2 h-2 bg-primary/25 rounded-full"
                    animate={{
                        x: [0, 40, 0],
                        y: [0, -90, 0],
                        opacity: [0.2, 0.4, 0.2],
                    }}
                    transition={{
                        duration: 17,
                        repeat: Infinity,
                        ease: "easeInOut",
                        delay: 6.5,
                    }}
                    style={{
                        left: "35%",
                        top: "5%",
                    }}
                />
            </div>

            <div className="relative container mx-auto px-6 py-20 z-10">
                <div className={`grid lg:grid-cols-2 gap-12 items-center transition-all duration-1000 ease-out ${
                    isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
                }`}>
                    <div className="order-1 text-center lg:text-left">
                        {/* Version Badge */}
                        <div className={`inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary/10 border border-primary/20 text-primary text-sm font-medium mb-6 transition-all duration-1000 delay-100 ${
                            isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
                        }`}>
                            <div className="w-2 h-2 bg-primary rounded-full animate-pulse"></div>
                            <span>{latestVersion}</span>
                        </div>

                        <h1 className={`text-5xl md:text-7xl lg:text-8xl font-black mb-8 text-foreground dark:text-white transition-all duration-1000 delay-200 tracking-tight leading-tight ${
                            isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
                        }`}>
                            <TextGenerateEffect words="PoloCloud" />
                        </h1>

                        <p className={`text-xl md:text-2xl lg:text-3xl text-muted-foreground mb-16 max-w-4xl mx-auto lg:mx-0 leading-relaxed font-light transition-all duration-1000 delay-400 ${
                            isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
                        }`}>
                            Deploy and manage your Minecraft servers with ease.
                            <span className="block mt-2 text-lg md:text-xl lg:text-2xl font-normal">
                Built for performance, designed for simplicity.
              </span>
                        </p>

                        <div className={`transition-all duration-1000 delay-600 ${
                            isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
                        }`}>
                            <GitHubStatsComponent />
                        </div>

                        <div className={`flex flex-col sm:flex-row gap-6 justify-center lg:justify-start transition-all duration-1000 delay-800 relative z-20 ${
                            isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
                        }`}>
                            <Link
                                href="/docs/cloud"
                                className="group bg-[rgba(0,120,255,0.9)] hover:bg-[rgba(0,120,255,1)] text-white px-8 py-3 rounded-xl font-semibold text-lg transition-all duration-300 shadow-[0_0_20px_rgba(0,120,255,0.3)] hover:shadow-[0_0_25px_rgba(0,120,255,0.4)] flex items-center justify-center gap-3 relative z-10"
                            >
                                <BookOpen className="w-5 h-5" />
                                Get Started
                                <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                            </Link>
                            <Link
                                href="https://github.com/HttpMarco/polocloud"
                                className="group bg-card/50 hover:bg-card border border-border/50 px-8 py-3 rounded-xl font-semibold text-lg transition-all duration-300 backdrop-blur-sm shadow-lg hover:shadow-xl flex items-center justify-center gap-3 relative z-10"
                            >
                                <Github className="w-5 h-5" />
                                View on GitHub
                            </Link>
                        </div>
                    </div>

                    <motion.div
                        className="order-2"
                        initial={{ opacity: 0, x: 50 }}
                        animate={{ opacity: 1, x: 0 }}
                        transition={{ duration: 0.8, delay: 0.3 }}
                    >
                        <div className="bg-[#282c34] rounded-lg shadow-2xl border border-border/50 overflow-hidden h-[26rem]">
                            <div className="bg-[#21252b] px-4 py-3 flex items-center justify-between border-b border-border/30">
                                <div className="flex items-center gap-2">
                                    <div className="w-3 h-3 bg-red-500 rounded-full"></div>
                                    <div className="w-3 h-3 bg-yellow-500 rounded-full"></div>
                                    <div className="w-3 h-3 bg-green-500 rounded-full"></div>
                                </div>
                                <div className="text-xs text-muted-foreground font-mono">
                                    PoloCloud Terminal
                                </div>
                                <div className="w-16"></div>
                            </div>

                            <div className="p-4 font-mono text-sm">
                                {showCommand && (
                                    <div className="flex items-center gap-2 mb-4">
                                        <span className="text-cyan-400 font-mono text-sm">/home/polocloud</span>
                                        <span className="text-gray-400 font-mono text-sm">$</span>
                                        <span className="text-gray-300 font-mono text-sm">
                      {typedText}
                                            {currentIndex < commandText.length && (
                                                <motion.span
                                                    className="text-gray-300"
                                                    animate={{ opacity: [1, 0, 1] }}
                                                    transition={{ duration: 0.8, repeat: Infinity }}
                                                >
                                                    |
                                                </motion.span>
                                            )}
                    </span>
                                    </div>
                                )}

                                {/* Phase 1 Logs */}
                                {showLogsPhase1 && terminalLinesPhase1.map((line, index) => (
                                    <motion.div
                                        key={`phase1-${index}`}
                                        className="flex items-start gap-2 mb-1"
                                        initial={{ opacity: 0, x: -20 }}
                                        animate={{ opacity: 1, x: 0 }}
                                        transition={{ duration: 0.3, delay: index * 0.1 }}
                                    >
                    <span className="text-gray-500 font-mono text-xs w-16 flex-shrink-0">
                      {line.time}
                    </span>
                                        <span className="text-gray-400 font-mono text-xs w-8 flex-shrink-0">
                      |
                    </span>
                                        <span className={`font-mono text-xs w-12 flex-shrink-0 ${
                                            line.level === 'WARN' ? 'text-yellow-400' : 'text-gray-300'
                                        }`}>
                      {line.level}:
                    </span>
                                        <span className={`font-mono text-xs ${line.color}`}>
                      {renderMessage(line.message, line.highlight, line.highlightColor)}
                    </span>
                                    </motion.div>
                                ))}

                                {/* Phase 2 Logs */}
                                {showLogsPhase2 && terminalLinesPhase2.map((line, index) => (
                                    <motion.div
                                        key={`phase2-${index}`}
                                        className="flex items-start gap-2 mb-1"
                                        initial={{ opacity: 0, x: -20 }}
                                        animate={{ opacity: 1, x: 0 }}
                                        transition={{ duration: 0.3, delay: index * 0.1 }}
                                    >
                    <span className="text-gray-500 font-mono text-xs w-16 flex-shrink-0">
                      {line.time}
                    </span>
                                        <span className="text-gray-400 font-mono text-xs w-8 flex-shrink-0">
                      |
                    </span>
                                        <span className={`font-mono text-xs w-12 flex-shrink-0 ${
                                            line.level === 'WARN' ? 'text-yellow-400' : 'text-white'
                                        }`}>
                      {line.level}:
                    </span>
                                        <span className={`font-mono text-xs ${line.color}`}>
                      {renderMessage(line.message, line.highlight, line.highlightColor)}
                    </span>
                                    </motion.div>
                                ))}

                                {/* Phase 3 Logs */}
                                {showLogsPhase3 && terminalLinesPhase3.map((line, index) => (
                                    <motion.div
                                        key={`phase3-${index}`}
                                        className="flex items-start gap-2 mb-1"
                                        initial={{ opacity: 0, x: -20 }}
                                        animate={{ opacity: 1, x: 0 }}
                                        transition={{ duration: 0.3, delay: index * 0.1 }}
                                    >
                    <span className="text-gray-500 font-mono text-xs w-16 flex-shrink-0">
                      {line.time}
                    </span>
                                        <span className="text-gray-400 font-mono text-xs w-8 flex-shrink-0">
                      |
                    </span>
                                        <span className={`font-mono text-xs w-12 flex-shrink-0 ${
                                            line.level === 'WARN' ? 'text-yellow-400' : 'text-white'
                                        }`}>
                      {line.level}:
                    </span>
                                        <span className={`font-mono text-xs ${line.color}`}>
                      {renderMessage(line.message, line.highlight, line.highlightColor)}
                    </span>
                                    </motion.div>
                                ))}

                                {showLogsPhase3 && (
                                    <motion.div
                                        className="flex items-center gap-2 mt-2"
                                        initial={{ opacity: 0 }}
                                        animate={{ opacity: 1 }}
                                        transition={{ duration: 0.3, delay: 1.5 }}
                                    >
                                        <span className="text-gray-300 font-mono text-sm">
                                            {renderMessage(`polocloud@${latestVersion.replace('v', '')}`, "polocloud", "text-cyan-400")}
                                        </span>
                                        <span className="text-gray-400 font-mono text-sm">»</span>
                                        <motion.span
                                            className="text-gray-300 font-mono text-xs"
                                            animate={{ opacity: [1, 0, 1] }}
                                            transition={{ duration: 1, repeat: Infinity }}
                                        >
                                            _
                                        </motion.span>
                                    </motion.div>
                                )}
                            </div>
                        </div>
                    </motion.div>
                </div>
            </div>

            <div className="absolute bottom-0 left-0 right-0 h-64 bg-gradient-to-t from-background via-background/95 via-background/80 to-transparent z-5" />
        </section>
    );
} 