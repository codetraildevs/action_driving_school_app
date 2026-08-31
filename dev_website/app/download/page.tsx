import Header from "@/components/Header";
import Footer from "@/components/Footer";
import CTA from "@/components/CTA";
import type { Metadata } from "next";

export const metadata: Metadata = {
    title: "Download Action Driving School App - Rwanda Driving Exam Practice",
    description: "Download Action Driving School App now! Practice real Rwanda driving theory test questions 2026/2026. Available on Google Play Store + Direct APK download for Rwanda users.",
};

export default function DownloadPage() {
    return (
        <>
            <Header />
            <main className="pt-32 pb-20 px-5">
                <div className="max-w-4xl mx-auto text-center">
                    <h1 className="text-4xl md:text-6xl font-bold mb-6">
                        Download Action Driving School
                    </h1>
                    <p className="text-lg text-foreground-accent mb-8 max-w-2xl mx-auto">
                        Start preparing for your Rwanda driving theory exam today.
                        Practice with real questions, road signs, and mock tests.
                    </p>

                    <div className="flex flex-col sm:flex-row items-center justify-center gap-6 mb-16">
                        <a
                            href="https://play.google.com/store/apps/details?id=com.drivingschoolrwandaapp"
                            target="_blank"
                            rel="noopener noreferrer"
                            className="inline-block"
                        >
                            <img
                                src="/Google_Play_Store_badge_EN.svg"
                                alt="Get it on Google Play"
                                className="h-16"
                            />
                        </a>
                    </div>

                    <div className="grid md:grid-cols-3 gap-8 mt-16">
                        <div className="p-6 rounded-2xl bg-background border border-border">
                            <h3 className="text-xl font-semibold mb-3">Real Exam Questions</h3>
                            <p className="text-foreground-accent">
                                Practice with actual Rwanda driving theory exam questions updated regularly.
                            </p>
                        </div>
                        <div className="p-6 rounded-2xl bg-background border border-border">
                            <h3 className="text-xl font-semibold mb-3">Mock Tests</h3>
                            <p className="text-foreground-accent">
                                Simulate the real exam experience with timed practice tests.
                            </p>
                        </div>
                        <div className="p-6 rounded-2xl bg-background border border-border">
                            <h3 className="text-xl font-semibold mb-3">Bilingual Support</h3>
                            <p className="text-foreground-accent">
                                Available in both Kinyarwanda and English for your convenience.
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
