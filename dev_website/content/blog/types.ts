import type { FC } from "react";

export interface BlogPost {
    slug: string;
    title: string;
    description: string;
    /** ISO date (YYYY-MM-DD) */
    date: string;
    lang: "en" | "fr" | "rw";
    keywords: string[];
    Content: FC;
}
