import type { BlogPost } from "./types";
import Link from "next/link";

const vocabulary: [string, string, string][] = [
    ["amategeko y'umuhanda", "traffic rules / the highway code", "le code de la route"],
    ["ibimenyetso by'umuhanda", "road signs", "les panneaux routiers"],
    ["umuvuduko", "speed", "la vitesse"],
    ["umuvuduko ntarengwa", "speed limit", "la limitation de vitesse"],
    ["impamyabushobozi y'ubushoferi", "driving license", "le permis de conduire"],
    ["ikizamini", "exam / test", "l'examen"],
    ["hagarara", "stop", "s'arrêter (stop)"],
    ["tanga umwanya", "give way / yield", "cédez le passage"],
    ["kwamagana", "prohibited (sign category)", "interdiction"],
    ["kuburira", "warning (sign category)", "avertissement"],
    ["kumenyesha", "informative (sign category)", "indication"],
    ["kwirenza", "to overtake", "dépasser"],
    ["intara", "distance", "la distance"],
    ["ingwate", "helmet", "le casque"],
    ["intanda z'umutekano", "seatbelt", "la ceinture de sécurité"],
    ["ikaraware / rond-point", "roundabout", "le rond-point"],
    ["ibihano", "penalties / fines", "les amendes / sanctions"],
    ["umupolisi w'umuhanda", "traffic police officer", "l'agent de la circulation"],
];

