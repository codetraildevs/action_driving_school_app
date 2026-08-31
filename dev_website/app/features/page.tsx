import Header from "@/components/Header";
import Footer from "@/components/Footer";
import Benefits from "@/components/Benefits";
import Stats from "@/components/Stats";
import type { Metadata } from "next";

export const metadata: Metadata = {
    title: "Features | Action Driving School App - Best Rwanda Driving Exam Practice",
    description: "Discover all powerful features of Action Driving School App: real Rwanda driving exam questions 2026/2026, road signs, traffic laws in Kinyarwanda & English, progress tracking, mock tests and more.",
};

export default function FeaturesPage() {
    return (
        <>
            <Header />
            <main className="pt-32 pb-20 px-5">
                <div className="max-w-4xl mx-auto text-center mb-16">
                    <h1 className="text-4xl md:text-6xl font-bold mb-6">
                        Features
                    </h1>
                    <p className="text-lg text-foreground-accent max-w-2xl mx-auto">
                        Everything you need to pass your Rwanda driving theory exam.
                        Our app provides comprehensive preparation tools.
                    </p>
                </div>
                <Stats />
                <div id="benefits">
                    <Benefits />
                </div>
            </main>
            <Footer />
        </>
    );
}
