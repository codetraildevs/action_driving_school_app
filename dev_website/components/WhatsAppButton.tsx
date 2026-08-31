'use client';

import React, { useState } from 'react';
import { FiMessageCircle, FiX } from 'react-icons/fi';

const WhatsAppButton: React.FC = () => {
    const [isOpen, setIsOpen] = useState(false);
    const phoneNumber = '+250780765548';
    const defaultMessage = 'Hello! I have a question about Action Driving School App.';

    const handleWhatsAppClick = (message?: string) => {
        const text = message || defaultMessage;
        const url = `https://wa.me/${phoneNumber.replace('+', '')}?text=${encodeURIComponent(text)}`;
        window.open(url, '_blank');
        setIsOpen(false);
    };

    return (
        <div className="fixed bottom-6 left-6 z-50">
            {isOpen && (
                <div className="mb-4 bg-popover rounded-2xl shadow-2xl border border-border p-4 w-72">
                    <div className="flex items-center justify-between mb-3">
                        <h3 className="font-semibold text-foreground">Chat with us</h3>
                        <button
                            onClick={() => setIsOpen(false)}
                            className="text-muted-foreground hover:text-foreground"
                        >
                            <FiX size={18} />
                        </button>
                    </div>
                    <p className="text-sm text-muted-foreground mb-3">
                        Have questions? Chat with us on WhatsApp!
                    </p>
                    <div className="space-y-2">
                        <button
                            onClick={() => handleWhatsAppClick('Hello! I have a question about Action Driving School App.')}
                            className="w-full text-left px-3 py-2 text-sm bg-muted hover:bg-muted/80 text-foreground rounded-lg transition-colors"
                        >
                            General inquiry
                        </button>
                        <button
                            onClick={() => handleWhatsAppClick('I need help with the app.')}
                            className="w-full text-left px-3 py-2 text-sm bg-muted hover:bg-muted/80 text-foreground rounded-lg transition-colors"
                        >
                            Need support
                        </button>
                        <button
                            onClick={() => handleWhatsAppClick('I would like to know about premium plans.')}
                            className="w-full text-left px-3 py-2 text-sm bg-muted hover:bg-muted/80 text-foreground rounded-lg transition-colors"
                        >
                            Premium plans
                        </button>
                    </div>
                </div>
            )}
            <button
                onClick={() => setIsOpen(!isOpen)}
                className="p-4 bg-green-500 hover:bg-green-600 text-white rounded-full shadow-lg hover:shadow-xl transition-all duration-300 transform hover:scale-110"
                aria-label="Chat on WhatsApp"
            >
                <FiMessageCircle size={24} />
            </button>
        </div>
    );
};

export default WhatsAppButton;
