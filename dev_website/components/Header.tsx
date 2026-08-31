'use client';

import Link from 'next/link';
import React, { useState, useEffect } from 'react';
import { HiOutlineXMark, HiBars3 } from 'react-icons/hi2';
import { FiSun, FiMoon, FiGlobe } from 'react-icons/fi';
import { BsDownload } from 'react-icons/bs';

import { siteDetails } from '@/data/siteDetails';
import { useLanguage } from '@/lib/LanguageContext';
import { Locale } from '@/lib/translations';

const languages: { code: Locale; label: string }[] = [
    { code: 'en', label: 'EN' },
    { code: 'fr', label: 'FR' },
    { code: 'rw', label: 'RW' },
];

const Header: React.FC = () => {
    const { locale, setLocale, t } = useLanguage();
    const [isOpen, setIsOpen] = useState(false);
    const [darkMode, setDarkMode] = useState(false);
    const [langOpen, setLangOpen] = useState(false);
    const [scrolled, setScrolled] = useState(false);

    useEffect(() => {
        const saved = localStorage.getItem('theme');
        if (saved === 'dark' || (!saved && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
            setDarkMode(true);
            document.documentElement.classList.add('dark');
        }
        const handleScroll = () => setScrolled(window.scrollY > 20);
        window.addEventListener('scroll', handleScroll);
        return () => window.removeEventListener('scroll', handleScroll);
    }, []);

    const toggleDark = () => {
        setDarkMode(!darkMode);
        if (!darkMode) {
            document.documentElement.classList.add('dark');
            localStorage.setItem('theme', 'dark');
        } else {
            document.documentElement.classList.remove('dark');
            localStorage.setItem('theme', 'light');
        }
    };

    return (
        <header className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${scrolled ? 'bg-background/95 backdrop-blur-md shadow-sm border-b border-border' : 'bg-transparent'}`}>
            <div className="max-w-7xl mx-auto px-6">
                <nav className="flex justify-between items-center h-16 md:h-20">
                    {/* Logo */}
                    <Link href="/" className="flex items-center gap-3">
                        <img src="/logo.png" alt="Logo" className="w-9 h-9 rounded-full" />
                        <span className="text-lg font-bold text-foreground tracking-tight">
                            {siteDetails.siteName}
                        </span>
                    </Link>

                    {/* Desktop Menu */}
                    <ul className="hidden md:flex items-center gap-8">
                        {[
                            { url: '/features', text: t.nav.features },
                            { url: '/how-it-works', text: t.nav.howItWorks },
                            { url: '/#pricing', text: t.nav.pricing },
                            { url: '/download', text: t.nav.download },
                        ].map(item => (
                            <li key={item.url}>
                                <Link href={item.url} className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">
                                    {item.text}
                                </Link>
                            </li>
                        ))}
                    </ul>

                    {/* Desktop Right Side */}
                    <div className="hidden md:flex items-center gap-4">
                        {/* Language Switcher */}
                        <div className="relative">
                            <button
                                onClick={() => setLangOpen(!langOpen)}
                                className="flex items-center gap-1.5 px-3 py-2 text-sm font-medium text-muted-foreground hover:bg-muted rounded-lg transition-colors"
                            >
                                <FiGlobe size={15} />
                                {languages.find(l => l.code === locale)?.label}
                            </button>
                            {langOpen && (
                                <div className="absolute right-0 mt-2 w-28 bg-popover border border-border rounded-xl shadow-lg overflow-hidden z-50">
                                    {languages.map(lang => (
                                        <button
                                            key={lang.code}
                                            onClick={() => { setLocale(lang.code); setLangOpen(false); }}
                                            className={`block w-full text-left px-4 py-2.5 text-sm hover:bg-muted transition-colors ${locale === lang.code ? 'text-primary font-semibold bg-primary/10' : 'text-foreground'}`}
                                        >
                                            {lang.label}
                                        </button>
                                    ))}
                                </div>
                            )}
                        </div>

                        {/* Dark Mode Toggle */}
                        <button
                            onClick={toggleDark}
                            className="p-2 text-muted-foreground hover:bg-muted rounded-lg transition-colors"
                            aria-label={t.nav.toggleDarkMode}
                        >
                            {darkMode ? <FiSun size={17} /> : <FiMoon size={17} />}
                        </button>

                        {/* Get App Button */}
                        <a
                            href="https://play.google.com/store/apps/details?id=com.drivingschoolrwandaapp"
                            target="_blank"
                            rel="noopener noreferrer"
                            className="flex items-center gap-2 px-5 py-2.5 bg-primary text-primary-foreground text-sm font-semibold rounded-full transition-all hover:shadow-lg hover:shadow-primary/25"
                        >
                            <BsDownload size={15} />
                            {t.nav.getApp}
                        </a>
                    </div>

                    {/* Mobile Menu Button */}
                    <div className="md:hidden flex items-center gap-2">
                        <button onClick={toggleDark} className="p-2 text-muted-foreground">
                            {darkMode ? <FiSun size={17} /> : <FiMoon size={17} />}
                        </button>
                        <button
                            onClick={() => setIsOpen(!isOpen)}
                            className="p-2 text-muted-foreground"
                            aria-label={t.nav.toggleMenu}
                        >
                            {isOpen ? <HiOutlineXMark className="h-6 w-6" /> : <HiBars3 className="h-6 w-6" />}
                        </button>
                    </div>
                </nav>
            </div>

            {/* Mobile Menu */}
            {isOpen && (
                <div className="md:hidden bg-background border-t border-border shadow-lg">
                    <div className="max-w-7xl mx-auto px-6 py-4">
                        <ul className="space-y-1">
                            {[
                                { url: '/features', text: t.nav.features },
                                { url: '/how-it-works', text: t.nav.howItWorks },
                                { url: '/#pricing', text: t.nav.pricing },
                                { url: '/download', text: t.nav.download },
                            ].map(item => (
                                <li key={item.url}>
                                    <Link href={item.url} className="block px-4 py-3 text-sm font-medium text-foreground hover:bg-muted rounded-xl transition-colors" onClick={() => setIsOpen(false)}>
                                        {item.text}
                                    </Link>
                                </li>
                            ))}
                        </ul>
                        <div className="mt-4 pt-4 border-t border-border">
                            {/* Mobile Language Switcher */}
                            <div className="flex gap-2 mb-4">
                                {languages.map(lang => (
                                    <button
                                        key={lang.code}
                                        onClick={() => setLocale(lang.code)}
                                        className={`px-4 py-2 text-xs font-semibold rounded-full border transition-all ${locale === lang.code ? 'bg-primary text-primary-foreground border-primary' : 'border-border text-muted-foreground hover:border-primary'}`}
                                    >
                                        {lang.label}
                                    </button>
                                ))}
                            </div>
                            <a
                                href="https://play.google.com/store/apps/details?id=com.drivingschoolrwandaapp"
                                target="_blank"
                                rel="noopener noreferrer"
                                className="flex items-center justify-center gap-2 w-full px-5 py-3 bg-primary text-primary-foreground text-sm font-semibold rounded-full transition-all"
                                onClick={() => setIsOpen(false)}
                            >
                                <BsDownload size={15} />
                                {t.nav.getApp}
                            </a>
                        </div>
                    </div>
                </div>
            )}
        </header>
    );
};

export default Header;
