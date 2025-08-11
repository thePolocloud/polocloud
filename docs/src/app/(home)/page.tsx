import { HeroSection } from './components/hero-section';
import { AboutSection } from './components/about-section';
import { CompatibilitySection } from './components/compatibility-section';
import { ContributorsSection } from './components/contributors-section';
import { AddonsSection } from './components/addons-section';
import { FAQSection } from './components/faq-section';
import { PartnersSection } from './components/partners-section';
import { BackToTopButton } from '@/components/back-to-top-button';
import FeedbackSection from '@/components/sections/feedback-section';

export default function HomePage() {
    return (
        <main className="flex flex-1 flex-col">
            <HeroSection />
            <section id="about">
                <AboutSection />
            </section>
            <section id="platforms">
                <CompatibilitySection />
            </section>
            <section id="addons">
                <AddonsSection />
            </section>
            <section id="faq">
                <FAQSection />
            </section>
            <section id="partners">
                <PartnersSection />
            </section>
            <section id="feedback">
                <FeedbackSection />
            </section>
            <section id="contributors">
                <ContributorsSection />
            </section>
            <BackToTopButton />
        </main>
    );
}
