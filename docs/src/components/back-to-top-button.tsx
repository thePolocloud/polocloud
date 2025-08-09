'use client';

import { ArrowUp } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { useState, useEffect } from 'react';

export function BackToTopButton() {
    const [isVisible, setIsVisible] = useState(false);

    useEffect(() => {
        const toggleVisibility = () => {
            if (window.pageYOffset > 300) {
                setIsVisible(true);
            } else {
                setIsVisible(false);
            }
        };

        window.addEventListener('scroll', toggleVisibility);
        return () => window.removeEventListener('scroll', toggleVisibility);
    }, []);

    const scrollToTop = () => {
        const currentPosition = window.pageYOffset;
        const targetPosition = 0;
        const distance = currentPosition - targetPosition;
        const duration = 1000; // 1 Sekunde
        let start: number | null = null;

        const animation = (currentTime: number) => {
            if (start === null) start = currentTime;
            const timeElapsed = currentTime - start;
            const progress = Math.min(timeElapsed / duration, 1);
            
            // Easing function für schönere Animation
            const easeOutQuart = 1 - Math.pow(1 - progress, 4);
            
            window.scrollTo(0, currentPosition - (distance * easeOutQuart));
            
            if (timeElapsed < duration) {
                requestAnimationFrame(animation);
            }
        };

        requestAnimationFrame(animation);
    };

    return (
        <AnimatePresence>
            {isVisible && (
                <motion.button
                    onClick={scrollToTop}
                    className="fixed bottom-8 right-8 z-50 w-12 h-12 bg-primary/90 hover:bg-primary text-white rounded-full shadow-lg hover:shadow-xl transition-all duration-300 backdrop-blur-sm border border-primary/20"
                    initial={{ opacity: 0, scale: 0, y: 20 }}
                    animate={{ opacity: 1, scale: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0, y: 20 }}
                    whileHover={{ 
                        scale: 1.1,
                        y: -2,
                        boxShadow: "0 10px 25px rgba(0, 0, 0, 0.2)"
                    }}
                    whileTap={{ scale: 0.95 }}
                    transition={{ 
                        type: "spring", 
                        stiffness: 300, 
                        damping: 20 
                    }}
                    title="Back to top"
                >
                    <ArrowUp className="w-5 h-5 mx-auto" />
                </motion.button>
            )}
        </AnimatePresence>
    );
} 