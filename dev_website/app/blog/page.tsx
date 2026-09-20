import type { Metadata } from "next";
import Link from "next/link";
import Header from "@/components/Header";
import Footer from "@/components/Footer";
import { blogPosts } from "@/content/blog";
import { pageMetadata } from "@/lib/pageMetadata";
import { siteDetails } from "@/data/siteDetails";

export const metadata: Metadata = pageMetadata(
    '/blog/',
    "Blog & Study Guides | Rwanda Driving Exam Tips - Action Driving School App",
    "Free study guides and articles for the Rwanda driving theory exam: traffic laws, road signs, mock tests and tips to pass your driving license exam on the first try."
);

function formatDate(iso: string) {
    return new Date(iso).toLocaleDateString("en-GB", {
        day: "numeric",
        month: "long",
        year: "numeric",
    });
}

export default function BlogPage() {
    return (
        <>
            <Header />
            <main className="pt-24 md:pt-32 pb-16 md:pb-24 px-6 min-h-screen">
                <div className="max-w-5xl mx-auto">
                    <p className="text-sm font-semibold text-foreground-accent uppercase tracking-wider mb-2">
                        {siteDetails.siteName}
                    </p>
                    <h1 className="text-4xl md:text-5xl font-bold text-foreground mb-4">
                        Blog &amp; Study Guides
                    </h1>
                    <p className="text-xl text-foreground-accent mb-12 max-w-2xl leading-relaxed">
                        Everything you need to pass the Rwanda driving theory
                        exam — traffic laws, road signs, and proven preparation
                        strategies.
                    </p>

                    <div className="grid gap-6">
                        {blogPosts.map((post) => (
                            <article
                                key={post.slug}
                                className="border border-border rounded-2xl p-6 md:p-8 hover:shadow-lg transition-shadow"
                            >
                                <time
                                    dateTime={post.date}
                                    className="text-sm text-muted-foreground"
                                >
                                    {formatDate(post.date)}
                                    {post.lang === "rw" ? " · Kinyarwanda" : ""}
                                </time>
                                <h2 className="text-2xl md:text-3xl font-bold mt-2 mb-3">
                                    <Link
                                        href={`/blog/${post.slug}/`}
                                        className="hover:text-primary transition-colors"
                                    >
                                        {post.title}
                                    </Link>
                                </h2>
                                <p className="text-foreground-accent leading-relaxed mb-4">
                                    {post.description}
                                </p>
                                <Link
                                    href={`/blog/${post.slug}/`}
                                    className="text-primary font-semibold hover:underline"
                                >
                                    Read the guide →
                                </Link>
                            </article>
                        ))}
                    </div>
                </div>
            </main>
            <Footer />
        </>
    );
}
