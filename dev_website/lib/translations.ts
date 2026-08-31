export type Locale = 'en' | 'fr' | 'rw';

export interface Translations {
    // Navigation
    nav: {
        features: string;
        howItWorks: string;
        pricing: string;
        download: string;
        getApp: string;
        toggleDarkMode: string;
        toggleMenu: string;
    };

    // Hero
    hero: {
        heading: string;
        subheading: string;
    };

    // Stats
    stats: {
        studentsCount: string;
        studentsDesc: string;
        ratingCount: string;
        ratingDesc: string;
        questionsCount: string;
        questionsDesc: string;
    };

    // Benefits
    benefits: {
        sectionTitle: string;
        sections: {
            title: string;
            description: string;
            bullets: {
                title: string;
                description: string;
            }[];
        }[];
    };

    // FAQ
    faq: {
        label: string;
        title: string;
        askUs: string;
        emailLabel: string;
        questions: {
            question: string;
            answer: string;
        }[];
    };

    // CTA
    cta: {
        heading: string;
        subheading: string;
    };

    // Footer
    footer: {
        subheading: string;
        quickLinks: string;
        contactUs: string;
        copyright: string;
        madeBy: string;
    };

    // Download page
    download: {
        title: string;
        subtitle: string;
        features: {
            title: string;
            description: string;
        }[];
    };

    // Features page
    features: {
        title: string;
        subtitle: string;
    };

    // How It Works page
    howItWorks: {
        title: string;
        subtitle: string;
        steps: {
            title: string;
            description: string;
        }[];
        whyStudentsLove: string;
        whyFeatures: {
            title: string;
            description: string;
        }[];
    };

    // Testimonials
    testimonials: {
        title: string;
        subtitle: string;
    };

    // Screenshots
    screenshots: {
        title: string;
        subtitle: string;
    };

    // Pricing
    pricing: {
        title: string;
        subtitle: string;
        perMonth: string;
        getStarted: string;
        features: string;
        everythingInBasic: string;
    };
}

