'use client';

import { useLanguage } from '@/lib/LanguageContext';
import { FiDownload, FiUserPlus, FiBookOpen, FiCheckCircle, FiTruck, FiWifiOff, FiRefreshCw, FiBarChart2, FiGlobe } from 'react-icons/fi';

const stepIcons = [
    <FiDownload size={20} />,
    <FiUserPlus size={20} />,
    <FiBookOpen size={20} />,
    <FiCheckCircle size={20} />,
    <FiTruck size={20} />,
];

const stepImages = [
    '/mobile-app-download-screen-with-play-store-icon.jpg',
    '/images/Screenshot_2026-01-02_014048-removebg-preview.png',
    '/images/Screenshot_2026-01-02_015203-removebg-preview.png',
    '/images/Screenshot_2026-01-02_015320-removebg-preview.png',
    '/safe-driver-on-road-with-traffic-signals.jpg',
];

const whyIcons = [
    <FiWifiOff size={18} className="text-primary" />,
    <FiRefreshCw size={18} className="text-primary" />,
    <FiBarChart2 size={18} className="text-primary" />,
    <FiGlobe size={18} className="text-primary" />,
];

export default function HowItWorksContent() {
    const { t } = useLanguage();

    return (
        <main className="pt-28 md:pt-36 pb-16 px-6">
            {/* Page Header */}
            <div className="max-w-4xl mx-auto text-center mb-16">
                <h1 className="text-4xl md:text-5xl lg:text-6xl font-bold mb-6">
                    {t.howItWorks.title}
                </h1>
                <p className="text-lg text-foreground-accent max-w-2xl mx-auto leading-relaxed">
                    {t.howItWorks.subtitle}
                </p>
            </div>

            {/* 5 Steps */}
            <div className="max-w-7xl mx-auto">
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 xl:grid-cols-5 gap-6">
                    {t.howItWorks.steps.map((step, index) => (
                        <div key={index} className="flex flex-col">
                            {/* Image */}
                            <div className="w-full h-48 rounded-2xl overflow-hidden mb-5 bg-muted">
                                <img
                                    src={stepImages[index]}
                                    alt={step.title}
                                    className="w-full h-full object-cover"
                                />
                            </div>

                            {/* Number + Icon */}
                            <div className="flex items-center gap-3 mb-3">
                                <div className="w-10 h-10 rounded-full bg-muted flex items-center justify-center text-sm font-bold text-foreground">
                                    {index + 1}
                                </div>
                                <div className="text-gray-500 dark:text-gray-400">
                                    {stepIcons[index]}
                                </div>
                            </div>

                            {/* Title */}
                            <h3 className="text-xl font-bold text-foreground mb-2">
                                {step.title}
                            </h3>

                            {/* Description */}
                            <p className="text-sm text-foreground-accent leading-relaxed">
                                {step.description}
                            </p>
                        </div>
                    ))}
                </div>
            </div>

            {/* Why Students Love Our App */}
            <div className="max-w-7xl mx-auto mt-20">
                <h2 className="text-3xl font-bold text-center mb-12">{t.howItWorks.whyStudentsLove}</h2>
                <div className="grid md:grid-cols-2 gap-8">
                    {t.howItWorks.whyFeatures.map((feature, index) => (
                        <div key={index} className="p-6 rounded-2xl bg-background border border-border">
                            <div className="flex items-center gap-2 mb-2">
                                {whyIcons[index]}
                                <h3 className="text-lg font-semibold">{feature.title}</h3>
                            </div>
                            <p className="text-foreground-accent">
                                {feature.description}
                            </p>
                        </div>
                    ))}
                </div>
            </div>
        </main>
    );
}
