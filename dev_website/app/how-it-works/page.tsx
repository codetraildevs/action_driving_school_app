import Header from "@/components/Header";
import Footer from "@/components/Footer";
import CTA from "@/components/CTA";
import type { Metadata } from "next";
import { FiDownload, FiUserPlus, FiBookOpen, FiCheckCircle, FiTruck, FiWifiOff, FiRefreshCw, FiBarChart2, FiGlobe } from "react-icons/fi";

export const metadata: Metadata = {
    title: "How It Works | Action Driving School App - Rwanda Driving Test Preparation",
    description: "Learn how to use Action Driving School App to prepare for Rwanda driving theory exam. Step-by-step guide: practice real questions, road signs, mock tests and get your driving license faster.",
};

const steps = [
    {
        number: 1,
        icon: <FiDownload size={20} />,
        title: "Download the App",
        description: "Get Action Driving School App from your Google Play Store in seconds.",
        image: "/mobile-app-download-screen-with-play-store-icon.jpg",
    },
    {
        number: 2,
        icon: <FiUserPlus size={20} />,
        title: "Create Your Account",
        description: "Sign up with your name and phone number to get started.",
        image: "/images/Screenshot_2026-01-02_014048-removebg-preview.png",
    },
    {
        number: 3,
        icon: <FiBookOpen size={20} />,
        title: "Start Learning",
        description: "Choose your learning path and begin mastering road laws at your pace.",
        image: "/images/Screenshot_2026-01-02_015203-removebg-preview.png",
    },
    {
        number: 4,
        icon: <FiCheckCircle size={20} />,
        title: "Practice & Test",
        description: "Complete quizzes, track progress, and measure your understanding.",
        image: "/images/Screenshot_2026-01-02_015320-removebg-preview.png",
    },
    {
        number: 5,
        icon: <FiTruck size={20} />,
        title: "Drive Safely",
        description: "Apply your knowledge and become a responsible, law-abiding driver.",
        image: "/safe-driver-on-road-with-traffic-signals.jpg",
    },
];

export default function HowItWorksPage() {
    return (
        <>
            <Header />
            <main className="pt-28 md:pt-36 pb-16 px-6">
                {/* Page Header */}
                <div className="max-w-4xl mx-auto text-center mb-16">
                    <h1 className="text-4xl md:text-5xl lg:text-6xl font-bold mb-6">
                        How It Works
                    </h1>
                    <p className="text-lg text-foreground-accent max-w-2xl mx-auto leading-relaxed">
                        Get started in 5 simple steps and prepare for your driving theory exam with confidence.
                    </p>
                </div>

                {/* 5 Steps */}
                <div className="max-w-7xl mx-auto">
                    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 xl:grid-cols-5 gap-6">
                        {steps.map((step) => (
                            <div key={step.number} className="flex flex-col">
                                {/* Image */}
                                <div className="w-full h-48 rounded-2xl overflow-hidden mb-5 bg-muted">
                                    <img
                                        src={step.image}
                                        alt={step.title}
                                        className="w-full h-full object-cover"
                                    />
                                </div>

                                {/* Number + Icon */}
                                <div className="flex items-center gap-3 mb-3">
                                    <div className="w-10 h-10 rounded-full bg-muted flex items-center justify-center text-sm font-bold text-foreground">
                                        {step.number}
                                    </div>
                                    <div className="text-gray-500 dark:text-gray-400">
                                        {step.icon}
                                    </div>
                                </div>

                                {/* Title */}
                                <h3 className="text-xl font-bold text-foreground mb-2">
                                    {step.title}
                                </h3>

                                {/* Description */}
                                <p className="text-sm text-foreground-accent leading-relaxed">
                                    {step.description}
                                </p>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Why Students Love Our App */}
                <div className="max-w-7xl mx-auto mt-20">
                    <h2 className="text-3xl font-bold text-center mb-12">Why Students Love Our App</h2>
                    <div className="grid md:grid-cols-2 gap-8">
                        <div className="p-6 rounded-2xl bg-background border border-border">
                            <div className="flex items-center gap-2 mb-2">
                                <FiWifiOff size={18} className="text-primary" />
                                <h3 className="text-lg font-semibold">Works Offline</h3>
                            </div>
                            <p className="text-foreground-accent">
                                Download questions once and practice anywhere, even without internet.
                            </p>
                        </div>
                        <div className="p-6 rounded-2xl bg-background border border-border">
                            <div className="flex items-center gap-2 mb-2">
                                <FiRefreshCw size={18} className="text-primary" />
                                <h3 className="text-lg font-semibold">Always Updated</h3>
                            </div>
                            <p className="text-foreground-accent">
                                Our question bank is regularly updated to match the latest exam format.
                            </p>
                        </div>
                        <div className="p-6 rounded-2xl bg-background border border-border">
                            <div className="flex items-center gap-2 mb-2">
                                <FiBarChart2 size={18} className="text-primary" />
                                <h3 className="text-lg font-semibold">Track Progress</h3>
                            </div>
                            <p className="text-foreground-accent">
                                See your improvement over time with detailed progress tracking.
                            </p>
                        </div>
                        <div className="p-6 rounded-2xl bg-background border border-border">
                            <div className="flex items-center gap-2 mb-2">
                                <FiGlobe size={18} className="text-primary" />
                                <h3 className="text-lg font-semibold">Bilingual</h3>
                            </div>
                            <p className="text-foreground-accent">
                                Study in Kinyarwanda or English — switch languages anytime.
                            </p>
                        </div>
                    </div>
                </div>
            </main>
            <CTA />
            <Footer />
        </>
    );
}
