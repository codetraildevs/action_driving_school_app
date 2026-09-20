import Link from "next/link";
import type { BlogPost, BlogLang } from "@/content/blog/types";
import { blogPosts } from "@/content/blog";
import { siteDetails } from "@/data/siteDetails";

interface Props {
    post: BlogPost;
    lang: BlogLang;
    variantPath: (lang: BlogLang) => string;
}

function formatDate(iso: string) {
    return new Date(iso).toLocaleDateString("en-GB", {
        day: "numeric",
        month: "long",
        year: "numeric",
    });
}

/** Shared renderer for /blog/[slug]/ (en) and /blog/[slug]/[lang]/ (fr, rw). */
export default function BlogArticleView({ post, lang, variantPath }: Props) {
    const variant = post.translations[lang];
    const others = blogPostsWithLang(post, lang);

    const articleJsonLd = JSON.stringify({
        "@context": "https://schema.org",
        "@type": "Article",
        headline: variant.title,
        description: variant.description,
        datePublished: post.date,
        dateModified: post.date,
        inLanguage: lang,
        author: {
            "@type": "Organization",
            name: siteDetails.siteName,
            url: `${siteDetails.siteUrl}/`,
        },
        publisher: {
            "@type": "Organization",
            name: siteDetails.siteName,
            logo: {
                "@type": "ImageObject",
                url: `${siteDetails.siteUrl}/images/logo.png`,
            },
        },
        mainEntityOfPage: {
            "@type": "WebPage",
            "@id": `${siteDetails.siteUrl}${variantPath(lang)}`,
        },
    });

    const langLabels: Record<BlogLang, string> = {
        en: "English",
        fr: "Français",
        rw: "Kinyarwanda",
    };

    return (
        <>
            <script
                type="application/ld+json"
                dangerouslySetInnerHTML={{ __html: articleJsonLd }}
            />
            <main className="pt-24 md:pt-32 pb-16 md:pb-24 px-6 min-h-screen">
                <article className="max-w-3xl mx-auto">
                    <nav className="text-sm text-muted-foreground mb-6">
                        <Link href="/" className="hover:text-foreground">Home</Link>
                        <span className="mx-2">/</span>
                        <Link href="/blog/" className="hover:text-foreground">Blog</Link>
                    </nav>
                    <h1 className="text-3xl md:text-4xl lg:text-5xl font-bold text-foreground mb-4 leading-tight">
                        {variant.title}
                    </h1>
                    <time
                        dateTime={post.date}
                        className="text-sm text-muted-foreground block mb-2"
                    >
                        {formatDate(post.date)}
                        {lang !== "en" ? ` · ${langLabels[lang]}` : ""}
                    </time>

                    {lang === "en" && (
                        <div className="text-sm mb-8 flex flex-wrap items-center gap-x-2">
                            <span className="text-muted-foreground">
                                Read in:
                            </span>
                            {(["en", "fr", "rw"] as BlogLang[]).map((l, i) => (
                                <span key={l}>
                                    {i > 0 && (
                                        <span className="text-muted-foreground mr-2">·</span>
                                    )}
                                    {l === lang ? (
                                        <span className="font-semibold text-foreground">
                                            {langLabels[l]}
                                        </span>
                                    ) : (
                                        <Link
                                            href={variantPath(l)}
                                            className="text-primary hover:underline"
                                        >
                                            {langLabels[l]}
                                        </Link>
                                    )}
                                </span>
                            ))}
                        </div>
                    )}
                    {lang !== "en" && <div className="mb-8" />}

                    <div className="text-lg text-foreground-accent leading-relaxed [&_h2]:text-2xl [&_h2]:font-bold [&_h2]:text-foreground [&_h2]:mt-10 [&_h2]:mb-4 [&_h3]:text-xl [&_h3]:font-semibold [&_h3]:text-foreground [&_h3]:mt-6 [&_h3]:mb-3 [&_p]:mb-4 [&_ul]:mb-6 [&_ul]:list-disc [&_ul]:pl-6 [&_ol]:mb-6 [&_ol]:list-decimal [&_ol]:pl-6 [&_li]:mb-2 [&_a]:text-primary [&_a]:underline [&_a:hover]:opacity-80">
                        <variant.Content />
                    </div>

                    <div className="mt-12 p-6 md:p-8 border border-border rounded-2xl bg-muted/30">
                        <h2 className="text-xl font-bold mb-2">
                            Prepare with real exam questions
                        </h2>
                        <p className="text-foreground-accent mb-4">
                            Practice with 1,000+ real Rwanda driving theory
                            exam questions, road signs and timed mock tests —
                            free to start.
                        </p>
                        <Link
                            href="/download/"
                            className="inline-block px-6 py-3 bg-primary text-primary-foreground font-semibold rounded-full hover:shadow-lg transition-all"
                        >
                            Download the app
                        </Link>
                    </div>

                    {others.length > 0 && (
                        <div className="mt-12">
                            <h2 className="text-xl font-bold mb-4">Keep reading</h2>
                            <ul className="space-y-3">
                                {others.map((p) => (
                                    <li key={p.slug}>
                                        <Link
                                            href={variantPathFor(p, lang)}
                                            className="text-primary font-semibold hover:underline"
                                        >
                                            {p.translations[lang].title}
                                        </Link>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    )}
                </article>
            </main>
        </>
    );
}

function blogPostsWithLang(post: BlogPost, lang: BlogLang): BlogPost[] {
    return blogPosts.filter((p) => p.slug !== post.slug);
}

function variantPathFor(post: BlogPost, lang: BlogLang): string {
    return lang === "en"
        ? `/blog/${post.slug}/`
        : `/blog/${post.slug}/${lang}/`;
}
