import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import Header from "@/components/Header";
import Footer from "@/components/Footer";
import { blogPosts } from "@/content/blog";
import { pageMetadata } from "@/lib/pageMetadata";
import { siteDetails } from "@/data/siteDetails";

interface Props {
    params: Promise<{ slug: string }>;
}

export function generateStaticParams() {
    return blogPosts.map((post) => ({ slug: post.slug }));
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
    const { slug } = await params;
    const post = blogPosts.find((p) => p.slug === slug);
    if (!post) return {};
    return pageMetadata(
        `/blog/${post.slug}/`,
        `${post.title} | ${siteDetails.siteName}`,
        post.description
    );
}

function formatDate(iso: string) {
    return new Date(iso).toLocaleDateString("en-GB", {
        day: "numeric",
        month: "long",
        year: "numeric",
    });
}

export default async function BlogPostPage({ params }: Props) {
    const { slug } = await params;
    const post = blogPosts.find((p) => p.slug === slug);
    if (!post) notFound();

    const articleJsonLd = JSON.stringify({
        "@context": "https://schema.org",
        "@type": "Article",
        headline: post.title,
        description: post.description,
        datePublished: post.date,
        dateModified: post.date,
        inLanguage: post.lang,
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
            "@id": `${siteDetails.siteUrl}/blog/${post.slug}/`,
        },
    });

    const others = blogPosts.filter((p) => p.slug !== post.slug);

    return (
        <>
            <Header />
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
                        {post.title}
                    </h1>
                    <time
                        dateTime={post.date}
                        className="text-sm text-muted-foreground block mb-8"
                    >
                        {formatDate(post.date)}
                        {post.lang === "rw" ? " · Kinyarwanda" : ""}
                    </time>

                    <div className="text-lg text-foreground-accent leading-relaxed [&_h2]:text-2xl [&_h2]:font-bold [&_h2]:text-foreground [&_h2]:mt-10 [&_h2]:mb-4 [&_h3]:text-xl [&_h3]:font-semibold [&_h3]:text-foreground [&_h3]:mt-6 [&_h3]:mb-3 [&_p]:mb-4 [&_ul]:mb-6 [&_ul]:list-disc [&_ul]:pl-6 [&_ol]:mb-6 [&_ol]:list-decimal [&_ol]:pl-6 [&_li]:mb-2 [&_a]:text-primary [&_a]:underline [&_a:hover]:opacity-80">
                        <post.Content />
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
                                            href={`/blog/${p.slug}/`}
                                            className="text-primary font-semibold hover:underline"
                                        >
                                            {p.title}
                                        </Link>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    )}
                </article>
            </main>
            <Footer />
        </>
    );
}
