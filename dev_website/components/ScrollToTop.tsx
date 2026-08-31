'use client';

import React, { useState, useEffect } from 'react';
import { FiArrowUp } from 'react-icons/fi';

const ScrollToTop: React.FC = () => {
    const [isVisible, setIsVisible] = useState(false);

    useEffect(() => {
        const toggleVisibility = () => {
            if (window.scrollY > 300) {
                setIsVisible(true);
            } else {
                setIsVisible(false);
            }
        };

        window.addEventListener('scroll', toggleVisibility);
        return () => window.removeEventListener('scroll', toggleVisibility);
    }, []);

    const scrollToTop = () => {
        window.scrollTo({
            top: 0,
            behavior: 'smooth',
        });
    };

    if (!isVisible) return null;

    return (
        <button
            onClick={scrollToTop}
            className="fixed bottom-6 right-6 z-50 p-3 bg-primary hover:bg-primary-accent text-primary-foreground rounded-full shadow-lg hover:shadow-xl transition-all duration-300 transform hover:scale-110"
            aria-label="Scroll to top"
        >
            <FiArrowUp size={20} />
        </button>
    );
};

export default ScrollToTop;