const en: Translations = {
    nav: {
        features: 'Features',
        howItWorks: 'How It Works',
        pricing: 'Pricing',
        download: 'Download',
        getApp: 'Get App',
        toggleDarkMode: 'Toggle dark mode',
        toggleMenu: 'Toggle menu',
    },

    hero: {
        heading: 'Learn to Drive Safely in Rwanda',
        subheading: 'Master the Rwanda driving theory exam with real practice questions, road signs, and mock tests. Available in Kinyarwanda, English, and French.',
    },

    stats: {
        studentsCount: '50K+',
        studentsDesc: 'Students have used our app to prepare for their driving exams.',
        ratingCount: '4.8',
        ratingDesc: 'Star rating on Google Play Store from thousands of reviews.',
        questionsCount: '1000+',
        questionsDesc: 'Real Rwanda driving exam questions with accurate answers and explanations.',
    },

    benefits: {
        sectionTitle: 'Features',
        sections: [
            {
                title: 'Real Exam Questions',
                description: 'Practice with actual Rwanda driving theory exam questions to build confidence and knowledge before your test day.',
                bullets: [
                    { title: 'Updated Question Bank', description: 'Questions are regularly updated to match the current Rwanda driving exam format.' },
                    { title: 'Mock Tests', description: 'Simulate the real exam experience with timed mock tests that match the actual test format.' },
                    { title: 'Instant Results', description: 'Get immediate feedback on your answers with detailed explanations for each question.' },
                ],
            },
            {
                title: 'Road Signs & Traffic Laws',
                description: 'Learn and master all Rwanda road signs, traffic rules, and road safety regulations with visual guides.',
                bullets: [
                    { title: 'Visual Road Signs', description: 'Study all official Rwanda road signs with clear images and descriptions.' },
                    { title: 'Traffic Laws', description: 'Comprehensive coverage of all Rwanda traffic laws and regulations.' },
                    { title: 'Progress Tracking', description: 'Track your learning progress and identify areas that need more practice.' },
                ],
            },
            {
                title: 'Multilingual Support',
                description: 'Practice in the language you are most comfortable with. The app supports Kinyarwanda, English, and French.',
                bullets: [
                    { title: 'Kinyarwanda Language', description: 'Full app experience in Kinyarwanda for local users.' },
                    { title: 'English Language', description: 'Complete English translation for international users.' },
                    { title: 'French Language', description: 'Full French support for Francophone users.' },
                    { title: 'Easy Switching', description: 'Switch between languages anytime from the app settings.' },
                ],
            },
            {
                title: 'Safe Learning Environment',
                description: 'Build your driving knowledge safely from anywhere. No internet required after downloading questions.',
                bullets: [
                    { title: 'Offline Mode', description: 'Practice without internet connection after the initial download.' },
                    { title: 'Safe Data', description: 'Your progress is securely saved and synced across devices.' },
                    { title: 'Expert Content', description: 'All content is reviewed by driving education professionals in Rwanda.' },
                ],
            },
        ],
    },

    faq: {
        label: "FAQ'S",
        title: 'Frequently Asked Questions',
        askUs: 'Ask us anything!',
        emailLabel: 'info@amategekoyumuhanda.rw',
        questions: [
            {
                question: 'How do I use Action Driving School App to prepare for my driving exam?',
                answer: 'Simply download the app from Google Play Store, create an account, and start practicing with real Rwanda driving theory questions. You can take mock tests, review road signs, and track your progress.',
            },
            {
                question: 'Is Action Driving School App available in Kinyarwanda?',
                answer: 'Yes! The app supports both Kinyarwanda and English, so you can practice in the language you are most comfortable with.',
            },
            {
                question: 'Are the practice questions the same as the real exam?',
                answer: 'Yes! Action Driving School App uses real Rwanda driving theory exam questions from the Rwanda National Police. Our question bank is regularly updated to match the current exam format.',
            },
            {
                question: 'Is the app free to use?',
                answer: 'The app offers free practice questions and limited mock tests. For full access to all questions, mock tests, and advanced features, check out our premium subscription plans.',
            },
            {
                question: 'What if I need help using the app?',
                answer: 'Our dedicated support team is available to help you. You can reach us via email or phone. Plus, the app includes helpful tutorials to guide you through all features.',
            },
        ],
    },

    cta: {
        heading: 'Download Action Driving School App Today',
        subheading: 'Start preparing for your Rwanda driving theory exam with real practice questions and mock tests. Available on Google Play Store.',
    },

    footer: {
        subheading: 'Preparing Rwandan drivers for success with real exam practice questions and road safety training.',
        quickLinks: 'Quick Links',
        contactUs: 'Contact Us',
        copyright: 'All rights reserved.',
        madeBy: 'Made by',
    },

    download: {
        title: 'Download Action Driving School',
        subtitle: 'Start preparing for your Rwanda driving theory exam today. Practice with real questions, road signs, and mock tests.',
        features: [
            { title: 'Real Exam Questions', description: 'Practice with actual Rwanda driving theory exam questions updated regularly.' },
            { title: 'Mock Tests', description: 'Simulate the real exam experience with timed practice tests.' },
            { title: 'Bilingual Support', description: 'Available in both Kinyarwanda and English for your convenience.' },
        ],
    },

    features: {
        title: 'Features',
        subtitle: 'Everything you need to pass your Rwanda driving theory exam. Our app provides comprehensive preparation tools.',
    },

    howItWorks: {
        title: 'How It Works',
        subtitle: 'Get started in 5 simple steps and prepare for your driving theory exam with confidence.',
        steps: [
            { title: 'Download the App', description: 'Get Action Driving School App from your Google Play Store in seconds.' },
            { title: 'Create Your Account', description: 'Sign up with your name and phone number to get started.' },
            { title: 'Start Learning', description: 'Choose your learning path and begin mastering road laws at your pace.' },
            { title: 'Practice & Test', description: 'Complete quizzes, track progress, and measure your understanding.' },
            { title: 'Drive Safely', description: 'Apply your knowledge and become a responsible, law-abiding driver.' },
        ],
        whyStudentsLove: 'Why Students Love Our App',
        whyFeatures: [
            { title: 'Works Offline', description: 'Download questions once and practice anywhere, even without internet.' },
            { title: 'Always Updated', description: 'Our question bank is regularly updated to match the latest exam format.' },
            { title: 'Track Progress', description: 'See your improvement over time with detailed progress tracking.' },
            { title: 'Bilingual', description: 'Study in Kinyarwanda or English — switch languages anytime.' },
        ],
    },

    testimonials: {
        title: 'What Our Students Say',
        subtitle: 'Join thousands of successful drivers who passed their exam with our app.',
    },

    screenshots: {
        title: 'App Screenshots',
        subtitle: 'Take a look at the app interface and features that will help you prepare for your driving exam.',
    },

    pricing: {
        title: 'Choose Your Plan',
        subtitle: 'Start for free or upgrade to Premium for full access to all features.',
        perMonth: 'month',
        getStarted: 'Get Started',
        features: 'FEATURES',
        everythingInBasic: 'Everything in basic, plus...',
    },
};

