import type { FC } from "react";
import type { Locale } from "@/lib/translations";

export type BlogLang = Locale; // 'en' | 'fr' | 'rw'

export const BLOG_LANGS: BlogLang[] = ["en", "fr", "rw"];

export const LANG_PATH: Record<BlogLang, string> = {
    en: "", // English is the default: /blog/{slug}/
    fr: "fr", // /blog/{slug}/fr/
    rw: "rw", // /blog/{slug}/rw/
};

export const LANG_LABEL: Record<BlogLang, string> = {
    en: "English",
    fr: "Français",
    rw: "Kinyarwanda",
};

export interface BlogPostVariant {
    lang: BlogLang;
    title: string;
    description: string;
    Content: FC;
}

export interface BlogPost {
    /** Canonical (English) slug: /blog/{slug}/ */
    slug: string;
    /** ISO date (YYYY-MM-DD) */
    date: string;
    keywords: string[];
    translations: Record<BlogLang, BlogPostVariant>;
}
