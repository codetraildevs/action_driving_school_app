'use client';

import React from 'react';
import Image from 'next/image';
import { testimonials } from '@/data/testimonials';
import SectionTitle from './SectionTitle';
import { useLanguage } from '@/lib/LanguageContext';

const Testimonials: React.FC = () => {
    const { t } = useLanguage();
    return (
        <section id="testimonials" className="py-16 md:py-24 px-6">
            <div className="max-w-7xl mx-auto">
                <div className="text-center mb-12">
                    <SectionTitle>
                        <h2 className="my-3 !leading-snug">{t.testimonials.title}</h2>
                    </SectionTitle>
                    <p className="mt-4 text-foreground-accent max-w-2xl mx-auto">
                        {t.testimonials.subtitle}
                    </p>
                </div>
                <div className="grid gap-14 max-w-lg w-full mx-auto lg:gap-8 lg:grid-cols-3 lg:max-w-full">
                    {testimonials.map((testimonial, index) => (
                        <div
                            key={index}
                            className="bg-card border border-border rounded-2xl p-6 shadow-sm"
                        >
                            <div className="flex items-center mb-4 w-full justify-center lg:justify-start">
                                <Image
                                    src={testimonial.avatar}
                                    alt={`${testimonial.name} avatar`}
                                    width={50}
                                    height={50}
                                    className="rounded-full shadow-md"
                                />
                                <div className="ml-4">
                                    <h3 className="text-lg font-semibold text-foreground">{testimonial.name}</h3>
                                    <p className="text-sm text-foreground-accent">{testimonial.role}</p>
                                </div>
                            </div>
                            <p className="text-foreground-accent text-center lg:text-left">&quot;{testimonial.message}&quot;</p>
                        </div>
                    ))}
                </div>
            </div>
        </section>
    );
};

export default Testimonials;
