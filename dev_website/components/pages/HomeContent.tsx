'use client';

import Hero from '@/components/Hero';
import Stats from '@/components/Stats';
import Benefits from '@/components/Benefits';
import AppScreenshots from '@/components/AppScreenshots';
import Pricing from '@/components/Pricing/Pricing';
import Testimonials from '@/components/Testimonials';
import FAQ from '@/components/FAQ';
import CTA from '@/components/CTA';
import Link from 'next/link';

function SeoIntro() {
    return (
        <section className="py-16 px-6">
            <div className="max-w-4xl mx-auto text-foreground-accent leading-relaxed">
                <h2 className="text-2xl md:text-3xl font-bold text-foreground mb-4">
                    Learn traffic rules and pass your Rwanda driving test
                </h2>
                <p className="mb-4">
                    <strong>Action Driving School</strong> helps learners across
                    Rwanda master the highway code, understand every road sign,
                    and prepare for the official driving theory exam. Whether
                    you are starting from zero or brushing up before your test
                    date, our app gives you real exam questions, clear
                    explanations and timed mock tests that match the actual
                    Rwanda driving test format.
                </p>
                <p className="mb-4">
                    Explore our app features, see how the training works, and
                    read free study guides on our blog — from the complete
                    theory exam guide to road signs, speed limits and
                    right-of-way rules. Everything is available in Kinyarwanda,
                    English and French, so you can learn traffic rules in the
                    language you understand best.
                </p>
                <p>
                    Ready to start?{' '}
                    <Link href="/download/" className="text-primary font-semibold hover:underline">
                        Download Action Driving School App
                    </Link>{' '}
                    and take your first practice driving test today — free.
                </p>
            </div>
        </section>
    );
}

export default function HomeContent() {
    return (
        <>
            <Hero />
            <Stats />
            <Benefits />
            <AppScreenshots />
            <Pricing />
            <Testimonials />
            <FAQ />
            <SeoIntro />
            <CTA />
        </>
    );
}
