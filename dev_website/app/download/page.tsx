import Header from "@/components/Header";
import Footer from "@/components/Footer";
import CTA from "@/components/CTA";
import type { Metadata } from "next";
import DownloadContent from "@/components/pages/DownloadContent";

export const metadata: Metadata = {
    title: "Download Action Driving School App - Rwanda Driving Exam Practice",
    description: "Download Action Driving School App now! Practice real Rwanda driving theory test questions 2026/2026. Available on Google Play Store + Direct APK download for Rwanda users.",
};

export default function DownloadPage() {
    return (
        <>
            <Header />
            <DownloadContent />
            <CTA />
            <Footer />
        </>
    );
}
