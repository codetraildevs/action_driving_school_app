import Header from "@/components/Header";
import Footer from "@/components/Footer";
import CTA from "@/components/CTA";
import type { Metadata } from "next";
import HowItWorksContent from "@/components/pages/HowItWorksContent";

export const metadata: Metadata = {
    title: "How It Works | Action Driving School App - Rwanda Driving Test Preparation",
    description: "Learn how to use Action Driving School App to prepare for Rwanda driving theory exam. Step-by-step guide: practice real questions, road signs, mock tests and get your driving license faster.",
};

export default function HowItWorksPage() {
    return (
        <>
            <Header />
            <HowItWorksContent />
            <CTA />
            <Footer />
        </>
    );
}
