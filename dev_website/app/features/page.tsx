import Header from "@/components/Header";
import Footer from "@/components/Footer";
import type { Metadata } from "next";
import FeaturesContent from "@/components/pages/FeaturesContent";

export const metadata: Metadata = {
    title: "Features | Action Driving School App - Best Rwanda Driving Exam Practice",
    description: "Discover all powerful features of Action Driving School App: real Rwanda driving exam questions 2026/2026, road signs, traffic laws in Kinyarwanda & English, progress tracking, mock tests and more.",
};

export default function FeaturesPage() {
    return (
        <>
            <Header />
            <FeaturesContent />
            <Footer />
        </>
    );
}
