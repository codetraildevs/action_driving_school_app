import type { Metadata } from "next";
import { notFound } from "next/navigation";
import Header from "@/components/Header";
import Footer from "@/components/Footer";
import BlogArticleView from "@/components/BlogArticleView";
import { blogPosts } from "@/content/blog";
import { BLOG_LANGS, type BlogLang } from "@/content/blog/types";
import { pageMetadata } from "@/lib/pageMetadata";
import { siteDetails } from "@/data/siteDetails";

interface Props {
    params: Promise<{ slug: string; lang: string }>;
}

function variantPath(slug: string, lang: BlogLang): string {
    return lang === "en" ? `/blog/${slug}/` : `/blog/${slug}/${lang}/`;
}

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

export function generateStaticParams() {
    return blogPosts.flatMap((post) =>
        BLOG_LANGS.filter((l) => l !== "en").map((lang) => ({
            slug: post.slug,
            lang,
        }))
    );
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
    const { slug, lang: rawLang } = await params;
    if (rawLang !== "fr" && rawLang !== "rw") return {};
    const lang: BlogLang = rawLang;
    const post = blogPosts.find((p) => p.slug === slug);
    if (!post) return {};
    const variant = post.translations[lang];
    return pageMetadata(
        `/blog/${post.slug}/${lang}/`,
        variant.title,
        variant.description,
        languagesFor(post.slug),
        OG_LOCALE[lang]
    );
}

export default async function BlogPostLangPage({ params }: Props) {
    const { slug, lang: rawLang } = await params;
    if (rawLang !== "fr" && rawLang !== "rw") notFound();
    const lang: BlogLang = rawLang;
    const post = blogPosts.find((p) => p.slug === slug);
    if (!post) notFound();

    return (
        <>
            <Header />
            <BlogArticleView
                post={post}
                lang={lang}
                variantPath={(l) => variantPath(post.slug, l)}
            />
            <Footer />
        </>
    );
}
