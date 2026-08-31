'use client';

import Hero from '@/components/Hero';
import Stats from '@/components/Stats';
import Benefits from '@/components/Benefits';
import AppScreenshots from '@/components/AppScreenshots';
import Pricing from '@/components/Pricing/Pricing';
import Testimonials from '@/components/Testimonials';
import FAQ from '@/components/FAQ';
import CTA from '@/components/CTA';

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
            <CTA />
        </>
    );
}
