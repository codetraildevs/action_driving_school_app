import type { Metadata, Viewport } from "next";
import "./globals.css";
import { Toaster } from "sonner";
import { Syne } from "next/font/google";
import { Suspense } from "react";
import { Loader2 } from "lucide-react";
import { siteDetails } from "@/data/siteDetails";

// Render all pages on-demand instead of pre-rendering them at build time.
// Required for this cPanel host: static generation spawns export workers
// which exceed the account's CloudLinux process/thread (NPROC) limit
// ("spawn ... EAGAIN"). Dynamic rendering also avoids running DB/FCM
// code during the build and is the correct mode for an authenticated console.
export const dynamic = "force-dynamic";

const inter = Syne({ subsets: ["latin"] });

export const metadata: Metadata = {
    title: siteDetails.metadata.title,
    description: siteDetails.metadata.description,
    keywords: siteDetails.metadata.keywords,
    applicationName: siteDetails.metadata.applicationName,
    generator: siteDetails.metadata.generator,
    referrer: 'origin-when-cross-origin',
    icons: {
        icon: '/favicon.ico',
    },
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
                url: `${siteDetails.siteUrl}/og-image.jpg`,
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
        images: [`${siteDetails.siteUrl}/og-image.jpg`],
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
            </head>
            <body className={inter.className}>
                <Suspense
                    fallback={
                        <div className="flex items-center justify-center min-h-screen">
                            <Loader2 className="animate-spin"/>
                        </div>
                    }
                >
                    {children}
                </Suspense>
                <Toaster position="top-right" richColors />
            </body>
        </html>
    );
}
