import Header from "@/components/Header";
import Footer from "@/components/Footer";
import CTA from "@/components/CTA";
import type { Metadata } from "next";
import DownloadContent from "@/components/pages/DownloadContent";
import { pageMetadata } from "@/lib/pageMetadata";

export const metadata: Metadata = pageMetadata(
    '/download/',
    "Download App | Rwanda Driving Test Practice - Action Driving School",
    "Download Action Driving School App now! Practice real Rwanda driving theory test questions 2026/2026. Available on Google Play Store + Direct APK download for Rwanda users."
);

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
