'use client';
import { useEffect, useState, useRef } from 'react';
import Link from 'next/link';
import { ArrowLeft, Shield, Eye, Lock, Database } from 'lucide-react';

export function PrivacyContent() {
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
            Privacy Policy
          </h1>
          <p className={`text-xl text-muted-foreground max-w-3xl mx-auto leading-relaxed transition-all duration-1000 delay-400 ${
            isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
          }`}>
            How we collect, use, and protect your personal information
          </p>
        </div>

        <div className={`max-w-4xl mx-auto transition-all duration-1000 delay-600 ${
          isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
        }`}>
          <div className="bg-card/30 backdrop-blur-sm border border-border/30 rounded-2xl p-8 shadow-lg">
            <div className="space-y-8">
              <div>
                <h2 className="text-2xl font-bold text-foreground dark:text-white mb-4 flex items-center gap-3">
                  <Shield className="w-6 h-6 text-blue-400" />
                  Introduction
                </h2>
                <div className="space-y-4 text-muted-foreground dark:text-white/60 leading-relaxed">
                  <p>
                    At PoloCloud, we take your privacy seriously. This Privacy Policy explains how we collect, 
                    use, disclose, and safeguard your information when you use our Minecraft server management 
                    platform and related services.
                  </p>
                  <p>
                    By using our services, you agree to the collection and use of information in accordance 
                    with this policy. If you do not agree with our policies and practices, please do not use 
                    our services.
                  </p>
                </div>
              </div>

              <div>
                <h2 className="text-2xl font-bold text-foreground dark:text-white mb-4 flex items-center gap-3">
                  <Database className="w-6 h-6 text-green-400" />
                  Information We Collect
                </h2>
                <div className="space-y-4 text-muted-foreground dark:text-white/60 leading-relaxed">
                  <p>
                    <strong className="text-foreground dark:text-white">Personal Information:</strong> 
                    We may collect personal information such as your name, email address, and Discord username 
                    when you register for our services or contact our support team.
                  </p>
                  <p>
                    <strong className="text-foreground dark:text-white">Usage Data:</strong> 
                    We collect information about how you use our services, including server configurations, 
                    performance metrics, and interaction with our platform features.
                  </p>
                  <p>
                    <strong className="text-foreground dark:text-white">Technical Data:</strong> 
                    We may collect technical information such as IP addresses, browser type, operating system, 
                    and device information for security and optimization purposes.
                  </p>
                </div>
              </div>

              <div>
                <h2 className="text-2xl font-bold text-foreground dark:text-white mb-4 flex items-center gap-3">
                  <Eye className="w-6 h-6 text-purple-400" />
                  How We Use Your Information
                </h2>
                <div className="space-y-4 text-muted-foreground dark:text-white/60 leading-relaxed">
                  <p>We use the collected information for various purposes:</p>
                  <ul className="list-disc list-inside space-y-2 ml-4">
                    <li>To provide and maintain our services</li>
                    <li>To notify you about changes to our services</li>
                    <li>To provide customer support and respond to inquiries</li>
                    <li>To monitor and analyze usage patterns to improve our services</li>
                    <li>To detect, prevent, and address technical issues</li>
                    <li>To ensure the security and integrity of our platform</li>
                  </ul>
                </div>
              </div>

              <div>
                <h2 className="text-2xl font-bold text-foreground dark:text-white mb-4 flex items-center gap-3">
                  <Lock className="w-6 h-6 text-red-400" />
                  Data Protection
                </h2>
                <div className="space-y-4 text-muted-foreground dark:text-white/60 leading-relaxed">
                  <p>
                    We implement appropriate security measures to protect your personal information against 
                    unauthorized access, alteration, disclosure, or destruction. These measures include:
                  </p>
                  <ul className="list-disc list-inside space-y-2 ml-4">
                    <li>Encryption of data in transit and at rest</li>
                    <li>Regular security assessments and updates</li>
                    <li>Access controls and authentication mechanisms</li>
                    <li>Secure data storage and backup procedures</li>
                  </ul>
                  <p>
                    However, no method of transmission over the internet or electronic storage is 100% secure. 
                    While we strive to use commercially acceptable means to protect your personal information, 
                    we cannot guarantee its absolute security.
                  </p>
                </div>
              </div>

              <div>
                <h2 className="text-2xl font-bold text-foreground dark:text-white mb-4">
                  Third-Party Services
                </h2>
                <div className="space-y-4 text-muted-foreground dark:text-white/60 leading-relaxed">
                  <p>
                    Our services may contain links to third-party websites or services that are not owned or 
                    controlled by PoloCloud. We have no control over and assume no responsibility for the 
                    content, privacy policies, or practices of any third-party websites or services.
                  </p>
                  <p>
                    We strongly advise you to review the privacy policy of every site you visit and the terms 
                    of service of any third-party services you use.
                  </p>
                </div>
              </div>

              <div>
                <h2 className="text-2xl font-bold text-foreground dark:text-white mb-4">
                  Changes to This Policy
                </h2>
                <div className="space-y-4 text-muted-foreground dark:text-white/60 leading-relaxed">
                  <p>
                    We may update our Privacy Policy from time to time. We will notify you of any changes by 
                    posting the new Privacy Policy on this page and updating the &ldquo;Last updated&rdquo; date.
                  </p>
                  <p>
                    You are advised to review this Privacy Policy periodically for any changes. Changes to 
                    this Privacy Policy are effective when they are posted on this page.
                  </p>
                </div>
              </div>

              <div>
                <h2 className="text-2xl font-bold text-foreground dark:text-white mb-4">
                  Contact Us
                </h2>
                <div className="space-y-4 text-muted-foreground dark:text-white/60 leading-relaxed">
                  <p>
                    If you have any questions about this Privacy Policy, please contact us:
                  </p>
                  <div className="space-y-2">
                    <p><strong className="text-foreground dark:text-white">Email:</strong> privacy@polocloud.de</p>
                    <p><strong className="text-foreground dark:text-white">Discord:</strong> discord.polocloud.de</p>
                    <p><strong className="text-foreground dark:text-white">Last Updated:</strong> January 2025</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
} 