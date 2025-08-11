'use client';
import { useEffect, useState, useRef } from 'react';
import Link from 'next/link';
import { ArrowLeft } from 'lucide-react';

export function ImprintContent() {
    const [isVisible, setIsVisible] = useState(false);
    const contentRef = useRef<HTMLElement>(null);

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

    return (
        <section ref={contentRef} className="relative py-12 overflow-hidden">
            <div className="absolute inset-0 bg-gradient-to-b from-background via-muted/5 to-muted/5" />

            <div className="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.02)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.02)_1px,transparent_1px)] bg-[size:50px_50px] dark:bg-[linear-gradient(rgba(255,255,255,0.01)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.01)_1px,transparent_1px)]" />

            <div className="relative container mx-auto px-6">
                <div className={`mb-8 transition-all duration-1000 ease-out ${
                    isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
                }`}>
                    <Link
                        href="/"
                        className="inline-flex items-center gap-2 px-4 py-2 bg-card/50 hover:bg-card border border-border/50 rounded-lg text-sm font-medium transition-all duration-300 hover:scale-105 backdrop-blur-sm"
                    >
                        <ArrowLeft className="w-4 h-4" />
                        Back to Home
                    </Link>
                </div>

                <div className={`text-center mb-16 transition-all duration-1000 ease-out ${
                    isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
                }`}>
                    <h1 className={`text-3xl md:text-4xl lg:text-5xl font-black mb-8 bg-gradient-to-r from-foreground via-primary to-foreground bg-clip-text text-transparent tracking-tight leading-tight transition-all duration-1000 delay-200 ${
                        isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
                    }`}>
                        Imprint
                    </h1>
                    <p className={`text-xl text-muted-foreground max-w-3xl mx-auto leading-relaxed transition-all duration-1000 delay-400 ${
                        isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
                    }`}>
                        Legal information and contact details for PoloCloud
                    </p>
                </div>

                <div className={`max-w-4xl mx-auto transition-all duration-1000 delay-600 ${
                    isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
                }`}>
                    <div className="bg-card/30 backdrop-blur-sm border border-border/30 rounded-2xl p-8 shadow-lg">
                        <div className="space-y-8">
                            <div>
                                <h2 className="text-2xl font-bold text-foreground dark:text-white mb-4">
                                    Company Information
                                </h2>
                                <div className="space-y-3 text-muted-foreground dark:text-white/60">
                                    <p><strong className="text-foreground dark:text-white">Developer:</strong> Mirco Lindenau</p>
                                    <p><strong className="text-foreground dark:text-white">c/o:</strong> MDC Management#2167</p>
                                    <p><strong className="text-foreground dark:text-white">Address:</strong> Welserstraße 3</p>
                                    <p><strong className="text-foreground dark:text-white">ZIP/City:</strong> 87463 Dietmannsried</p>
                                </div>
                            </div>

                            <div>
                                <h2 className="text-2xl font-bold text-foreground dark:text-white mb-4">
                                    Legal Notice
                                </h2>
                                <div className="space-y-4 text-muted-foreground dark:text-white/60 leading-relaxed">
                                    <p>
                                        <strong className="text-foreground dark:text-white">Content Liability</strong><br />
                                        The contents of our pages have been created with the utmost care. However, we cannot
                                        guarantee the accuracy, completeness and timeliness of the content.
                                    </p>
                                    <p>
                                        <strong className="text-foreground dark:text-white">Link Liability</strong><br />
                                        Our offer contains links to external websites of third parties, over whose content we
                                        have no influence. Therefore, we cannot assume any liability for these external contents.
                                    </p>
                                    <p>
                                        <strong className="text-foreground dark:text-white">Copyright</strong><br />
                                        The contents and works created by the site operators on these pages are subject to
                                        German copyright law.
                                    </p>
                                </div>
                            </div>

                            <div>
                                <h2 className="text-2xl font-bold text-foreground dark:text-white mb-4">
                                    Privacy Policy
                                </h2>
                                <div className="space-y-4 text-muted-foreground dark:text-white/60 leading-relaxed">
                                    <p>
                                        <strong className="text-foreground dark:text-white">Data Protection Declaration</strong><br />
                                        The use of our website is generally possible without providing personal data.
                                        Insofar as personal data (e.g., name, address or e-mail addresses) is collected on our pages,
                                        this is always done on a voluntary basis as far as possible.
                                    </p>
                                    <p>
                                        <strong className="text-foreground dark:text-white">Cookies</strong><br />
                                        The websites use so-called cookies in some cases. Cookies do not cause any damage to your
                                        computer and do not contain viruses.
                                    </p>
                                </div>
                            </div>

                            <div>
                                <h2 className="text-2xl font-bold text-foreground dark:text-white mb-4">
                                    Contact Information
                                </h2>
                                <div className="space-y-3 text-muted-foreground dark:text-white/60">
                                    <p><strong className="text-foreground dark:text-white">Developer:</strong> Mirco Lindenau</p>
                                    <p><strong className="text-foreground dark:text-white">Address:</strong> Welserstraße 3, 87463 Dietmannsried</p>
                                    <p><strong className="text-foreground dark:text-white">c/o:</strong> MDC Management#2167</p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </section>
    );
} 