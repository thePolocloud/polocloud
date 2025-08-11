'use client';
import { useEffect, useState, useRef } from 'react';
import { ExternalLink, Users, Handshake, Star, Cloud, Award, Zap } from 'lucide-react';
import { motion } from 'framer-motion';
import Image from 'next/image';

interface Partner {
    id: string;
    name: string;
    logo: string;
    website?: string;
    description?: string;
}

const PartnersCarousel = () => {
    const [isVisible, setIsVisible] = useState(false);
    const [partners, setPartners] = useState<Partner[]>([]);
    const [loading, setLoading] = useState(true);
    const carouselRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const observer = new IntersectionObserver(
            ([entry]) => {
                if (entry.isIntersecting) {
                    setIsVisible(true);
                }
            },
            { threshold: 0.1, rootMargin: '0px 0px -100px 0px' }
        );

        if (carouselRef.current) {
            observer.observe(carouselRef.current);
        }

        return () => observer.disconnect();
    }, []);


    useEffect(() => {
        const fetchPartners = async () => {
            try {
                const response = await fetch('/api/public/partners');
                if (response.ok) {
                    const data = await response.json();
                    setPartners(data.partners || []);
                } else {
                    console.error('Failed to fetch partners');
                }
            } catch (error) {
                console.error('Error fetching partners:', error);
            } finally {
                setLoading(false);
            }
        };

        fetchPartners();
    }, []);

    return (
        <div
            ref={carouselRef}
            className={`transition-all duration-1000 ease-out ${
                isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
            }`}
        >
            <div className="relative overflow-hidden bg-gradient-to-br from-card/30 via-card/20 to-card/30 backdrop-blur-xl border border-border/50 rounded-xl p-8 shadow-2xl">
                <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-primary/5" />
                <div className="absolute top-0 right-0 w-24 h-24 bg-primary/10 rounded-full blur-2xl" />
                <div className="absolute bottom-0 left-0 w-20 h-20 bg-primary/10 rounded-full blur-xl" />

                <div className="relative z-10">
                    <div className="text-center mb-8">
                        <div className="w-12 h-12 bg-gradient-to-br from-primary/20 to-primary/10 rounded-xl flex items-center justify-center mx-auto mb-4 shadow-lg">
                            <Star className="w-6 h-6 text-primary" />
                        </div>
                        <h3 className="text-2xl md:text-3xl font-black text-foreground dark:text-white mb-4">
                            Our Partners
                        </h3>
                        <p className="text-base md:text-lg text-muted-foreground dark:text-white/70 leading-relaxed max-w-2xl mx-auto">
                            Discover our trusted partners and integrations that make PoloCloud the ultimate Minecraft hosting solution.
                        </p>
                    </div>

                    <div className="relative overflow-hidden max-w-2xl mx-auto">
                        {loading ? (
                            <div className="text-center text-muted-foreground py-8">
                                Loading partners...
                            </div>
                        ) : partners.length === 0 ? (
                            <div className="text-center text-muted-foreground py-8">
                                No partners available.
                            </div>
                        ) : partners.length === 1 ? (

                            <div className="flex justify-center">
                                <motion.div
                                    className="flex flex-col items-center"
                                    initial={{ opacity: 0, scale: 0.8 }}
                                    whileInView={{ opacity: 1, scale: 1 }}
                                    viewport={{ once: true }}
                                    transition={{ delay: 0.1 }}
                                >
                                    <div 
                                        className="w-32 h-20 bg-gradient-to-br from-background/50 to-background/30 backdrop-blur-sm border border-border/30 rounded-lg flex items-center justify-center mb-3 shadow-lg hover:shadow-xl transition-all duration-300 group cursor-pointer"
                                        onClick={() => {
                                            if (partners[0].website) {
                                                window.open(partners[0].website, '_blank', 'noopener,noreferrer');
                                            }
                                        }}
                                    >
                                        <img
                                            src={partners[0].logo}
                                            alt={partners[0].name}
                                            className="w-full h-full object-contain p-3 group-hover:scale-110 transition-transform duration-300"
                                            onError={(e) => {
                                                console.warn(`Failed to load logo for ${partners[0].name}:`, partners[0].logo);
                                                e.currentTarget.style.display = 'none';
                                            }}
                                        />
                                    </div>
                                    <span className="text-sm font-semibold text-foreground dark:text-white/80 text-center">
                                        {partners[0].name}
                                    </span>
                                    {partners[0].website && (
                                        <ExternalLink className="w-3 h-3 text-muted-foreground opacity-0 group-hover:opacity-100 transition-opacity duration-300 mt-1" />
                                    )}
                                </motion.div>
                            </div>
                        ) : (

                            <div className="flex space-x-6 animate-scroll">
                                {partners.map((partner, index) => (
                                    <div
                                        key={`first-${partner.id}`}
                                        className="flex-shrink-0 flex flex-col items-center"
                                    >
                                        <div 
                                            className="w-28 h-18 bg-gradient-to-br from-background/50 to-background/30 backdrop-blur-sm border border-border/30 rounded-lg flex items-center justify-center mb-2 shadow-lg hover:shadow-xl transition-all duration-300 group cursor-pointer"
                                            onClick={() => {
                                                if (partner.website) {
                                                    window.open(partner.website, '_blank', 'noopener,noreferrer');
                                                }
                                            }}
                                        >
                                            <img
                                                src={partner.logo}
                                                alt={partner.name}
                                                className="w-full h-full object-contain p-2 group-hover:scale-110 transition-transform duration-300"
                                                onError={(e) => {
                                                    console.warn(`Failed to load logo for ${partner.name}:`, partner.logo);
                                                    e.currentTarget.style.display = 'none';
                                                }}
                                            />
                                        </div>
                                        <span className="text-xs font-semibold text-foreground dark:text-white/80 text-center">
                                            {partner.name}
                                        </span>
                                        {partner.website && (
                                            <ExternalLink className="w-3 h-3 text-muted-foreground opacity-0 group-hover:opacity-100 transition-opacity duration-300 mt-1" />
                                        )}
                                    </div>
                                ))}
                                {partners.map((partner, index) => (
                                    <div
                                        key={`second-${partner.id}`}
                                        className="flex-shrink-0 flex flex-col items-center"
                                    >
                                        <div 
                                            className="w-28 h-18 bg-gradient-to-br from-background/50 to-background/30 backdrop-blur-sm border border-border/30 rounded-lg flex items-center justify-center mb-2 shadow-lg hover:shadow-xl transition-all duration-300 group cursor-pointer"
                                            onClick={() => {
                                                if (partner.website) {
                                                    window.open(partner.website, '_blank', 'noopener,noreferrer');
                                                }
                                            }}
                                        >
                                            <img
                                                src={partner.logo}
                                                alt={partner.name}
                                                className="w-full h-full object-contain p-2 group-hover:scale-110 transition-transform duration-300"
                                                onError={(e) => {
                                                    console.warn(`Failed to load logo for ${partner.name}:`, partner.logo);
                                                    e.currentTarget.style.display = 'none';
                                                }}
                                            />
                                        </div>
                                        <span className="text-xs font-semibold text-foreground dark:text-white/80 text-center">
                                            {partner.name}
                                        </span>
                                        {partner.website && (
                                            <ExternalLink className="w-3 h-3 text-muted-foreground opacity-0 group-hover:opacity-100 transition-opacity duration-300 mt-1" />
                                        )}
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    <motion.div
                        className="text-center mt-8"
                        initial={{ opacity: 0, y: 20 }}
                        whileInView={{ opacity: 1, y: 0 }}
                        viewport={{ once: true }}
                        transition={{ delay: 0.5 }}
                    >
                        <div className="inline-flex items-center gap-2 px-6 py-3 bg-gradient-to-r from-primary/10 to-primary/5 border border-primary/20 text-primary rounded-lg text-sm font-semibold hover:from-primary/20 hover:to-primary/10 transition-all duration-300 cursor-pointer group">
                            <Award className="w-4 h-4 group-hover:scale-110 transition-transform" />
                            Become a partner
                            <ExternalLink className="w-3 h-3 group-hover:translate-x-1 transition-transform" />
                        </div>
                    </motion.div>
                </div>
            </div>
        </div>
    );
};

const PartnershipInfo = () => {
    const [isVisible, setIsVisible] = useState(false);
    const infoRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const observer = new IntersectionObserver(
            ([entry]) => {
                if (entry.isIntersecting) {
                    setIsVisible(true);
                }
            },
            { threshold: 0.1, rootMargin: '0px 0px -100px 0px' }
        );

        if (infoRef.current) {
            observer.observe(infoRef.current);
        }

        return () => observer.disconnect();
    }, []);

    const benefits = [
        {
            icon: Users,
            title: "Partner Listing",
            description: "Featured on polocloud.de partner list",
            color: "from-blue-500/20 to-blue-600/20",
            iconColor: "text-blue-400"
        },
        {
            icon: Handshake,
            title: "Premium Support",
            description: "Direct contact person for support",
            color: "from-green-500/20 to-green-600/20",
            iconColor: "text-green-400"
        },
        {
            icon: Star,
            title: "Higher Priority",
            description: "Priority handling for issues",
            color: "from-yellow-500/20 to-yellow-600/20",
            iconColor: "text-yellow-400"
        },
        {
            icon: Cloud,
            title: "Custom Addons",
            description: "Development of special addons",
            color: "from-purple-500/20 to-purple-600/20",
            iconColor: "text-purple-400"
        },
        {
            icon: Award,
            title: "Exclusive Badges",
            description: "Special partner badges and recognition",
            color: "from-pink-500/20 to-pink-600/20",
            iconColor: "text-pink-400"
        },
        {
            icon: Zap,
            title: "Early Access",
            description: "Access to new features before release",
            color: "from-orange-500/20 to-orange-600/20",
            iconColor: "text-orange-400"
        }
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

    const cardVariants = {
        hidden: {
            opacity: 0,
            y: 20,
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
                stiffness: 300,
                damping: 20
            }
        }
    };

    return (
        <motion.div
            ref={infoRef}
            className="relative overflow-hidden bg-gradient-to-br from-card/50 via-card/30 to-card/50 backdrop-blur-xl border border-border/50 rounded-2xl p-6 shadow-2xl"
            initial={{ opacity: 0, y: 50 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, amount: 0.3 }}
            transition={{ duration: 0.8, ease: "easeOut" }}
        >
            <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-primary/5" />
            <div className="absolute top-0 right-0 w-24 h-24 bg-primary/10 rounded-full blur-2xl" />
            <div className="absolute bottom-0 left-0 w-20 h-20 bg-primary/10 rounded-full blur-xl" />
            <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-32 h-32 bg-primary/5 rounded-full blur-2xl" />

            <div className="relative z-10">
                <div className="text-center mb-8">
                    <motion.div
                        className="w-14 h-14 bg-gradient-to-br from-primary/20 to-primary/10 rounded-xl flex items-center justify-center mx-auto mb-4 shadow-lg"
                        initial={{ opacity: 0, scale: 0.8, rotate: -180 }}
                        whileInView={{ opacity: 1, scale: 1, rotate: 0 }}
                        viewport={{ once: true, amount: 0.3 }}
                        transition={{ duration: 0.8, delay: 0.1, type: "spring", stiffness: 200 }}
                    >
                        <Handshake className="w-7 h-7 text-primary" />
                    </motion.div>
                    <motion.h3
                        className="text-2xl md:text-3xl font-black text-foreground dark:text-white mb-4 tracking-tight leading-tight bg-gradient-to-r from-foreground via-primary to-foreground bg-clip-text text-transparent"
                        initial={{ opacity: 0, y: 20 }}
                        whileInView={{ opacity: 1, y: 0 }}
                        viewport={{ once: true, amount: 0.3 }}
                        transition={{ duration: 0.6, delay: 0.2 }}
                    >
                        Partner with PoloCloud
                    </motion.h3>
                    <motion.p
                        className="text-base md:text-lg text-muted-foreground dark:text-white/70 max-w-4xl mx-auto leading-relaxed"
                        initial={{ opacity: 0, y: 20 }}
                        whileInView={{ opacity: 1, y: 0 }}
                        viewport={{ once: true, amount: 0.3 }}
                        transition={{ duration: 0.6, delay: 0.4 }}
                    >
                        Partner with PoloCloud and help shape the future of Minecraft server management.
                        Enjoy exclusive benefits and grow your business with us.
                    </motion.p>
                </div>

                <div className="mb-8">
                    <motion.h4
                        className="text-xl md:text-2xl font-black text-foreground dark:text-white text-center mb-6 tracking-tight leading-tight"
                        initial={{ opacity: 0, y: 20 }}
                        whileInView={{ opacity: 1, y: 0 }}
                        viewport={{ once: true, amount: 0.3 }}
                        transition={{ duration: 0.6, delay: 0.6 }}
                    >
                        Exclusive Partner Benefits
                    </motion.h4>
                    <motion.div
                        className="grid md:grid-cols-2 lg:grid-cols-3 gap-4"
                        variants={containerVariants}
                        initial="hidden"
                        whileInView="visible"
                        viewport={{ once: true, amount: 0.3 }}
                    >
                        {benefits.map((benefit, index) => (
                            <motion.div
                                key={index}
                                className="group text-center p-4 bg-gradient-to-br backdrop-blur-sm rounded-xl border border-border/30 shadow-lg relative overflow-hidden hover:shadow-xl transition-all duration-300"
                                variants={cardVariants}
                                whileHover={{
                                    scale: 1.05,
                                    y: -5,
                                    transition: { type: "spring", stiffness: 400, damping: 20 }
                                }}
                                whileTap={{ scale: 0.98 }}
                                style={{
                                    background: `linear-gradient(135deg, ${benefit.color.split(' ')[1].replace('to-', '')}20, ${benefit.color.split(' ')[0].replace('from-', '')}20)`
                                }}
                            >
                                <motion.div
                                    className="absolute inset-0 bg-gradient-to-br from-white/10 to-transparent opacity-0 group-hover:opacity-100 transition-all duration-300"
                                />

                                <motion.div
                                    className={`w-12 h-12 bg-gradient-to-br rounded-xl flex items-center justify-center mx-auto mb-3 relative z-10 shadow-lg`}
                                    style={{
                                        background: `linear-gradient(135deg, ${benefit.color.split(' ')[1].replace('to-', '')}40, ${benefit.color.split(' ')[0].replace('from-', '')}40)`
                                    }}
                                    whileHover={{
                                        scale: 1.2,
                                        rotate: 360,
                                        transition: { duration: 0.6, ease: "easeInOut" }
                                    }}
                                >
                                    <div className={`w-6 h-6 ${benefit.iconColor}`}>
                                        <benefit.icon className="w-full h-full" />
                                    </div>
                                </motion.div>

                                <motion.h5
                                    className="font-black text-foreground dark:text-white mb-2 text-base leading-tight relative z-10"
                                    transition={{ type: "spring", stiffness: 400 }}
                                >
                                    {benefit.title}
                                </motion.h5>

                                <motion.p
                                    className="text-xs text-muted-foreground dark:text-white/70 font-medium relative z-10 leading-relaxed"
                                    initial={{ opacity: 0 }}
                                    whileInView={{ opacity: 1 }}
                                    transition={{ delay: 0.3 + index * 0.1 }}
                                >
                                    {benefit.description}
                                </motion.p>
                            </motion.div>
                        ))}
                    </motion.div>
                </div>

                <motion.div
                    className="text-center"
                    initial={{ opacity: 0, y: 30 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    viewport={{ once: true, amount: 0.3 }}
                    transition={{ duration: 0.8, delay: 1.0 }}
                >
                    <div className="relative overflow-hidden bg-gradient-to-r from-primary/15 via-primary/10 to-primary/15 border border-primary/30 rounded-xl p-6 mb-6 shadow-lg">
                        <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-primary/5" />
                        <div className="absolute top-0 right-0 w-16 h-16 bg-primary/20 rounded-full blur-xl" />
                        <div className="absolute bottom-0 left-0 w-12 h-12 bg-primary/20 rounded-full blur-lg" />

                        <div className="relative z-10">
                            <h4 className="text-lg font-bold text-foreground dark:text-white mb-3">
                                Ready to Partner?
                            </h4>
                            <p className="text-sm text-muted-foreground dark:text-white/70 mb-4 leading-relaxed">
                                Join our Discord to discuss partnership opportunities and take your business to the next level.
                            </p>
                            <motion.a
                                href="https://discord.com/channels/1401907740489678912/1402064495857369189"
                                target="_blank"
                                rel="noopener noreferrer"
                                className="inline-flex items-center gap-2 px-6 py-3 bg-gradient-to-r from-primary to-primary/90 hover:from-primary/90 hover:to-primary text-primary-foreground rounded-lg font-semibold text-base shadow-[0_0_20px_rgba(0,120,255,0.3)] hover:shadow-[0_0_30px_rgba(0,120,255,0.5)] group transition-all duration-300"
                                whileHover={{
                                    scale: 1.05,
                                    boxShadow: "0 0 40px rgba(0,120,255,0.6)",
                                    y: -2
                                }}
                                whileTap={{ scale: 0.95 }}
                            >
                                <Handshake className="w-4 h-4 group-hover:rotate-12 transition-transform duration-300" />
                                Apply for Partnership
                                <ExternalLink className="w-3 h-3 group-hover:translate-x-1 transition-transform duration-300" />
                            </motion.a>
                        </div>
                    </div>
                </motion.div>
            </div>
        </motion.div>
    );
};

export function PartnersSection() {
    const [isVisible, setIsVisible] = useState(false);
    const sectionRef = useRef<HTMLElement>(null);

    useEffect(() => {
        const observer = new IntersectionObserver(
            ([entry]) => {
                if (entry.isIntersecting) {
                    setIsVisible(true);
                }
            },
            { threshold: 0.1, rootMargin: '0px 0px -100px 0px' }
        );

        if (sectionRef.current) {
            observer.observe(sectionRef.current);
        }

        return () => observer.disconnect();
    }, []);

    return (
        <section ref={sectionRef} className="relative py-32 overflow-hidden">
            <div className="absolute top-0 left-0 right-0 h-48 bg-gradient-to-b from-background via-background/95 via-background/80 to-transparent" />
            <div className="absolute inset-0 bg-gradient-to-b from-background via-muted/5 to-muted/5" />

            <div className="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.02)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.02)_1px,transparent_1px)] bg-[size:50px_50px] dark:bg-[linear-gradient(rgba(255,255,255,0.01)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.01)_1px,transparent_1px)]" />

            <div className="absolute top-20 left-10 w-32 h-32 bg-primary/5 rounded-full blur-3xl animate-pulse" />
            <div className="absolute bottom-20 right-10 w-40 h-40 bg-primary/5 rounded-full blur-3xl animate-pulse delay-1000" />

            <div className="relative container mx-auto px-6">
                <div className={`text-center mb-20 transition-all duration-1000 ease-out ${
                    isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
                }`}>
                    <h2 className={`text-3xl md:text-4xl lg:text-5xl font-black mb-8 bg-gradient-to-r from-foreground via-primary to-foreground bg-clip-text text-transparent transition-all duration-1000 delay-200 ${
                        isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
                    }`}>
                        Official Partners
                    </h2>
                    <p className={`text-base md:text-lg text-muted-foreground max-w-4xl mx-auto leading-relaxed transition-all duration-1000 delay-400 ${
                        isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
                    }`}>
                        We partner with leading cloud providers and server networks to deliver the best Minecraft hosting experience.
                    </p>
                </div>

                <div className={`transition-all duration-1000 delay-600 ${
                    isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
                }`}>
                    <PartnersCarousel />
                </div>

                <div className={`transition-all duration-1000 delay-800 mt-16 ${
                    isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
                }`}>
                    <PartnershipInfo />
                </div>
            </div>

            <div className="absolute bottom-0 left-0 right-0 h-48 bg-gradient-to-t from-background via-background/95 via-background/80 to-transparent" />
        </section>
    );
} 