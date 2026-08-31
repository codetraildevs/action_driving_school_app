'use client';

import { useLanguage } from '@/lib/LanguageContext';

export default function DownloadContent() {
    const { t } = useLanguage();

    return (
        <main className="pt-32 pb-20 px-5">
            <div className="max-w-4xl mx-auto text-center">
                <h1 className="text-4xl md:text-6xl font-bold mb-6">
                    {t.download.title}
                </h1>
                <p className="text-lg text-foreground-accent mb-8 max-w-2xl mx-auto">
                    {t.download.subtitle}
                </p>

                <div className="flex flex-col sm:flex-row items-center justify-center gap-6 mb-16">
                    <a
                        href="https://play.google.com/store/apps/details?id=com.drivingschoolrwandaapp"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-block"
                    >
                        <img
                            src="/Google_Play_Store_badge_EN.svg"
                            alt="Get it on Google Play"
                            className="h-16"
                        />
                    </a>
                </div>

                <div className="grid md:grid-cols-3 gap-8 mt-16">
                    {t.download.features.map((feature, index) => (
                        <div key={index} className="p-6 rounded-2xl bg-background border border-border">
                            <h3 className="text-xl font-semibold mb-3">{feature.title}</h3>
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
