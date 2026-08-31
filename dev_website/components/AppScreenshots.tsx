'use client';

import React, { useState } from 'react';
import SectionTitle from './SectionTitle';
import { useLanguage } from '@/lib/LanguageContext';

const screenshots = [
    { src: '/images/Screenshot_2026-01-02_015142-removebg-preview.png', alt: 'App Dashboard' },
    { src: '/images/Screenshot_2026-01-02_015203-removebg-preview.png', alt: 'Quiz Interface' },
    { src: '/images/Screenshot_2026-01-02_015320-removebg-preview.png', alt: 'Mock Test' },
    { src: '/images/Screenshot_2026-01-02_015333-removebg-preview.png', alt: 'Progress Tracking' },
    { src: '/images/Screenshot_2026-01-02_015350-removebg-preview.png', alt: 'Road Signs' },
    { src: '/images/Screenshot_2026-01-02_015409-removebg-preview.png', alt: 'Results' },
    { src: '/images/Screenshot_2026-01-02_015423-removebg-preview.png', alt: 'Settings' },
    { src: '/images/Screenshot_2026-01-02_015559-removebg-preview.png', alt: 'Profile' },
];

const AppScreenshots: React.FC = () => {
    const { t } = useLanguage();
    const [selectedIndex, setSelectedIndex] = useState<number | null>(null);

    const openLightbox = (index: number) => setSelectedIndex(index);
    const closeLightbox = () => setSelectedIndex(null);

    const goNext = () => {
        if (selectedIndex !== null) {
            setSelectedIndex((selectedIndex + 1) % screenshots.length);
        }
    };

    const goPrev = () => {
        if (selectedIndex !== null) {
            setSelectedIndex((selectedIndex - 1 + screenshots.length) % screenshots.length);
        }
    };

    return (
        <section id="screenshots" className="py-16 md:py-24 px-6">
            <div className="max-w-7xl mx-auto">
                <div className="text-center mb-12">
                    <SectionTitle>
                        <h2 className="my-3 !leading-snug">{t.screenshots.title}</h2>
                    </SectionTitle>
                    <p className="mt-4 text-foreground-accent max-w-2xl mx-auto">
                        {t.screenshots.subtitle}
                    </p>
                </div>

                {/* Screenshots Grid */}
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                    {screenshots.map((screenshot, index) => (
                        <div
                            key={index}
                            className="relative group cursor-pointer overflow-hidden rounded-2xl bg-muted border border-border hover:shadow-lg transition-all duration-300"
                            onClick={() => openLightbox(index)}
                        >
                            <img
                                src={screenshot.src}
                                alt={screenshot.alt}
                                className="w-full h-40 md:h-56 object-cover group-hover:scale-105 transition-transform duration-300"
                            />
                            <div className="absolute inset-0 bg-black/0 group-hover:bg-black/20 transition-all duration-300 flex items-center justify-center">
                                <span className="text-white opacity-0 group-hover:opacity-100 transition-opacity duration-300 text-sm font-medium">
                                    {screenshot.alt}
                                </span>
                            </div>
                        </div>
                    ))}
                </div>

                {/* Lightbox */}
                {selectedIndex !== null && (
                    <div
                        className="fixed inset-0 z-50 bg-black/90 flex items-center justify-center p-4"
                        onClick={closeLightbox}
                    >
                        <button
                            onClick={(e) => { e.stopPropagation(); goPrev(); }}
                            className="absolute left-4 top-1/2 -translate-y-1/2 p-2 bg-white/20 hover:bg-white/30 rounded-full text-white transition-colors"
                        >
                            ←
                        </button>
                        <img
                            src={screenshots[selectedIndex].src}
                            alt={screenshots[selectedIndex].alt}
                            className="max-w-full max-h-[80vh] object-contain rounded-2xl"
                            onClick={(e) => e.stopPropagation()}
                        />
                        <button
                            onClick={(e) => { e.stopPropagation(); goNext(); }}
                            className="absolute right-4 top-1/2 -translate-y-1/2 p-2 bg-white/20 hover:bg-white/30 rounded-full text-white transition-colors"
                        >
                            →
                        </button>
                        <button
                            onClick={closeLightbox}
                            className="absolute top-4 right-4 p-2 bg-white/20 hover:bg-white/30 rounded-full text-white transition-colors"
                        >
                            ✕
                        </button>
                        <div className="absolute bottom-4 text-white text-sm">
                            {selectedIndex + 1} / {screenshots.length}
                        </div>
                    </div>
                )}
            </div>
        </section>
    );
};

export default AppScreenshots;
