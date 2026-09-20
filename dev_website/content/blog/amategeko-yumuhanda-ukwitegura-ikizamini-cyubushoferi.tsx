import type { BlogPost } from "./types";
import Link from "next/link";

export const amategekoYumuhandaIkizamini: BlogPost = {
    slug: "amategeko-yumuhanda-ukwitegura-ikizamini-cyubushoferi",
    title: "Amategeko y'Umuhanda mu Rwanda: Uko Witegura Ikizamini cy'Ubushoferi",
    description:
        "Ibyo ukena kumenya ku bipimo n'amategeko y'imodoka mu Rwanda, ibimenyetso by'umuhanda, n'uko witegura neza ikizamini cy'ubushoferi ukoresheje ibibazo nyacyo.",
    date: "2026-09-20",
    lang: "rw",
    keywords: [
        "amategeko y'umuhanda mu rwanda",
        "ikizamini cy'ubushoferi",
        "impamyabushobozi y'ubushoferi rwanda",
        "ibimenyetso by'umuhanda",
    ],
    Content: () => (
        <>
            <p>
                Mbere yo koherezwa mu kizamini cy&rsquo;imodoka (practical), buri
                wese ukena kwipasa ikizamini cy&rsquo;amategeko y&rsquo;umuhanda.
                Abantu benshi bapfa iki kizamini kubera ko batiteguye neza,
                atari ukubera ko bigoye. Muri iyi ngingo tuzakureba ibyo ukena
                kumenya n&rsquo;uko witegura neza kugira ngo unyure ku nshuro ya
                mbere.
            </p>

            <h2>Ibikubiyemo ikizamini</h2>
            <ul>
                <li>
                    <strong>Amategeko y&rsquo;umuhanda:</strong> ibipimo cy&rsquo;umuvuduko,
                    uko abantu basa ahantu hanini (carrefour), roundabout,
                    kwirenza mu muhanda, n&rsquo;ibihano bihawe uyarengeza
                    amategeko.
                </li>
                <li>
                    <strong>Ibimenyetso by&rsquo;umuhanda:</strong> ibimenyetso
                    byemeza (regulatory), ibimenyetso(cb) kwabura ibyago
                    (warning), n&rsquo;ibimenyetso igamije kumenyesha (informative).
                </li>
                <li>
                    <strong>Ubutwari bw&rsquo;imodoka (conduite sécurisée):</strong>
                    intera igomba kuba hagati y&rsquo;imodoka zombi, uko utwara
                    imvura cyangwa mu mwijima, n&rsquo;uko utwara aho hari
                    abanyamuryango (piyons) n&rsquo;imodoka nto (motari).
                </li>
            </ul>

            <h2>Uko witegura neza</h2>
            <ol>
                <li>
                    <strong>Soma amategeko y&rsquo;umuhanda.</strong> Tangira usome
                    igitabo cya Polisi y&rsquo;u Rwanda kireba amategeko
                    y&rsquo;umuhanda, kugira ngo ibibazo ubonye mu myitozo
                    ubisobanukirwe.
                </li>
                <li>
                    <strong>Kora ibibazo nyacyo bya litegeko.</strong>{" "}
                    <Link href="/download/">
                        App ya Action Driving School
                    </Link>{" "}
                    irimo ibibazo bindi irenga 1,000 bya nyabyo bisa mu
                    kizamini cya Polisi, hamwe n&rsquo;ibisobanuro kuri buri
                    kibazo.
                </li>
                <li>
                    <strong>Kora ibizamini by&rsquo;igereranya (mock tests).</strong>{" "}
                    Ibi biguteka kwiha igihe nk&rsquo;uko biri ku munsi
                    w&rsquo;ikizamini nyacyo. Kora ibirenga 5 kugera ku 10 mbere
                    y&rsquo;umunsi wawe.
                </li>
                <li>
                    <strong>Reba ibyo wakomye.</strong> Kuri buri kibazo
                    cyakubangiye, soma icyumviguro. Ni ho wizera ukomoka.
                </li>
            </ol>

            <h2>Ibimenyetso by'umuhanda: uburyo bworoshye bwo kubimenya</h2>
            <ul>
                <li>
                    Icyirwa (cercle) gifite umupaka umuhondo = <strong>kwamagana</strong>{" "}
                    (nta kwinjira, nta kwirenza, umuvuduko ntarengwa).
                </li>
                <li>
                    Icyirwa kirabura (nya-burayiki) = <strong>ibitegeko</strong>{" "}
                    (koza iburyo, hagarara aho bijyanye).
                </li>
                <li>
                    Inyanditashu (triangle) ifite umupaka umuhondo ={" "}
                    <strong>kuburira</strong> — hari ikintu cyateye imbere
                    gisaba ubwitonzi.
                </li>
                <li>
                    Umwanya mweru (rectangle) = <strong>kumenyesha</strong> —
                    inzira, akarere k&rsquo;ibitaro, parking.
                </li>
            </ul>

            <h2>Igikorwa cya nyuma</h2>
            <p>
                Abanyure ku nshuro ya mbere ni abajiye imyitozo myinshi
                ukoresheje ibibazo nyacyo.{" "}
                <Link href="/download/">
                    Kurura app ya Action Driving School ubonaswe
                </Link>{" "}
                kuri Google Play, hitamo ururimi rwawe (Kinyarwanda, Icyongereza
                cyangwa Igifaransa), kandi utangire uyu munsi.{" "}
                <Link href="/how-it-works/">
                    Reba uko app ikora
                </Link>{" "}
                niba uri kwitangira.
            </p>
        </>
    ),
};