const fr: Translations = {
    nav: {
        features: 'Fonctionnalités',
        howItWorks: 'Comment ça marche',
        pricing: 'Tarifs',
        download: 'Télécharger',
        getApp: "Obtenir l'app",
        toggleDarkMode: 'Basculer le mode sombre',
        toggleMenu: 'Basculer le menu',
    },

    hero: {
        heading: 'Apprenez à conduire en toute sécurité au Rwanda',
        subheading: "Maîtrisez l'examen théorique de conduite au Rwanda avec des questions pratiques réelles, des panneaux routiers et des tests blancs. Disponible en Kinyarwanda, Anglais et Français.",
    },

    stats: {
        studentsCount: '50K+',
        studentsDesc: "Étudiants ont utilisé notre application pour se préparer à leurs examens de conduite.",
        ratingCount: '4.8',
        ratingDesc: "Note étoilée sur le Google Play Store basée sur des milliers d'avis.",
        questionsCount: '1000+',
        questionsDesc: "Vraies questions d'examen de conduite rwandais avec des réponses et explications précises.",
    },

    benefits: {
        sectionTitle: 'Fonctionnalités',
        sections: [
            {
                title: "Vraies questions d'examen",
                description: "Entraînez-vous avec de vraies questions de l'examen théorique de conduite rwandais pour gagner en confiance et en connaissances avant votre jour d'examen.",
                bullets: [
                    { title: 'Banque de questions mise à jour', description: 'Les questions sont régulièrement mises à jour pour correspondre au format actuel du examen de conduite rwandais.' },
                    { title: 'Tests blancs', description: 'Simulez la vraie expérience de lexamen avec des tests chronométrés qui correspondent au format réel du test.' },
                    { title: 'Résultats instantanés', description: 'Obtenez un retour immédiat sur vos réponses avec des explications détaillées pour chaque question.' },
                ],
            },
            {
                title: 'Panneaux routiers et Code de la route',
                description: 'Apprenez et maîtrisez tous les panneaux routiers du Rwanda, les règles de la circulation et les réglementations de sécurité routière avec des guides visuels.',
                bullets: [
                    { title: 'Panneaux routiers visuels', description: 'Étudiez tous les panneaux routiers officiels du Rwanda avec des images et des descriptions claires.' },
                    { title: 'Code de la route', description: 'Couverture complète de toutes les lois et réglementations de la circulation au Rwanda.' },
                    { title: 'Suivi de progression', description: 'Suivez votre progression d\'apprentissage et identifiez les domaines qui nécessitent plus de pratique.' },
                ],
            },
            {
                title: 'Support multilingue',
                description: "Entraînez-vous dans la langue avec laquelle vous êtes le plus à l'aise. L'application prend en charge le Kinyarwanda, l'Anglais et le Français.",
                bullets: [
                    { title: 'Langue Kinyarwanda', description: "Expérience complète de l'application en Kinyarwanda pour les utilisateurs locaux." },
                    { title: 'Langue Anglaise', description: "Traduction complète en Anglais pour les utilisateurs internationaux." },
                    { title: 'Langue Française', description: 'Support complet en Français pour les utilisateurs francophones.' },
                    { title: 'Changement facile', description: "Changez de langue à tout moment depuis les paramètres de l'application." },
                ],
            },
            {
                title: "Environnement d'apprentissage sûr",
                description: 'Développez vos connaissances en conduite en toute sécurité, où que vous soyez. Aucune connexion Internet requise après le téléchargement des questions.',
                bullets: [
                    { title: 'Mode hors ligne', description: "Entraînez-vous sans connexion Internet après le téléchargement initial." },
                    { title: 'Données sécurisées', description: 'Votre progression est sauvegardée de manière sécurisée et synchronisée entre les appareils.' },
                    { title: 'Contenu expert', description: 'Tout le contenu est examiné par des professionnels de léducation routière au Rwanda.' },
                ],
            },
        ],
    },

    faq: {
        label: 'FAQ',
        title: 'Foire aux questions',
        askUs: 'Demandez-nous!',
        emailLabel: 'info@amategekoyumuhanda.rw',
        questions: [
            {
                question: "Comment utiliser l'application Action Driving School pour me préparer à mon examen de conduite?",
                answer: "Téléchargez simplement l'application depuis le Google Play Store, créez un compte et commencez à vous entraîner avec de vraies questions de lexamen théorique de conduite rwandais. Vous pouvez passer des tests blancs, réviser les panneaux routiers et suivre votre progression.",
            },
            {
                question: "L'application Action Driving School est-elle disponible en Kinyarwanda?",
                answer: "Oui! L'application prend en charge le Kinyarwanda et l'Anglais, vous pouvez donc vous entraîner dans la langue avec laquelle vous êtes le plus à l'aise.",
            },
            {
                question: 'Les questions pratiques sont-elles les mêmes que le vrai examen?',
                answer: "Oui! L'application Action Driving School utilise de vraies questions de lexamen théorique de conduite rwandais de la Police Nationale Rwandaise. Notre banque de questions est régulièrement mise à jour pour correspondre au format actuel du examen.",
            },
            {
                question: "L'application est-elle gratuite?",
                answer: "L'application offre des questions pratiques gratuites et des tests blancs limités. Pour un accès complet à toutes les questions, tests blancs et fonctionnalités avancées, consultez nos abonnements premium.",
            },
            {
                question: "Et si j'ai besoin d'aide pour utiliser l'application?",
                answer: "Notre équipe de support dédiée est disponible pour vous aider. Vous pouvez nous contacter par e-mail ou par téléphone. De plus, l'application comprend des tutoriels utiles pour vous guider à travers toutes les fonctionnalités.",
            },
        ],
    },

    cta: {
        heading: "Téléchargez l'application Action Driving School aujourd'hui",
        subheading: "Commencez à vous préparer à votre examen théorique de conduite au Rwanda avec de vraies questions pratiques et des tests blancs. Disponible sur le Google Play Store.",
    },

    footer: {
        subheading: "Préparer les conducteurs rwandais à la réussite avec de vraies questions d'examen pratiques et une formation à la sécurité routière.",
        quickLinks: 'Liens rapides',
        contactUs: 'Contactez-nous',
        copyright: 'Tous droits réservés.',
        madeBy: 'Fait par',
    },

    download: {
        title: "Téléchargez l'application Action Driving School",
        subtitle: "Commencez à vous préparer à votre examen théorique de conduite au Rwanda dès aujourd'hui. Entraînez-vous avec de vraies questions, des panneaux routiers et des tests blancs.",
        features: [
            { title: "Vraies questions d'examen", description: "Entraînez-vous avec de vraies questions de l'examen théorique de conduite rwandais régulièrement mises à jour." },
            { title: 'Tests blancs', description: "Simulez la vraie expérience de lexamen avec des tests pratiques chronométrés." },
            { title: 'Support bilingue', description: 'Disponible en Kinyarwanda et en Anglais pour votre commodité.' },
        ],
    },

    features: {
        title: 'Fonctionnalités',
        subtitle: "Tout ce dont vous avez besoin pour réussir votre examen théorique de conduite au Rwanda. Notre application fournit des outils de préparation complets.",
    },

    howItWorks: {
        title: 'Comment ça marche',
        subtitle: 'Commencez en 5 étapes simples et préparez-vous à votre examen théorique de conduite en toute confiance.',
        steps: [
            { title: "Téléchargez l'application", description: "Obtenez l'application Action Driving School depuis votre Google Play Store en quelques secondes." },
            { title: 'Créez votre compte', description: 'Inscrivez-vous avec votre nom et numéro de téléphone pour commencer.' },
            { title: 'Commencez à apprendre', description: "Choisissez votre parcours d'apprentissage et commencez à maîtriser les règles de la route à votre rythme." },
            { title: 'Entraînez-vous et testez-vous', description: "Complétez des quiz, suivez vos progrès et mesurez votre compréhension." },
            { title: 'Conduisez en toute sécurité', description: 'Appliquez vos connaissances et devenez un conducteur responsable et respectueux des règles.' },
        ],
        whyStudentsLove: 'Pourquoi les étudiants adorent notre application',
        whyFeatures: [
            { title: 'Fonctionne hors ligne', description: "Téléchargez les questions une fois et entraînez-vous n'importe où, même sans Internet." },
            { title: 'Toujours à jour', description: "Notre banque de questions est régulièrement mise à jour pour correspondre au dernier format dexamen." },
            { title: 'Suivez vos progrès', description: 'Voyez votre amélioration au fil du temps avec un suivi de progression détaillé.' },
            { title: 'Bilingue', description: 'Étudiez en Kinyarwanda ou en Anglais — changez de langue à tout moment.' },
        ],
    },

    testimonials: {
        title: 'Ce que disent nos étudiants',
        subtitle: 'Rejoignez des milliers de conducteurs qui ont réussi leur examen avec notre application.',
    },

    screenshots: {
        title: 'Captures d\'écran',
        subtitle: 'Découvrez l\'interface et les fonctionnalités de l\'application qui vous aideront à préparer votre examen de conduite.',
    },

    pricing: {
        title: 'Choisissez votre forfait',
        subtitle: 'Commencez gratuitement ou passez à Premium pour un accès complet à toutes les fonctionnalités.',
        perMonth: 'mois',
        getStarted: 'Commencer',
        features: 'FONCTIONNALITÉS',
        everythingInBasic: 'Tout est inclus, plus...',
    },
};

