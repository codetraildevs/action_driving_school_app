import type { MetadataRoute } from "next";
import { siteDetails } from "@/data/siteDetails";
import { blogPosts } from "@/content/blog";

// Required for `output: 'export'` — renders sitemap.xml at build time.
export const dynamic = "force-static";

/**
 * Static sitemap generated at build time into out/sitemap.xml.
 * URLs use trailing slashes to match `trailingSlash: true` and the
 * canonical URLs emitted by pageMetadata().
 */
export default function sitemap(): MetadataRoute.Sitemap {
    const base = siteDetails.siteUrl;
    const lastModified = new Date();

    return [
        {
            url: `${base}/`,
            lastModified,
            changeFrequency: 'weekly',
            priority: 1,
        },
        {
            url: `${base}/download/`,
            lastModified,
            changeFrequency: 'monthly',
            priority: 0.9,
        },
        {
            url: `${base}/features/`,
            lastModified,
            changeFrequency: 'monthly',
            priority: 0.8,
        },
        {
            url: `${base}/how-it-works/`,
            lastModified,
            changeFrequency: 'monthly',
            priority: 0.8,
        },
        {
            url: `${base}/blog/`,
            lastModified,
            changeFrequency: 'weekly',
            priority: 0.7,
        },
        ...blogPosts.map((post) => ({
            url: `${base}/blog/${post.slug}/`,
            lastModified: new Date(post.date),
            changeFrequency: 'monthly' as const,
            priority: 0.6,
        })),
    ];
}
