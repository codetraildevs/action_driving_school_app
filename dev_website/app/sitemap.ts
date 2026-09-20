import type { MetadataRoute } from "next";
import { siteDetails } from "@/data/siteDetails";
import { blogPosts } from "@/content/blog";
import { BLOG_LANGS } from "@/content/blog/types";

// Required for `output: 'export'` — renders sitemap.xml at build time.
export const dynamic = "force-static";

/**
 * Static sitemap generated at build time into out/sitemap.xml.
 * URLs use trailing slashes to match `trailingSlash: true` and the
 * canonical URLs emitted by pageMetadata(). Blog posts list all language
 * variants with hreflang alternates.
 */
export default function sitemap(): MetadataRoute.Sitemap {
    const base = siteDetails.siteUrl;
    const lastModified = new Date();

    const postEntries: MetadataRoute.Sitemap = blogPosts.flatMap((post) =>
        BLOG_LANGS.map((lang) => ({
            url:
                lang === "en"
                    ? `${base}/blog/${post.slug}/`
                    : `${base}/blog/${post.slug}/${lang}/`,
            lastModified: new Date(post.date),
            changeFrequency: "monthly" as const,
            priority: 0.6,
            alternates: {
                languages: {
                    en: `${base}/blog/${post.slug}/`,
                    fr: `${base}/blog/${post.slug}/fr/`,
                    rw: `${base}/blog/${post.slug}/rw/`,
                    "x-default": `${base}/blog/${post.slug}/`,
                },
            },
        }))
    );

    return [
        {
            url: `${base}/`,
            lastModified,
            changeFrequency: "weekly",
            priority: 1,
        },
        {
            url: `${base}/download/`,
            lastModified,
            changeFrequency: "monthly",
            priority: 0.9,
        },
        {
            url: `${base}/features/`,
            lastModified,
            changeFrequency: "monthly",
            priority: 0.8,
        },
        {
            url: `${base}/how-it-works/`,
            lastModified,
            changeFrequency: "monthly",
            priority: 0.8,
        },
        {
            url: `${base}/blog/`,
            lastModified,
            changeFrequency: "weekly",
            priority: 0.7,
        },
        ...postEntries,
    ];
}