const rw: Translations = {
    nav: {
        features: 'Ibintu',
        howItWorks: 'Uko bigenda',
        pricing: 'Igiciro',
        download: 'Kurura',
        getApp: 'Shakisha App',
        toggleDarkMode: 'Hindura uburiri bw\'umwijima',
        toggleMenu: 'Hindura ibikubiyemo',
    },

    hero: {
        heading: 'Iga Amategeko y\'umuhanda utsinde neza',
        subheading: 'Menya neza ikizamini cy\'ubushoferi mu Rwanda ukoresheje ibibazo vy\'ukuri, ibimenyetso by\'umuhanda n\'ibizamini by\'igereranya. Ibikorwa mu Kinyarwanda, Icyongereza n\'Igifaransa.',
    },

    stats: {
        studentsCount: '50K+',
        studentsDesc: 'Abanyeshuli bakoresha app yacu kugira ngo bakore ikizamini cy\'ubushoferi.',
        ratingCount: '4.8',
        ratingDesc: 'Igiteramero kuri Google Play Store kuva mu birenga ibihumbi vy\'ibisubizo.',
        questionsCount: '1000+',
        questionsDesc: 'Ibibazo vy\'ukuri vy\'ikizamini c\'ubushoferi mu Rwanda hamwe n\'ibisubizo n\'ibisobanuro.',
    },

    benefits: {
        sectionTitle: 'Ibintu',
        sections: [
            {
                title: 'Ibibazo vy\'ukuri vy\'ikizamini',
                description: 'Jyana imyitozo hamwe n\'ibibazo vy\'ukuri vy\'ikizamini c\'ubushoferi mu Rwanda kugira ngo wizere kandi umeze neza mbere y\'umunsi w\'ikizamini.',
                bullets: [
                    { title: 'Ibibazo bisubijwe', description: 'Ibibazo bisubijwa buri gihe kugira biringanire n\'uburyo bw\'ikizamini c\'ubushoferi c\'ubu mu Rwanda.' },
                    { title: 'Ibizamini by\'igereranya', description: 'Gereranya uburambe bw\'ikizamini ry\'ukuri ukoresheje ibizamini bisabwa bifata igihe bihuriye n\'uburyo bw\'ikizamini ry\'ukuri.' },
                    { title: 'Ibisubizo vyo aho', description: 'Shakisha igitegererezo ku bisubizo byawe amasezerano y\'ibanze kuri buri kibazo.' },
                ],
            },
            {
                title: 'Ibimenyetso by\'umuhanda n\'Amategeko y\'abaruku_umuhanda',
                description: 'Menya kandi ukize ibimenyetso vyose by\'umuhanda mu Rwanda, amategeko y\'ingendo n\'amategeko y\'ubuhumane bw\'umuhanda ukoresheje icyerekezo.',
                bullets: [
                    { title: 'Ibimenyetso vy\'umuhanda', description: 'Soma ibimenyetso vyose vy\'umuhanda vy\'ubucuruzi bw\'u Rwanda hamwe n\'amasanamu y\'agaciro n\'ibisobanuro.' },
                    { title: 'Amategeko y\'ingendo', description: 'Kuburikiranya vyose amategeko y\'ingendo n\'amategeko y\'u Rwanda.' },
                    { title: 'Gukurikirana inkoragero', description: 'Kurikirana iterambere ryawe mu kwiga kandi umenye ibibanza bishyizweho kubw\'uko birenze imyitozo.' },
                ],
            },
            {
                title: 'Ubufasha bw\'ururimi rurenga',
                description: 'Jyana imyitozo mu rurimi rwo kubaha neza. App iriho ifasha mu Kinyarwanda, Icyongereza n\'Igifaransa.',
                bullets: [
                    { title: 'Ururimi rwa Kinyarwanda', description: 'Ibikorwa vyose by\'app mu Kinyarwanda ku bakoresha bo mu Rwanda.' },
                    { title: 'Ururimi rw\'Icyongereza', description: 'Ibisobanuro vyose mu Kinyarwanda ku bakoresha bo hanze y\'u Rwanda.' },
                    { title: 'Ururimi rw\'Igifaransa', description: 'Ubufasha bwose mugifaransa ku bakoresha bavuga Ifaransa.' },
                    { title: 'Guhindura mu buryo bworoshye', description: 'Hindura ururimi ugomba mu gihe cyose ushyira mu bikorwa vy\'app.' },
                ],
            },
            {
                title: 'Ahantu heza ho kwiga',
                description: 'Jya umenya neza ubushobozi bwo gutwara imodoka ahantu hose mu buzima. Nta internet ikenewe nyuma yo kurura ibibazo.',
                bullets: [
                    { title: 'Uburyo bwo mu buryo', description: 'Jyana imyitozo nta internet nyuma yo kurura ibintu bwambere.' },
                    { title: 'Amakuru y\'ubwoko bwiza', description: 'Ingerero yawe yubatse neza kandi yegeranijwe ku bwoko bw\'intebe zose.' },
                    { title: 'Ibikubiyemo vy\'abatanga ubuyobozi', description: 'Ibikubiyemo vyose birasuzumwa n\'abatanga ubuyobozi bw\'ubushobozi mu Rwanda.' },
                ],
            },
        ],
    },

    faq: {
        label: 'IBIBAZO',
        title: 'Ibibazo bishyizweho ibibaho',
        askUs: 'Tuza tubaze!',
        emailLabel: 'info@amategekoyumuhanda.rw',
        questions: [
            {
                question: 'Nakoresha app Action Driving School gute kugira ngo njye nitegure ikizamini cy\'ubushoferi?',
                answer: 'Kurura app mu Google Play Store, ubanza ureme konte, hanyuma utangire gufata imyitozo hamwe n\'ibibazo vy\'ukuri vy\'ubushoferi mu Rwanda. Urashobora gufata ibizamini by\'igereranya, kureba ibimenyetso by\'umuhanda, no gukurikirana iterambere ryawe.',
            },
            {
                question: 'App Action Driving School iriho mu Kinyarwanda?',
                answer: 'Yego! App iriho mu Kinyarwanda no mu Kinyarwanda, bityo urashobora gufata imyitozo mu rurimi ukeneye neza.',
            },
            {
                question: 'Ibibazo vy\'imyitozo n\'ibyo vy\'ikizamini ry\'ukuri?',
                answer: 'Yego! App Action Driving School ikoresha ibibazo vy\'ukuri vy\'ikizamini c\'ubushoferi mu Rwanda uva mu Polisi y\'Ubumwe bw\'u Rwanda. Ibibazo vyacu bisubijwa buri gihe kugira biri ngombwa n\'uburyo bw\'ikizamini c\'ubu.',
            },
            {
                question: 'App yo gukoresha ni y\'ubuntu?',
                answer: 'App itanga ibibazo vy\'imyitozo vy\'ubuntu n\'ibizamini by\'igereranya. Kugira ngo uje ku buryo bwose ku bibazo, ibizamini n\'ibintu vy\'inyongera, reba amasezerano yacu y\'agaciro.',
            },
            {
                question: 'Niba nkeneye ubufasha mu gukoresha app?',
                answer: 'Umukoresha wacu ufasha ariho kugira ngo akugireho ubufasha. Urashobora kubandana natwe kuri email cyangwa telefone. Byongeye, app iriho amabitso y\'amahugurwa yo kukugobora mu bikorwa vyose.',
            },
        ],
    },

    cta: {
        heading: 'Kurura app Action Driving School Uyu munsi',
        subheading: 'Tangira gitegura ikizamini cy\'ubushoferi mu Rwanda ukoresheje ibibazo vy\'ukuri n\'ibizamini by\'igereranya. Iriho kuri Google Play Store.',
    },

    footer: {
        subheading: 'Gutegeka abatwara imodoka mu Rwanda kugira ngo bishimire ukoresheje ibibazo vy\'ukuri vy\'ikizamini n\'amahugurwa y\'ubuhumane bw\'umuhanda.',
        quickLinks: 'Amahuriro y\'ako kanya',
        contactUs: 'Tuvugishe',
        copyright: 'Uburenganzira bwose burabitswe.',
        madeBy: 'Byakoreshwa na',
    },

    download: {
        title: 'Kurura app Action Driving School',
        subtitle: 'Tangira gitegura ikizamini cy\'ubushoferi mu Rwanda uyu munsi. Jyana imyitozo hamwe n\'ibibazo vy\'ukuri, ibimenyetso by\'umuhanda n\'ibizamini by\'igereranya.',
        features: [
            { title: 'Ibibazo vy\'ukuri vy\'ikizamini', description: 'Jyana imyitozo hamwe n\'ibibazo vy\'ukuri vy\'ikizamini c\'ubushoferi mu Rwanda bisubijwe buri gihe.' },
            { title: 'Ibizamini by\'igereranya', description: 'Gereranya uburambe bw\'ikizamini ry\'ukuri ukoresheje ibizamini by\'imyitozo bisabwa bifata igihe.' },
            { title: 'Ubufasha bw\'ururimi bubiri', description: 'Iriho mu Kinyarwanda no mu Kinyarwanda kugira ngo bikwerure.' },
        ],
    },

    features: {
        title: 'Ibintu',
        subtitle: 'Ibintu vyose ukeneye kugira ngo upashe ikizamini c\'ubushoferi mu Rwanda. App yacu itanga ibikoresho vy\'itegurezo vyuzuye.',
    },

    howItWorks: {
        title: 'Uko bigenda',
        subtitle: 'Tangira mu buryo bworoshye bw\'impera 5 kandi uitegure ikizamini c\'ubushoferi wizere neza.',
        steps: [
            { title: 'Kurura app', description: 'Shakisha app Action Driving School mu Google Play Store mu isaha ndende.' },
            { title: 'Kurema konte yawe', description: 'Wandikisha izina ryawe n\'nomero ya telefone yawe kugira ngo utangire.' },
            { title: 'Tangira kwiga', description: 'Hitamwo inzira yawe yo kwiga kandi utangire kumenya amategeko y\'umuhanda ku bwihutire bwawe.' },
            { title: 'Jyana imyitozo wizere', description: 'Uzuza ibibazo, kurikirana iterambere, no kugereranya ubusobanisi bwawe.' },
            { title: 'Utware mu buzima', description: 'Koresha ubumenyi bwawe kandi ube umutwara w\'imodoka ufise ububasha kandi uburinzi.' },
        ],
        whyStudentsLove: 'Kubera iki abanyeshuli bakunda app yacu',
        whyFeatures: [
            { title: 'Iraho mu buryo', description: 'Kurura ibibazo rimwe kandi ukore imyitozo aho hose, nta internet.' },
            { title: 'Buri gihe bisubijwe', description: 'Ibibazo vyacu bisubijwa buri gihe kugira biringanire n\'uburyo bw\'ikizamini bw\'imbere.' },
            { title: 'Kurikirana iterambere', description: 'Raba iterambere ryawe mu gihe rikomeza ukoresheje uburyo bw\'iterambere bw\'amakuru.' },
            { title: 'Ubuvangabuzima', description: 'Soma mu Kinyarwanda cyangwa mu Kinyarwanda — hindura ururimi uhitamwo mu gihe cyose.' },
        ],
    },

    testimonials: {
        title: 'Ico Abanyeshuli Batubwira',
        subtitle: 'Joina abanyeshuli ibihumbi batashye ikizamini cabo ukoresheje app yacu.',
    },

    screenshots: {
        title: 'Ibisanzwe by\'App',
        subtitle: 'Raba uburyo bw\'app n\'ibintu igiye kukugufasha gutegura ikizamini cy\'ubushoferi.',
    },

    pricing: {
        title: 'Hitamwo Igiciro',
        subtitle: 'Tangira ubusa cyangwa ushyize ku Premium kugira ngo ufungure ibintu vyose.',
        perMonth: 'ukwezi',
        getStarted: 'Tangira',
        features: 'IBINTU',
        everythingInBasic: 'Ibintu vyose, plus...',
    },
};

export const translations: Record<Locale, Translations> = {
    en,
    fr,
    rw,
};
