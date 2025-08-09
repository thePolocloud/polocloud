'use client';
import { useEffect, useState, useRef } from 'react';
import { HelpCircle, ChevronDown } from 'lucide-react';

interface FAQItem {
  question: string;
  answer: string;
}

const faqs: FAQItem[] = [
  {
    question: "What is PoloCloud?",
    answer: "PoloCloud is a modern cloud-native platform designed specifically for Minecraft server hosting. It offers scalable, flexible, and easy-to-manage server infrastructure with support for multiple server platforms."
  },
  {
    question: "Which Minecraft versions are supported?",
    answer: "PoloCloud supports all major Minecraft versions through various platforms like Paper, Velocity, Waterfall, and more. This includes both the latest releases and legacy versions."
  },
  {
    question: "Is PoloCloud free to use?",
    answer: "Yes, PoloCloud is completely free and open-source. You can download, modify, and use it for your Minecraft network without any licensing fees."
  },
  {
    question: "What are the system requirements?",
    answer: "PoloCloud requires Java 17 or higher. For optimal performance, we recommend at least 4GB RAM for the master node. Additional requirements depend on your specific server setup."
  },
  {
    question: "Can I contribute to PoloCloud?",
    answer: "Absolutely! PoloCloud is open-source and we welcome contributions. You can contribute through our GitHub repository by submitting pull requests, reporting issues, or improving documentation."
  },
  {
    question: "How do I get support?",
    answer: "We offer support through multiple channels: our Discord community, GitHub issues for bug reports, and comprehensive documentation. Our active community is always ready to help."
  }
];

export function FAQSection() {
  const [isVisible, setIsVisible] = useState(false);
  const [isBuilding, setIsBuilding] = useState(false);
  const [openIndex, setOpenIndex] = useState<number | null>(null);
  const sectionRef = useRef<HTMLElement>(null);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsVisible(true);
          setTimeout(() => {
            setIsBuilding(true);
          }, 300);
        }
      },
      { threshold: 0.1, rootMargin: '0px 0px -100px 0px' }
    );

    if (sectionRef.current) {
      observer.observe(sectionRef.current);
    }

    return () => observer.disconnect();
  }, []);

  const toggleFAQ = (index: number) => {
    setOpenIndex(openIndex === index ? null : index);
  };

  return (
    <section ref={sectionRef} className="relative py-32 overflow-hidden">
      <div className={`absolute top-0 left-0 right-0 h-48 bg-gradient-to-b from-background via-background/95 via-background/80 to-transparent transition-all duration-1000 ${
        isVisible ? 'opacity-100' : 'opacity-0'
      }`} />
      <div className={`absolute inset-0 bg-gradient-to-b from-background via-muted/5 to-muted/5 transition-all duration-1000 delay-200 ${
        isVisible ? 'opacity-100' : 'opacity-0'
      }`} />

      <div className={`absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.02)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.02)_1px,transparent_1px)] bg-[size:50px_50px] dark:bg-[linear-gradient(rgba(255,255,255,0.01)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.01)_1px,transparent_1px)] transition-all duration-1000 delay-400 ${
        isVisible ? 'opacity-100' : 'opacity-0'
      }`} />

      <div className={`absolute top-20 left-10 w-32 h-32 bg-primary/5 rounded-full blur-3xl animate-pulse transition-all duration-1000 delay-600 ${
        isVisible ? 'opacity-100 scale-100' : 'opacity-0 scale-50'
      }`} />
      <div className={`absolute bottom-20 right-10 w-40 h-40 bg-primary/5 rounded-full blur-3xl animate-pulse delay-1000 transition-all duration-1000 delay-800 ${
        isVisible ? 'opacity-100 scale-100' : 'opacity-0 scale-50'
      }`} />

      <div className="relative container mx-auto px-6">
        <div className={`text-center mb-20 transition-all duration-1000 ease-out ${
          isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
        }`}>
          <h2 className={`text-3xl md:text-4xl lg:text-5xl font-black mb-8 bg-gradient-to-r from-foreground via-primary to-foreground bg-clip-text text-transparent transition-all duration-1000 delay-200 tracking-tight leading-tight ${
            isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
          }`}>
            Frequently Asked Questions
          </h2>
          <p className={`text-base md:text-lg text-muted-foreground max-w-4xl mx-auto leading-relaxed transition-all duration-1000 delay-400 ${
            isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
          }`}>
            Everything you need to know about PoloCloud
            <span className="block mt-2 text-sm md:text-base font-normal">
              Can&apos;t find what you&apos;re looking for? Join our Discord community for more help.
            </span>
          </p>
        </div>

        <div className={`max-w-4xl mx-auto transition-all duration-1000 delay-600 ${
          isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
        }`}>
          {faqs.map((faq, index) => (
            <div
              key={faq.question}
              className={`mb-4 transition-all duration-500 ${
                isBuilding ? 'opacity-100 translate-y-0 scale-100' : 'opacity-0 translate-y-8 scale-95'
              }`}
              style={{ 
                transitionDelay: `${(index + 1) * 100}ms`,
                transform: isBuilding ? 'translateY(0) scale(1)' : 'translateY(20px) scale(0.95)'
              }}
            >
              <div className="bg-card/30 backdrop-blur-sm border border-border/50 rounded-xl overflow-hidden hover:bg-card/40 hover:border-border/70 transition-all duration-300 group">
                <button
                  onClick={() => toggleFAQ(index)}
                  className="w-full text-left transition-all duration-300"
                >
                  <div className="px-6 py-6 flex items-center justify-between hover:bg-card/20 transition-all duration-300">
                  <div className="flex items-center gap-4">
                    <div className="w-10 h-10 bg-card/60 rounded-lg flex items-center justify-center text-primary group-hover:scale-110 transition-transform duration-300">
                      <HelpCircle className="w-5 h-5" />
                    </div>
                    <h3 className="font-bold text-foreground dark:text-white text-lg leading-tight">
                      {faq.question}
                    </h3>
                  </div>
                  <ChevronDown 
                    className={`w-5 h-5 text-muted-foreground transition-transform duration-300 ${
                      openIndex === index ? 'rotate-180' : ''
                    }`} 
                  />
                  </div>
                </button>
                
                <div 
                  className={`overflow-hidden transition-all duration-500 ease-in-out ${
                    openIndex === index ? 'max-h-[500px] opacity-100' : 'max-h-0 opacity-0'
                  }`}
                >
                  <div className="px-6 pb-6 pt-0">
                    <div className="pl-14">
                      <p className="text-muted-foreground dark:text-white/70 leading-relaxed">
                        {faq.answer}
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className={`absolute bottom-0 left-0 right-0 h-48 bg-gradient-to-t from-background via-background/95 via-background/80 to-transparent transition-all duration-1000 delay-1000 ${
        isVisible ? 'opacity-100' : 'opacity-0'
      }`} />
    </section>
  );
} 