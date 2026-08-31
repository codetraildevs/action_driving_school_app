export interface IMenuItem {
    text: string;
    url: string;
}

export interface ISocials {
    [key: string]: string;
}

export interface IBenefitBullet {
    title: string;
    description: string;
    icon: React.ReactNode;
}

export interface IBenefit {
    title: string;
    description: string;
    bullets: IBenefitBullet[];
    imageSrc: string;
}

export interface IFAQ {
    question: string;
    answer: string;
}

export interface IPricing {
    name: string;
    price: number | string;
    currency?: string;
    features: string[];
}

export interface IStats {
    title: string;
    icon: React.ReactNode;
    description: string;
}

export interface ITestimonial {
    name: string;
    role: string;
    message: string;
    avatar: string;
}
