'use client';

import { useLanguage } from '@/lib/LanguageContext';
import Stats from '@/components/Stats';
import Benefits from '@/components/Benefits';

export default function FeaturesContent() {
    const { t } = useLanguage();

    return (
        <main className="pt-32 pb-20 px-5">
            <div className="max-w-4xl mx-auto text-center mb-16">
                <h1 className="text-4xl md:text-6xl font-bold mb-6">
                    {t.features.title}
                </h1>
                <p className="text-lg text-foreground-accent max-w-2xl mx-auto">
                    {t.features.subtitle}
                </p>
            </div>
            <Stats />
            <div id="benefits">
                <Benefits />
            </div>
        </main>
    );
}