export const drivingVocabularyArticle: BlogPost = {
    slug: "driving-exam-vocabulary-kinyarwanda-english-french",
    date: "2026-09-20",
    keywords: [
        "driving exam vocabulary kinyarwanda",
        "traffic signs kinyarwanda english",
        "amategeko yumuhanda english translation",
    ],
    translations: {
        en: {
            lang: "en",
            title: "Driving Exam Vocabulary: Key Terms in Kinyarwanda, English & French",
            description:
                "The driving terms that appear in the Rwanda theory exam, side by side in Kinyarwanda, English and French — so a language switch on exam day never surprises you.",
            Content: () => (
                <>
                    <p>
                        The Rwanda theory exam and its study materials move
                        between Kinyarwanda, English and French. If you study
                        in one language but read a question in another, small
                        vocabulary gaps can cost you the answer. Here are the
                        terms that appear most often — learn them in all three
                        languages.
                    </p>

                    <h2>Essential driving exam vocabulary</h2>
                    <table>
                        <thead>
                            <tr>
                                <th>Kinyarwanda</th>
                                <th>English</th>
                                <th>Français</th>
                            </tr>
                        </thead>
                        <tbody>
                            {vocabulary.map(([rw, en, fr]) => (
                                <tr key={rw}>
                                    <td>{rw}</td>
                                    <td>{en}</td>
                                    <td>{fr}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>

                    <h2>Why this matters in the exam</h2>
                    <ul>
                        <li>
                            <strong>Sign categories:</strong> the exam asks
                            whether a sign &ldquo;prohibits&rdquo;,
                            &ldquo;warns&rdquo; or &ldquo;informs&rdquo; — know
                            those three verbs in your exam language (and the
                            others too).
                        </li>
                        <li>
                            <strong>Sign text:</strong> regulatory signs in
                            Rwanda use English text (STOP, GIVE WAY) — connect
                            them to their Kinyarwanda equivalents so recognition
                            is instant.
                        </li>
                        <li>
                            <strong>Switching mid-course:</strong> our app
                            lets you{" "}
                            <Link href="/features/">
                                switch language anytime
                            </Link>{" "}
                            — practicing the same question in two languages is
                            one of the fastest memory techniques there is.
                        </li>
                    </ul>

                    <h2>How to memorize the vocabulary fast</h2>
                    <ol>
                        <li>Read the table once per day for a week — five minutes each time.</li>
                        <li>Self-test: cover two columns and recall them from the third.</li>
                        <li>Then do practice questions with the{" "}
                        <Link href="/download/">app set to a different language</Link>{" "}
                        than usual — the terms lock in within days.</li>
                    </ol>

                    <p>
                        Vocabulary is the cheapest exam advantage you can buy
                        yourself: it costs minutes a day and removes an entire
                        category of confusion on exam day.
                    </p>
                </>
            ),
        },
        fr: {
            lang: "fr",
            title: "Vocabulaire de l'examen de conduite : les termes clés en Kinyarwanda, Anglais et Français",
            description:
                "Les termes de conduite qui reviennent à l'examen théorique rwandais, côte à côte en Kinyarwanda, Anglais et Français — pour qu'un changement de langue le jour J ne vous surprenne jamais.",
            Content: () => (
                <>
                    <p>
                        L&rsquo;examen théorique rwandais et ses supports
                        passent du Kinyarwanda à l&rsquo;Anglais et au
                        Français. Si vous révisez dans une langue et lisez la
                        question dans une autre, un petit manque de vocabulaire
                        peut vous coûter la réponse. Voici les termes les plus
                        fréquents — apprenez-les dans les trois langues.
                    </p>

                    <h2>Vocabulaire essentiel de l'examen</h2>
                    <table>
                        <thead>
                            <tr>
                                <th>Kinyarwanda</th>
                                <th>English</th>
                                <th>Français</th>
                            </tr>
                        </thead>
                        <tbody>
                            {vocabulary.map(([rw, en, fr]) => (
                                <tr key={rw}>
                                    <td>{rw}</td>
                                    <td>{en}</td>
                                    <td>{fr}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>

                    <h2>Pourquoi c'est décisif à l'examen</h2>
                    <ul>
                        <li><strong>Catégories de panneaux :</strong> l'examen demande si un panneau « interdit », « avertit » ou « indique » — connaissez ces trois verbes dans votre langue d'examen.</li>
                        <li><strong>Texte des panneaux :</strong> les panneaux de prescription utilisent du texte anglais (STOP, GIVE WAY) — reliez-les à leurs équivalents.</li>
                        <li><strong>Changer de langue en cours de route :</strong> notre application permet de{" "}
                        <Link href="/features/">changer de langue à tout moment</Link>{" "}
                        — travailler la même question en deux langues est une technique de mémorisation redoutable.</li>
                    </ul>

                    <h2>Comment mémoriser vite</h2>
                    <ol>
                        <li>Lisez le tableau une fois par jour pendant une semaine — cinq minutes.</li>
                        <li>Auto-test : couvrez deux colonnes, restituez depuis la troisième.</li>
                        <li>Puis faites des questions avec{" "}
                        <Link href="/download/">l'application dans une autre langue</Link>{" "}
                        que d'habitude — les termes s'ancrent en quelques jours.</li>
                    </ol>

                    <p>
                        Le vocabulaire est l&rsquo;avantage le moins cher de
                        l&rsquo;examen : quelques minutes par jour contre une
                        catégorie entière de confusion en moins le jour J.
                    </p>
                </>
            ),
        },
        rw: {
            lang: "rw",
            title: "Amagambo y'ingenzi y'ikizamini c'ubushoferi: Ikinyarwanda, Icyongereza n'Igifaransa",
            description:
                "Amagambo agaragara cyane ku kizamini cy'ubushoferi mu Rwanda, hamwe mu Kinyarwanda, Icyongereza n'Igifaransa — kugira ngo guhindura ururimi ku munsi w'ikizamini bigutempere.",
            Content: () => (
                <>
                    <p>
                        Ikizamini cy&rsquo;ubushoferi mu Rwanda n&rsquo;ibyo
                        kwigishaho bihindanya hagati y&rsquo;Ikinyarwanda,
                        Icyongereza n&rsquo;Igifaransa. Niba wiga mu rurimi
                        kandi usoma ikibazo mu rurimi rundi, amagambo make
                        yashobora kuguhisha igisubizo. Dore amagambo
                        agaragara cyane — ayigeze mu ndimi zose.
                    </p>

                    <h2>Amagambo y'ingenzi ku kizamini</h2>
                    <table>
                        <thead>
                            <tr>
                                <th>Kinyarwanda</th>
                                <th>English</th>
                                <th>Français</th>
                            </tr>
                        </thead>
                        <tbody>
                            {vocabulary.map(([rw, en, fr]) => (
                                <tr key={rw}>
                                    <td>{rw}</td>
                                    <td>{en}</td>
                                    <td>{fr}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>

                    <h2>Impamvu ibi bijyanye n'ikizamini</h2>
                    <ul>
                        <li><strong>Amatsinda y'ibimenyetso:</strong> ikizamini kibaza niba icyomenyetso "cyamagana", "kiburira" cyangwa "cumenyesha" — yiga aya magatatu mu rurimi rw'ikizamini.</li>
                        <li><strong>Andi mabaraga ku bimenyetso:</strong> ibimenyetso byemeza mu Rwanda bifite amagambo y'icyongereza (STOP, GIVE WAY) — yiyunganishe n'ay'i Kinyarwanda.</li>
                        <li><strong>Guhindura ururimi:</strong> app yacu iguha{" "}
                        <Link href="/features/">amahitamo yo guhindura ururimi igihe cyose</Link>{" "}
                        — kugira ikibazo rimwe mu ndimi zombi ni uburyo bwihuse bwo kwibuka.</li>
                    </ul>

                    <h2>Uburyo bwo kuyibuka vuba</h2>
                    <ol>
                        <li>Soma urutonde rimwe ku munsi mu cyumweru — iminota atanu buri munsi.</li>
                        <li>Igerageze: kwibuka amagambo muri kolone imwe arebye izindi.</li>
                        <li>Hanyuma ukore ibibazo na{" "}
                        <Link href="/download/">app mu rurimi rutandukanye</Link>{" "}
                        n'uko ukoresha — amagambo ashira mu mutwe mu minsi mike.</li>
                    </ol>

                    <p>
                        Amagambo ni akazi gato gakubiyeho akamaro kanini:
                        iminota mike ku munsi, kandi bigukuraho ikibazo
                        cyose cy'ururimi ku munsi w'ikizamini.
                    </p>
                </>
            ),
        },
    },
};
