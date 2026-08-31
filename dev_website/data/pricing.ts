import { IPricing } from "@/types";

export const tiers: IPricing[] = [
    {
        name: 'Free',
        price: 'Free',
        features: [
            '20+ Practice questions',
            'Basic road signs guide',
            '1 Mock tests per day',
            'Kinyarwanda, English & French',
        ],
    },
    {
        name: 'Premium',
        price: 5000,
        currency: 'RWF',
        features: [
            '1000+ Practice questions',
            'All road signs with images',
            'Unlimited mock tests',
            'Kinyarwanda, English & French',
            'Detailed progress tracking',
            'Offline mode',
        ],
    },
    {
        name: 'Premium Plus',
        price: 10000,
        currency: 'RWF',
        features: [
            'Everything in Premium',
            'Priority support via WhatsApp',
            'Exclusive exam tips',
            'Early access to new features',
            'Ad-free experience',
            'Certificate of completion',
        ],
    },
]