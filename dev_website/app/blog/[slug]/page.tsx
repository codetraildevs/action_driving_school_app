import type { Metadata } from "next";
import { notFound } from "next/navigation";
import Header from "@/components/Header";
import Footer from "@/components/Footer";
import BlogArticleView from "@/components/BlogArticleView";
import { blogPosts } from "@/content/blog";
import { pageMetadata } from "@/lib/pageMetadata";
import { siteDetails } from "@/data/siteDetails";

interface Props {
    params: Promise<{ slug: string }>;
}

/** URL path for a language variant of a post. English is the default (no suffix). */
export function variantPath(slug: string, lang: "en" | "fr" | "rw"): string {
    return lang === "en" ? `/blog/${slug}/` : `/blog/${slug}/${lang}/`;
}

export function generateStaticParams() {
    return blogPosts.map((post) => ({ slug: post.slug }));
}

/** hreflang map cross-linking all language variants of a post. */
function languagesFor(slug: string): Record<string, string> {
    const base = siteDetails.siteUrl;
    return {
        en: `${base}/blog/${slug}/`,
        fr: `${base}/blog/${slug}/fr/`,
        rw: `${base}/blog/${slug}/rw/`,
        "x-default": `${base}/blog/${slug}/`,
    };
}

const OG_LOCALE = { en: "en_RW", fr: "fr_RW", rw: "rw_RW" } as const;

export async function generateMetadata({ params }: Props): Promise<Metadata> {
    const { slug } = await params;
    const post = blogPosts.find((p) => p.slug === slug);
    if (!post) return {};
    const variant = post.translations.en;
    return pageMetadata(
        `/blog/${post.slug}/`,
        variant.title,
        variant.description,
        languagesFor(post.slug),
        OG_LOCALE.en
    );
}

export default async function BlogPostPage({ params }: Props) {
    const { slug } = await params;
    const post = blogPosts.find((p) => p.slug === slug);
    if (!post) notFound();

    return (
        <>
            <Header />
            <BlogArticleView
                post={post}
                lang="en"
                variantPath={(l) => variantPath(post.slug, l)}
            />
            <Footer />
        </>
    );
}
