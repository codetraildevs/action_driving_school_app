import { siteDetails } from "@/data/siteDetails";

/**
 * Site-wide structured data (JSON-LD), rendered once in the root layout.
 * Includes the website, the publisher organization and the Android app
 * itself so Google can link the Play Store listing with this site.
 */
export function siteStructuredData() {
    return {
        "@context": "https://schema.org",
        "@graph": [
            {
                "@type": "WebSite",
                "@id": `${siteDetails.siteUrl}/#website`,
                url: `${siteDetails.siteUrl}/`,
                name: `${siteDetails.siteName} Rwanda`,
                description: siteDetails.metadata.description,
                inLanguage: "en",
                publisher: { "@id": `${siteDetails.siteUrl}/#organization` },
            },
            {
                "@type": "Organization",
                "@id": `${siteDetails.siteUrl}/#organization`,
                name: siteDetails.siteName,
                url: `${siteDetails.siteUrl}/`,
                email: "info@amategekoyumuhanda.rw",
                telephone: "+250780765548",
                logo: {
                    "@type": "ImageObject",
                    url: `${siteDetails.siteUrl}/images/logo.png`,
                },
            },
            {
                "@type": "MobileApplication",
                "@id": `${siteDetails.siteUrl}/#app`,
                name: siteDetails.siteName,
                operatingSystem: "Android",
                applicationCategory: "EducationalApplication",
                description: siteDetails.metadata.description,
                url: `${siteDetails.siteUrl}/`,
                image: `${siteDetails.siteUrl}/og-image.png`,
                offers: {
                    "@type": "Offer",
                    price: "0",
                    priceCurrency: "RWF",
                },
                aggregateRating: {
                    "@type": "AggregateRating",
                    ratingValue: "4.8",
                    ratingCount: "1000",
                },
                installUrl:
                    "https://play.google.com/store/apps/details?id=com.drivingschoolrwandaapp",
            },
        ],
    };
}

export const structuredDataJsonLd = JSON.stringify(siteStructuredData());
