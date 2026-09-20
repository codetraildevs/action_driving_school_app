import type { Metadata, Viewport } from "next";
import Script from "next/script";
import "./globals.css";
import { Toaster } from "sonner";
import { Syne } from "next/font/google";
import { Suspense } from "react";
import { Loader2 } from "lucide-react";
import { siteDetails } from "@/data/siteDetails";
import { structuredDataJsonLd } from "@/lib/structuredData";
import { LanguageProvider } from "@/lib/LanguageContext";
import ScrollToTop from "@/components/ScrollToTop";
import WhatsAppButton from "@/components/WhatsAppButton";

const inter = Syne({ subsets: ["latin"] });

export const metadata: Metadata = {
    metadataBase: new URL(siteDetails.siteUrl),
    title: siteDetails.metadata.title,
    description: siteDetails.metadata.description,
    keywords: siteDetails.metadata.keywords,
    applicationName: siteDetails.metadata.applicationName,
    generator: siteDetails.metadata.generator,
    referrer: 'origin-when-cross-origin',
    icons: {
        icon: '/favicon.ico',
    },
    manifest: '/site.webmanifest',
    appleWebApp: {
        capable: true,
        title: siteDetails.metadata.title,
        statusBarStyle: 'default',
    },
    other: {
        'mobile-web-app-capable': 'yes',
    },
    openGraph: {
        title: siteDetails.metadata.title,
        description: siteDetails.metadata.description,
        url: siteDetails.siteUrl,
        siteName: `${siteDetails.siteName} Rwanda`,
        locale: siteDetails.locale,
        type: 'website',
        images: [
            {
                url: `${siteDetails.siteUrl}/og-image.png`,
                width: 1200,
                height: 675,
                alt: siteDetails.siteName,
            },
        ],
    },
    twitter: {
        card: 'summary_large_image',
        title: siteDetails.metadata.title,
        description: siteDetails.metadata.description,
        images: [`${siteDetails.siteUrl}/og-image.png`],
    },
    alternates: {
        canonical: siteDetails.siteUrl,
    },
};

export const viewport: Viewport = {
    width: 'device-width',
    initialScale: 1,
};

export default function RootLayout({
    children,
}: Readonly<{
    children: React.ReactNode;
}>) {
    return (
        <html lang="en" suppressHydrationWarning>
            <head>
                <script
                    dangerouslySetInnerHTML={{
                        __html: `
              if (localStorage.getItem('theme') === 'dark' || 
                  (!localStorage.getItem('theme') && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
                document.documentElement.classList.add('dark')
              }
            `,
                    }}
                />
                <link rel="preconnect" href="https://fonts.googleapis.com" />
                <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
                {siteDetails.googleTagManagerId && (
                    <script
                        id="gtm-init"
                        dangerouslySetInnerHTML={{
                            __html: `(function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({'gtm.start':
new Date().getTime(),event:'gtm.js'});var f=d.getElementsByTagName(s)[0],
j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';j.async=true;j.src=
'https://www.googletagmanager.com/gtm.js?id='+i+dl;f.parentNode.insertBefore(j,f);
})(window,document,'script','dataLayer','${siteDetails.googleTagManagerId}');`,
                        }}
                    />
                )}
            </head>
            <body className={inter.className}>
                {siteDetails.googleTagManagerId && (
                    <noscript>
                        <iframe
                            src={`https://www.googletagmanager.com/ns.html?id=${siteDetails.googleTagManagerId}`}
                            height="0"
                            width="0"
                            style={{ display: 'none', visibility: 'hidden' }}
                            title="Google Tag Manager"
                        />
                    </noscript>
                )}
                <script
                    type="application/ld+json"
                    dangerouslySetInnerHTML={{ __html: structuredDataJsonLd }}
                />
                <Suspense
                    fallback={
                        <div className="flex items-center justify-center min-h-screen">
                            <Loader2 className="animate-spin"/>
                        </div>
                    }
                >
                    <LanguageProvider>
                        {children}
                    </LanguageProvider>
                </Suspense>
                <ScrollToTop />
                <WhatsAppButton />
                <Toaster position="top-right" richColors />
                {siteDetails.googleAnalyticsId && (
                    <>
                        <Script
                            src={`https://www.googletagmanager.com/gtag/js?id=${siteDetails.googleAnalyticsId}`}
                            strategy="afterInteractive"
                        />
                        <Script id="ga4-init" strategy="afterInteractive">
                            {`
                                window.dataLayer = window.dataLayer || [];
                                function gtag(){dataLayer.push(arguments);}
                                gtag('js', new Date());
                                gtag('config', '${siteDetails.googleAnalyticsId}');
                            `}
                        </Script>
                    </>
                )}
            </body>
        </html>
    );
}
