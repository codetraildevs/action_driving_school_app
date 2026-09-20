import type { Metadata } from "next";
import { siteDetails } from "@/data/siteDetails";

/**
 * Builds complete per-page metadata: canonical URL, Open Graph and Twitter
 * cards. Every non-home page must use this (or its own alternates) so it does
 * not inherit the root layout's homepage canonical.
 *
 * `languages` (hreflang map) and `ogLocale` are used by the trilingual blog
 * posts to cross-link their language variants.
 */
export function pageMetadata(
    path: string,
    title: string,
    description: string,
    languages?: Record<string, string>,
    ogLocale?: string
): Metadata {
    const url = `${siteDetails.siteUrl}${path}`;
    const image = {
        url: `${siteDetails.siteUrl}/og-image.png`,
        width: 1200,
        height: 675,
        alt: siteDetails.siteName,
    };

    return {
        title,
        description,
        alternates: {
            canonical: url,
            ...(languages ? { languages } : {}),
        },
        openGraph: {
            title,
            description,
            url,
            siteName: `${siteDetails.siteName} Rwanda`,
            locale: ogLocale ?? siteDetails.locale,
            type: 'website',
            images: [image],
        },
        twitter: {
            card: 'summary_large_image',
            title,
            description,
            images: [image.url],
        },
    };
}
