import React from 'react';
import { heroDetails } from '@/data/hero';
import PlayStoreButton from './PlayStoreButton';

const Hero: React.FC = () => {
    return (
        <section id="hero" className="relative pb-0 pt-24 md:pt-32 px-6">
            {/* Grid pattern background */}
            <div className="absolute left-0 top-0 bottom-0 -z-10 w-full">
                <div className="absolute inset-0 h-full w-full bg-hero-background bg-[linear-gradient(to_right,#80808012_1px,transparent_1px),linear-gradient(to_bottom,#80808012_1px,transparent_1px)] bg-[size:40px_40px] [mask-image:radial-gradient(ellipse_50%_50%_at_50%_50%,#000_60%,transparent_100%)]">
                </div>
            </div>

            {/* Bottom fade */}
            <div className="absolute left-0 right-0 bottom-0 backdrop-blur-[2px] h-40 bg-gradient-to-b from-transparent via-[rgba(233,238,255,0.5)] to-[rgba(202,208,230,0.5)]">
            </div>

            <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center gap-8 md:gap-12 lg:gap-20">
                {/* Left: Text Content */}
                <div className="w-full md:w-1/2 text-center md:text-left">
                    <h1 className="text-4xl md:text-5xl lg:text-6xl font-bold text-foreground leading-tight tracking-tight">
                        {heroDetails.heading}
                    </h1>
                    <p className="mt-5 text-xl text-foreground-accent max-w-xl mx-auto md:mx-0 leading-relaxed">
                        {heroDetails.subheading}
                    </p>
                    <div className="mt-7 flex justify-center md:justify-start">
                        <PlayStoreButton />
                    </div>
                </div>

                {/* Right: Image */}
                <div className="w-full md:w-1/2 flex justify-center">
                    <img
                        src={heroDetails.centerImageSrc}
                        alt="app mockup"
                        className="max-w-xs md:max-w-sm w-full relative z-10"
                    />
                </div>
            </div>
        </section>
    );
};

export default Hero;
