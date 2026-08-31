import { IMenuItem, ISocials } from "@/types";

export const footerDetails: {
    subheading: string;
    quickLinks: IMenuItem[];
    email: string;
    telephone: string;
    socials: ISocials;
} = {
    subheading: "Preparing Rwandan drivers for success with real exam practice questions and road safety training.",
    quickLinks: [
        {
            text: "Features",
            url: "#features"
        },
        {
            text: "Benefits",
            url: "#benefits"
        },
        {
            text: "FAQ",
            url: "#faq"
        }
    ],
    email: 'info@amategekoyumuhanda.rw',
    telephone: '+250 780 765 548',
    socials: {}
}