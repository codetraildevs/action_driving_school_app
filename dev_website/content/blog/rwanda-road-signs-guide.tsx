import type { BlogPost } from "./types";
import Link from "next/link";

export const rwandaRoadSignsGuide: BlogPost = {
    slug: "rwanda-road-signs-guide",
    date: "2026-09-20",
    keywords: [
        "rwanda road signs",
        "road signs test rwanda",
        "traffic signs rwanda",
    ],
    translations: {
        en: {
            lang: "en",
            title: "Rwanda Road Signs Explained: The Complete Study Guide",
            description:
                "Learn all Rwanda road signs by category — regulatory, warning, and informative — with the memory tricks that help you pass the road signs section of the driving theory exam.",
            Content: () => (
                <>
                    <p>
                        Road signs are one of the most heavily tested areas of
                        the Rwanda driving theory exam — and one of the easiest
                        to score full marks on, because signs follow simple
                        visual rules. Once you understand the shape-and-colour
                        system below, you can often guess correctly even on
                        signs you have never seen before.
                    </p>

                    <h2>The three categories of Rwanda road signs</h2>
                    <h3>1. Regulatory signs — you MUST obey them</h3>
                    <ul>
                        <li>
                            <strong>Circular with red border</strong> = prohibition.
                            No entry, no overtaking, no parking, speed limits.
                        </li>
                        <li>
                            <strong>Circular, blue background</strong> = mandatory.
                            Turn left only, keep right, cyclists&rsquo; path.
                        </li>
                        <li>
                            <strong>Octagon</strong> = stop.{" "}
                            <strong>Triangle pointing down</strong> = give way.
                        </li>
                    </ul>
                    <h3>2. Warning signs — danger ahead</h3>
                    <p>
                        Almost always <strong>triangular with a red border</strong>:
                        curves, junctions ahead, pedestrian crossings, schools,
                        animal crossings, slippery road. The picture inside the
                        triangle tells you the hazard.
                    </p>
                    <h3>3. Informative signs — guidance</h3>
                    <p>
                        Rectangular signs that give information: directions and
                        distances, hospital, fuel station, parking, one-way
                        street. Blue for general information, green for major
                        roads and directions.
                    </p>

                    <h2>Memory tricks that work</h2>
                    <ul>
                        <li><strong>Red always restricts.</strong> Any sign with red limits what you do.</li>
                        <li><strong>Blue informs or instructs.</strong> Blue circles command, blue rectangles inform.</li>
                        <li><strong>Triangles warn.</strong> Something ahead requires caution.</li>
                        <li><strong>Shape beats colour.</strong> The octagon is always STOP and the downward triangle is always GIVE WAY — worldwide.</li>
                    </ul>

                    <h2>Signs that are most often confused in the exam</h2>
                    <ul>
                        <li>
                            <strong>No overtaking vs. end of no overtaking</strong> —
                            the same sign with diagonal grey stripes means the
                            restriction has ended.
                        </li>
                        <li>
                            <strong>No parking vs. no stopping</strong> — a single
                            red slash means no parking; a cross means no
                            stopping at all.
                        </li>
                        <li>
                            <strong>Give way vs. stop</strong> — stop requires a
                            full stop regardless of traffic; give way only
                            requires yielding when traffic is present.
                        </li>
                        <li>
                            <strong>Pedestrian crossing warning vs. crossing
                            sign</strong> — triangles warn you it&rsquo;s ahead; the
                            blue square marks the crossing itself.
                        </li>
                    </ul>

                    <h2>How to practice road signs for the exam</h2>
                    <ol>
                        <li>
                            Study all official Rwanda signs with images in the{" "}
                            <Link href="/features/">
                                road signs section of our app
                            </Link>
                            .
                        </li>
                        <li>Do sign-only quizzes until you score 100% twice in a row.</li>
                        <li>
                            Then mix signs into full{" "}
                            <Link href="/download/">mock tests</Link> — signs
                            feel harder when they arrive mid-test.
                        </li>
                    </ol>
                </>
            ),
        },
        fr: {
            lang: "fr",
            title: "Les panneaux routiers du Rwanda expliqués : guide complet",
            description:
                "Apprenez tous les panneaux routiers du Rwanda par catégorie — prescription, danger et indication — avec les astuces mnémotechniques pour réussir la section des panneaux de l'examen théorique.",
            Content: () => (
                <>
                    <p>
                        Les panneaux routiers sont l&rsquo;une des parties les
                        plus testées de l&rsquo;examen théorique de conduite au
                        Rwanda — et l&rsquo;une des plus faciles à réussir
                        parfaitement, car les panneaux suivent des règles
                        visuelles simples. Une fois le système forme-couleur
                        compris, vous pouvez souvent deviner correctement même
                        face à un panneau jamais vu.
                    </p>

                    <h2>Les trois catégories de panneaux au Rwanda</h2>
                    <h3>1. Panneaux de prescription — à respecter obligatoirement</h3>
                    <ul>
                        <li>
                            <strong>Cercle à liseré rouge</strong> = interdiction.
                            Sens interdit, interdiction de dépasser, de
                            stationner, limitations de vitesse.
                        </li>
                        <li>
                            <strong>Cercle bleu</strong> = obligation. Tourner à
                            gauche uniquement, garder la droite, piste cyclable.
                        </li>
                        <li>
                            <strong>Octogone</strong> = arrêt obligatoire.{" "}
                            <strong>Triangle pointe en bas</strong> = cédez le
                            passage.
                        </li>
                    </ul>
                    <h3>2. Panneaux de danger — prudence</h3>
                    <p>
                        Presque toujours <strong>triangulaires à liseré
                        rouge</strong> : virages, intersections, passages
                        piétons, écoles, traversée d&rsquo;animaux, chaussée
                        glissante. L&rsquo;image dans le triangle indique le
                        danger.
                    </p>
                    <h3>3. Panneaux d'indication — information</h3>
                    <p>
                        Panneaux rectangulaires : directions et distances,
                        hôpital, station-service, parking, sens unique. Bleu
                        pour l&rsquo;information générale, vert pour les grandes
                        routes.
                    </p>

                    <h2>Astuces mémorisation qui marchent</h2>
                    <ul>
                        <li><strong>Le rouge interdit.</strong> Tout panneau avec du rouge limite votre action.</li>
                        <li><strong>Le bleu indique ou ordonne.</strong> Cercle bleu = obligation, rectangle bleu = information.</li>
                        <li><strong>Le triangle avertit.</strong> Un danger exige la prudence plus loin.</li>
                        <li><strong>La forme prime sur la couleur.</strong> L&rsquo;octogone = STOP et le triangle pointe en bas = CÉDEZ LE PASSAGE, partout dans le monde.</li>
                    </ul>

                    <h2>Panneaux les plus confondus à l'examen</h2>
                    <ul>
                        <li>
                            <strong>Interdiction de dépasser vs. fin
                            d'interdiction</strong> — le même panneau avec des
                            bandes grises diagonales annonce la fin de
                            l&rsquo;interdiction.
                        </li>
                        <li>
                            <strong>Stationnement interdit vs. arrêt
                            interdit</strong> — une barre rouge = stationnement
                            interdit ; une croix = arrêt interdit.
                        </li>
                        <li>
                            <strong>Cédez le passage vs. arrêt</strong> — l&rsquo;arrêt
                            impose un arrêt total même sans trafic.
                        </li>
                    </ul>

                    <h2>Comment s'entraîner</h2>
                    <ol>
                        <li>
                            Étudiez tous les panneaux officiels avec images dans{" "}
                            <Link href="/features/">
                                la section panneaux de notre application
                            </Link>
                            .
                        </li>
                        <li>Faites des quiz panneaux jusqu'à deux 100% consécutifs.</li>
                        <li>
                            Mélangez ensuite les panneaux dans des{" "}
                            <Link href="/download/">tests blancs</Link> complets.
                        </li>
                    </ol>
                </>
            ),
        },
        rw: {
            lang: "rw",
            title: "Ibimenyetso by'Umuhanda mu Rwanda: Igitekerezo cyuzuye",
            description:
                "Iga ibimenyetso by'umuhanda mu Rwanda ukoresheje amatsinda yacyo — ibyemeza, ibiburira n'ibimenyesha — hamwe n'uburyo bworoshye bwo kubibuka kugira ngo unyure ikizamini cy'ubushoferi.",
            Content: () => (
                <>
                    <p>
                        Ibimenyetso by&rsquo;umuhanda ni kimwe mu bice
                        bikorwa cyane ku kizamini cy&rsquo;ubushoferi mu
                        Rwanda — kandi ni nacyo kirecyacyoro kuzamurwa
                        amanota yuzuye, kuko ibimenyetso bikurikira amategeko
                        yoroheje y&rsquo;imiterere n&rsquo;ibara. Ulasobanukirwe
                        uburyo bw&rsquo;imiterere n&rsquo;ibara muri iki
                        gitekerezo, ubwo urashobora no kwiba ibisubizo ku
                        bimenyetso utari ubona mbere.
                    </p>

                    <h2>Amatsinda atatu y'ibimenyetso</h2>
                    <h3>1. Ibimenyetso byemeza — ukena kububaha</h3>
                    <ul>
                        <li>
                            <strong>Cercle ifite umupaka umuhondo</strong> = kwamagana.
                            Nta kwinjira, nta kwirenza, nta mahugurwa, umuvuduko
                            ntarengwa.
                        </li>
                        <li>
                            <strong>Cercle ya mu buryo bwa ble</strong> = ibitegeko.
                            Koza ibumoso gusa, komeza iburyo, inzira
                            y&rsquo;abifite ipikipiki.
                        </li>
                        <li>
                            <strong>Octogone</strong> = hagarara.{" "}
                            <strong>Triangle igana hasi</strong> = tanga
                            abandi umwanya.
                        </li>
                    </ul>
                    <h3>2. Ibimenyetso biburira — ibyago imbere</h3>
                    <p>
                        Byose bikomeye <strong>bifite ishusho ya triangle
                        n&rsquo;umupaka umuhondo</strong>: ibyuma bihinduka,
                        mahantu hanini yaza, abanyamuryango banze, amashuri,
                        ibitungwa, inzira idindagarira. Ishusho iri mu triangle
                        igaragaza ikibazo.
                    </p>
                    <h3>3. Ibimenyetso menyesha — kuyobora</h3>
                    <p>
                        Ibimenyetso bifite imiterere ya rectangle: inzira
                        n&rsquo;intera, ibitaro, gasabo (station), mahugurwa
                        y&rsquo;imodoka, inzira imwe. Ble = amakuru y&rsquo;ingenzi;
                        vert = inzira nkuru n&rsquo;indiri.
                    </p>

                    <h2>Uburyo bworoshye bwo kubibuka</h2>
                    <ul>
                        <li><strong>Umuhondo ubuza.</strong> Buri kimenyetso gifite umuhondo kibuza ikintu.</li>
                        <li><strong>Ble itangaza cyangwa iteka.</strong> Cercle ya ble iteka, rectangle ya ble itangaza.</li>
                        <li><strong>Triangle iburira.</strong> Hari ikibazo gisaba ubwitonzi imbere.</li>
                        <li><strong>Imiterere itanga ukuri kurenza ibara.</strong> Octogone ni HAGARARA kandi triangle igana hasi ni TANGA UMWANYA — ku isi yose.</li>
                    </ul>

                    <h2>Ibimenyetso abantu bameka cyane ku kizamini</h2>
                    <ul>
                        <li>
                            <strong>Kwamagana kwirenza vs. iherezo ryacyo</strong> —
                            kimenyetso kimwe gifite amabara ya grise
                            (bandes) agaragaza ko ububasha buheruka.
                        </li>
                        <li>
                            <strong>Nta mahugurwa vs. nta kuhagarara</strong> —
                            umuraba umwe (barre) = nta mahugurwa; umusaraba
                            (croix) = nta kuhagarara na rimwe.
                        </li>
                        <li>
                            <strong>Tanga umwanya vs. hagarara</strong> —
                            kuhagarara bisaba guhagarara burundu, ata
                            kugendagenda; tanga umwanya bigomba gusa
                            ubwo hari ibinyabiziga biri imbere.
                        </li>
                    </ul>

                    <h2>Uko wakora imyitozo</h2>
                    <ol>
                        <li>
                            Soma ibimenyetso byose by&rsquo;u Rwanda
                            bifite amasanamu mu{" "}
                            <Link href="/features/">
                                icece cya app ryihariye ibimenyetso
                            </Link>
                            .
                        </li>
                        <li>Kora ibibazo vy&rsquo;ibimenyetso gusa kugeza ubone 100% inshuro ebyeri.</li>
                        <li>
                            Ubwo vanza ibimenyetso mu{" "}
                            <Link href="/download/">bizamini by&rsquo;igereranya</Link>{" "}
                            byuzuye.
                        </li>
                    </ol>
                </>
            ),
        },
    },
};
