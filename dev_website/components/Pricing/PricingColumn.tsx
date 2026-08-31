'use client';

import clsx from "clsx";
import { BsFillCheckCircleFill } from "react-icons/bs";
import { IPricing } from "@/types";
import { useLanguage } from "@/lib/LanguageContext";

interface Props {
    tier: IPricing;
    highlight?: boolean;
}

const PricingColumn: React.FC<Props> = ({ tier, highlight }: Props) => {
    const { name, price, currency, features } = tier;
    const { t } = useLanguage();

    return (
        <div className={clsx("w-full max-w-sm mx-auto rounded-xl border lg:max-w-full transition-all", {
            "bg-card border-primary shadow-lg ring-2 ring-primary/20": highlight,
            "bg-card border-border shadow-sm": !highlight,
        })}>
            <div className="p-6 border-b border-border rounded-t-xl">
                <h3 className="text-2xl font-semibold text-foreground mb-4">{name}</h3>
                <p className="text-3xl md:text-5xl font-bold mb-6">
                    <span className={clsx("text-foreground", { "text-primary": highlight })}>
                        {typeof price === 'number' ? `${price.toLocaleString()} ${currency || ''}` : price}
                    </span>
                    {typeof price === 'number' && <span className="text-lg font-normal text-muted-foreground">/{t.pricing.perMonth}</span>}
                </p>
                <button className={clsx("w-full py-3 px-4 rounded-full transition-colors font-semibold", {
                    "bg-primary hover:bg-primary-accent text-primary-foreground": highlight,
                    "bg-muted hover:bg-muted/80 text-foreground border border-border": !highlight,
                })}>
                    {t.pricing.getStarted}
                </button>
            </div>
            <div className="p-6 mt-1">
                <p className="font-bold text-foreground mb-0">{t.pricing.features}</p>
                <p className="text-muted-foreground mb-5">{t.pricing.everythingInBasic}</p>
                <ul className="space-y-4 mb-8">
                    {features.map((feature, index) => (
                        <li key={index} className="flex items-center">
                            <BsFillCheckCircleFill className={clsx("h-5 w-5 mr-2", { "text-primary": highlight, "text-muted-foreground": !highlight })} />
                            <span className="text-foreground-accent">{feature}</span>
                        </li>
                    ))}
                </ul>
            </div>
        </div>
    )
}

export default PricingColumn