'use client';

import Hero from '@/components/Hero';
import Stats from '@/components/Stats';
import Benefits from '@/components/Benefits';
import FAQ from '@/components/FAQ';
import CTA from '@/components/CTA';

export default function HomeContent() {
    return (
        <>
            <Hero />
            <Stats />
            <Benefits />
            <FAQ />
            <CTA />
        </>
    );
}
