'use client';
import { useEffect, useState, useRef } from 'react';
import Link from 'next/link';
import { ArrowLeft, FileText, AlertTriangle, CheckCircle, XCircle, Shield } from 'lucide-react';

export function TermsContent() {
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
            Terms of Service
          </h1>
          <p className={`text-xl text-muted-foreground max-w-3xl mx-auto leading-relaxed transition-all duration-1000 delay-400 ${
            isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
          }`}>
            Please read these terms carefully before using our services
          </p>
        </div>

        <div className={`max-w-4xl mx-auto transition-all duration-1000 delay-600 ${
          isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'
        }`}>
          <div className="bg-card/30 backdrop-blur-sm border border-border/30 rounded-2xl p-8 shadow-lg">
            <div className="space-y-8">
              <div>
                <h2 className="text-2xl font-bold text-foreground dark:text-white mb-4 flex items-center gap-3">
                  <FileText className="w-6 h-6 text-blue-400" />
                  Introduction
                </h2>
                <div className="space-y-4 text-muted-foreground dark:text-white/60 leading-relaxed">
                  <p>
                    These Terms of Service (&ldquo;Terms&rdquo;) govern your use of PoloCloud&apos;s Minecraft server 
                    management platform and related services. By accessing or using our services, you agree to 
                    be bound by these Terms.
                  </p>
                  <p>
                    If you disagree with any part of these terms, then you may not access our services. 
                    These Terms apply to all visitors, users, and others who access or use our services.
                  </p>
                </div>
              </div>

              <div>
                <h2 className="text-2xl font-bold text-foreground dark:text-white mb-4 flex items-center gap-3">
                  <CheckCircle className="w-6 h-6 text-green-400" />
                  Acceptable Use
                </h2>
                <div className="space-y-4 text-muted-foreground dark:text-white/60 leading-relaxed">
                  <p>You agree to use our services only for lawful purposes and in accordance with these Terms:</p>
                  <ul className="list-disc list-inside space-y-2 ml-4">
                    <li>Comply with all applicable laws and regulations</li>
                    <li>Respect the rights and privacy of other users</li>
                    <li>Use the services for their intended purpose</li>
                    <li>Maintain the security and integrity of the platform</li>
                    <li>Report any security vulnerabilities or issues</li>
                  </ul>
                </div>
              </div>

              <div>
                <h2 className="text-2xl font-bold text-foreground dark:text-white mb-4 flex items-center gap-3">
                  <XCircle className="w-6 h-6 text-red-400" />
                  Prohibited Activities
                </h2>
                <div className="space-y-4 text-muted-foreground dark:text-white/60 leading-relaxed">
                  <p>You may not use our services to:</p>
                  <ul className="list-disc list-inside space-y-2 ml-4">
                    <li>Violate any applicable laws or regulations</li>
                    <li>Infringe upon the rights of others</li>
                    <li>Distribute malware, viruses, or harmful code</li>
                    <li>Attempt to gain unauthorized access to our systems</li>
                    <li>Interfere with or disrupt the services</li>
                    <li>Use the services for illegal or harmful purposes</li>
                    <li>Harass, abuse, or harm other users</li>
                  </ul>
                </div>
              </div>

              <div>
                <h2 className="text-2xl font-bold text-foreground dark:text-white mb-4 flex items-center gap-3">
                  <Shield className="w-6 h-6 text-purple-400" />
                  Service Availability
                </h2>
                <div className="space-y-4 text-muted-foreground dark:text-white/60 leading-relaxed">
                  <p>
                    We strive to provide reliable and continuous service, but we cannot guarantee that our 
                    services will be available 100% of the time. We may need to perform maintenance, updates, 
                    or other activities that may temporarily affect service availability.
                  </p>
                  <p>
                    We will make reasonable efforts to notify users of planned maintenance and minimize 
                    service disruptions. However, we are not liable for any damages resulting from service 
                    interruptions or unavailability.
                  </p>
                </div>
              </div>

              <div>
                <h2 className="text-2xl font-bold text-foreground dark:text-white mb-4">
                  User Responsibilities
                </h2>
                <div className="space-y-4 text-muted-foreground dark:text-white/60 leading-relaxed">
                  <p>As a user of our services, you are responsible for:</p>
                  <ul className="list-disc list-inside space-y-2 ml-4">
                    <li>Maintaining the confidentiality of your account credentials</li>
                    <li>All activities that occur under your account</li>
                    <li>Ensuring your server configurations comply with our policies</li>
                    <li>Backing up your data and configurations</li>
                    <li>Reporting any security concerns or violations</li>
                    <li>Complying with all applicable laws and regulations</li>
                  </ul>
                </div>
              </div>

              <div>
                <h2 className="text-2xl font-bold text-foreground dark:text-white mb-4">
                  Intellectual Property
                </h2>
                <div className="space-y-4 text-muted-foreground dark:text-white/60 leading-relaxed">
                  <p>
                    Our services and their original content, features, and functionality are owned by PoloCloud 
                    and are protected by international copyright, trademark, patent, trade secret, and other 
                    intellectual property laws.
                  </p>
                  <p>
                    You retain ownership of any content you create or upload to our services. By using our 
                    services, you grant us a limited license to use, store, and process your content solely 
                    for the purpose of providing our services.
                  </p>
                </div>
              </div>

              <div>
                <h2 className="text-2xl font-bold text-foreground dark:text-white mb-4 flex items-center gap-3">
                  <AlertTriangle className="w-6 h-6 text-yellow-400" />
                  Limitation of Liability
                </h2>
                <div className="space-y-4 text-muted-foreground dark:text-white/60 leading-relaxed">
                  <p>
                    In no event shall PoloCloud, nor its directors, employees, partners, agents, suppliers, 
                    or affiliates, be liable for any indirect, incidental, special, consequential, or punitive 
                    damages, including without limitation, loss of profits, data, use, goodwill, or other 
                    intangible losses, resulting from your use of our services.
                  </p>
                  <p>
                    Our total liability to you for any claims arising from the use of our services shall not 
                    exceed the amount you paid us, if any, for the services in the twelve (12) months 
                    preceding the claim.
                  </p>
                </div>
              </div>

              <div>
                <h2 className="text-2xl font-bold text-foreground dark:text-white mb-4">
                  Termination
                </h2>
                <div className="space-y-4 text-muted-foreground dark:text-white/60 leading-relaxed">
                  <p>
                    We may terminate or suspend your account and access to our services immediately, without 
                    prior notice or liability, for any reason whatsoever, including without limitation if you 
                    breach the Terms.
                  </p>
                  <p>
                    Upon termination, your right to use the services will cease immediately. If you wish to 
                    terminate your account, you may simply discontinue using our services.
                  </p>
                </div>
              </div>

              <div>
                <h2 className="text-2xl font-bold text-foreground dark:text-white mb-4">
                  Changes to Terms
                </h2>
                <div className="space-y-4 text-muted-foreground dark:text-white/60 leading-relaxed">
                  <p>
                    We reserve the right to modify or replace these Terms at any time. If a revision is 
                    material, we will try to provide at least 30 days notice prior to any new terms taking 
                    effect.
                  </p>
                  <p>
                    What constitutes a material change will be determined at our sole discretion. By continuing 
                    to access or use our services after any revisions become effective, you agree to be bound 
                    by the revised terms.
                  </p>
                </div>
              </div>

              <div>
                <h2 className="text-2xl font-bold text-foreground dark:text-white mb-4">
                  Contact Information
                </h2>
                <div className="space-y-4 text-muted-foreground dark:text-white/60 leading-relaxed">
                  <p>
                    If you have any questions about these Terms of Service, please contact us:
                  </p>
                  <div className="space-y-2">
                    <p><strong className="text-foreground dark:text-white">Email:</strong> legal@polocloud.de</p>
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