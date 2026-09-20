import Header from "@/components/Header";
import Footer from "@/components/Footer";
import HomeContent from "@/components/pages/HomeContent";
import { faqs } from "@/data/faq";
import { siteDetails } from "@/data/siteDetails";

const faqStructuredData = JSON.stringify({
    "@context": "https://schema.org",
    "@type": "FAQPage",
    mainEntity: faqs.map((faq) => ({
        "@type": "Question",
        name: faq.question,
        acceptedAnswer: {
            "@type": "Answer",
            text: faq.answer,
        },
    })),
});

export default function Home() {
    return (
        <>
            <script
                type="application/ld+json"
                dangerouslySetInnerHTML={{ __html: faqStructuredData }}
                key={`faq-jsonld-${siteDetails.siteUrl}`}
            />
            <Header />
            <HomeContent />
            <Footer />
        </>
    );
}
