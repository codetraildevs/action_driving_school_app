'use client';

import PricingColumn from "./PricingColumn";
import SectionTitle from "@/components/SectionTitle";
import { useLanguage } from "@/lib/LanguageContext";
import { tiers } from "@/data/pricing";

const Pricing: React.FC = () => {
    const { t } = useLanguage();
    return (
        <section id="pricing" className="py-16 md:py-24 px-6">
            <div className="max-w-7xl mx-auto">
                <div className="text-center mb-12">
                    <SectionTitle>
                        <h2 className="my-3 !leading-snug">{t.pricing.title}</h2>
                    </SectionTitle>
                    <p className="mt-4 text-foreground-accent max-w-2xl mx-auto">
                        {t.pricing.subtitle}
                    </p>
                </div>
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                    {tiers.map((tier, index) => (
                        <PricingColumn key={tier.name} tier={tier} highlight={index === 1} />
                    ))}
                </div>
            </div>
        </section>
    );
}

export default Pricing