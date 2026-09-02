'use client';

import Link from 'next/link';
import React from 'react';
import { siteDetails } from '@/data/siteDetails';
import { footerDetails } from '@/data/footer';
import { getPlatformIconByName } from '@/utils';
import { useLanguage } from '@/lib/LanguageContext';

const Footer: React.FC = () => {
    const { t } = useLanguage();
    return (
        <footer className="bg-hero-background text-foreground py-16 px-6">
            <div className="max-w-7xl mx-auto">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-12 md:gap-8">
                    <div>
                        <Link href="/" className="flex items-center gap-3">
                            <img src="/logo.png" alt="Logo" className="w-9 h-9 rounded-full" />
                            <h3 className="text-lg font-bold">
                                {siteDetails.siteName}
                            </h3>
                        </Link>
                        <p className="mt-4 text-sm text-foreground-accent leading-relaxed max-w-sm">
                            {t.footer.subheading}
                        </p>
                    </div>
                    <div>
                        <h4 className="text-sm font-semibold uppercase tracking-wider mb-5">{t.footer.quickLinks}</h4>
                        <ul className="space-y-3">
                            {footerDetails.quickLinks.map(link => (
                                <li key={link.text}>
                                    <Link href={link.url} className="text-sm text-foreground-accent hover:text-foreground transition-colors">
                                        {link.text}
                                    </Link>
                                </li>
                            ))}
                        </ul>
                    </div>
                    <div>
                        <h4 className="text-sm font-semibold uppercase tracking-wider mb-5">{t.footer.contactUs}</h4>
                        <div className="space-y-3">
                            {footerDetails.email && (
                                <a href={`mailto:${footerDetails.email}`} className="block text-sm text-foreground-accent hover:text-foreground transition-colors">
                                    {footerDetails.email}
                                </a>
                            )}
                            {footerDetails.telephone && (
                                <a href={`tel:${footerDetails.telephone}`} className="block text-sm text-foreground-accent hover:text-foreground transition-colors">
                                    {footerDetails.telephone}
                                </a>
                            )}
                        </div>
                        {footerDetails.socials && (
                            <div className="mt-6 flex items-center gap-4">
                                {Object.keys(footerDetails.socials).map(platformName => {
                                    if (platformName && footerDetails.socials[platformName]) {
                                        return (
                                            <Link href={footerDetails.socials[platformName]} key={platformName} aria-label={platformName} className="text-foreground-accent hover:text-foreground transition-colors">
                                                {getPlatformIconByName(platformName)}
                                            </Link>
                                        )
                                    }
                                })}
                            </div>
                        )}
                    </div>
                </div>
                <div className="mt-12 pt-8 border-t border-border md:text-center text-foreground-accent">
                    <p className="text-sm">Copyright &copy; {new Date().getFullYear()} {siteDetails.siteName}. {t.footer.copyright}</p>
                    <p className="text-xs mt-2 text-muted-foreground">{t.footer.madeBy} <a href="https://codebridgecademy.com/" target="_blank" className="hover:underline text-foreground-accent hover:text-primary italic">Fidele Software Engineer</a></p>
                </div>
            </div>
        </footer>
    );
};

export default Footer;
