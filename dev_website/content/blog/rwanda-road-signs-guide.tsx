import type { BlogPost } from "./types";
import Link from "next/link";

export const rwandaRoadSignsGuide: BlogPost = {
    slug: "rwanda-road-signs-guide",
    title: "Rwanda Road Signs Explained: The Complete Study Guide",
    description:
        "Learn all Rwanda road signs by category — regulatory, warning, and informative — with the memory tricks that help you pass the road signs section of the driving theory exam.",
    date: "2026-09-20",
    lang: "en",
    keywords: [
        "rwanda road signs",
        "road signs test rwanda",
        "traffic signs rwanda",
        "ibimenyetso by'umuhanda rwanda",
    ],
    Content: () => (
        <>
            <p>
                Road signs are one of the most heavily tested areas of the
                Rwanda driving theory exam — and one of the easiest to score
                full marks on, because signs follow simple visual rules. Once
                you understand the shape-and-colour system below, you can often
                guess correctly even on signs you have never seen before.
            </p>

            <h2>The three categories of Rwanda road signs</h2>
            <h3>1. Regulatory signs — you MUST obey them</h3>
            <p>
                These tell you what you must or must not do. Breaking them is a
                traffic offence.
            </p>
            <ul>
                <li>
                    <strong>Circular with red border</strong> = prohibition. No
                    entry, no overtaking, no parking, speed limits.
                </li>
                <li>
                    <strong>Circular, blue background</strong> = mandatory.
                    Turn left only, keep right, cyclists&rsquo; path.
                </li>
                <li>
                    <strong>Octagon</strong> = stop. <strong>Triangle pointing down</strong> = give way.
                </li>
            </ul>
            <h3>2. Warning signs — danger ahead</h3>
            <p>
                Almost always <strong>triangular with a red border</strong>:
                curves, junctions ahead, pedestrian crossings, schools, animal
                crossings, slippery road. The picture inside the triangle tells
                you the hazard.
            </p>
            <h3>3. Informative signs — guidance</h3>
            <p>
                Rectangular signs that give information: directions and
                distances, hospital, fuel station, parking, one-way street.
                Blue for general information, green for major roads and
                directions.
            </p>

            <h2>Memory tricks that work</h2>
            <ul>
                <li>
                    <strong>Red always restricts.</strong> Any sign with red
                    limits what you do — stop, give way, don&rsquo;t enter, don&rsquo;t
                    park, reduce speed.
                </li>
                <li>
                    <strong>Blue informs or instructs.</strong> Blue circles
                    command, blue rectangles inform.
                </li>
                <li>
                    <strong>Triangles warn.</strong> If it&rsquo;s triangular, something
                    ahead requires caution.
                </li>
                <li>
                    <strong>Shape beats colour.</strong> The octagon is always
                    STOP and the downward triangle is always GIVE WAY —
                    worldwide.
                </li>
            </ul>

            <h2>Signs that are most often confused in the exam</h2>
            <ul>
                <li>
                    <strong>No overtaking vs. end of no overtaking</strong> —
                    the same sign with diagonal grey stripes means the
                    restriction has ended.
                </li>
                <li>
                    <strong>No parking vs. no stopping</strong> — a single red
                    slash means no parking; a cross (two slashes) means no
                    stopping at all.
                </li>
                <li>
                    <strong>Give way vs. stop</strong> — stop requires a full
                    stop regardless of traffic; give way only requires yielding
                    when traffic is present.
                </li>
                <li>
                    <strong>Pedestrian crossing warning vs. crossing regulatory
                    sign</strong> — triangles warn you it&rsquo;s ahead; the blue
                    square marks the crossing itself.
                </li>
            </ul>

            <h2>How to practice road signs for the exam</h2>
            <p>
                Reading a list once is not enough — the exam shows you signs
                among similar-looking options. You need visual repetition:
            </p>
            <ol>
                <li>
                    Study all official Rwanda signs with images and
                    descriptions in the{" "}
                    <Link href="/features/">road signs section of our app</Link>.
                </li>
                <li>Do sign-only quizzes until you score 100% twice in a row.</li>
                <li>
                    Then mix signs into full{" "}
                    <Link href="/download/">mock tests</Link> — signs feel
                    harder when they arrive mid-test.
                </li>
            </ol>

            <p>
                Master the shape-and-colour system, drill the confusable pairs
                above, and the road signs section becomes free marks on your
                exam.
            </p>
        </>
    ),
};
