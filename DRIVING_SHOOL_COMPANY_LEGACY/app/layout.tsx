import type { Metadata } from "next";
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
  openGraph: {
    title: siteDetails.metadata.title,
    description: siteDetails.metadata.description,
    url: siteDetails.siteUrl,
    type: 'website',
    images: [
      {
        url: '/images/og-image.jpg',
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
    images: ['/images/twitter-image.jpg'],
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
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
