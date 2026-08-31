import { FiBarChart2, FiBookOpen, FiCheckCircle, FiClock, FiGlobe, FiHelpCircle, FiMap, FiShield, FiTarget, FiTrendingUp, FiUser } from "react-icons/fi";

import { IBenefit } from "@/types"

export const benefits: IBenefit[] = [
    {
        title: "Real Exam Questions",
        description: "Practice with actual Rwanda driving theory exam questions to build confidence and knowledge before your test day.",
        bullets: [
            {
                title: "Updated Question Bank",
                description: "Questions are regularly updated to match the current Rwanda driving exam format.",
                icon: <FiCheckCircle size={26} />
            },
            {
                title: "Mock Tests",
                description: "Simulate the real exam experience with timed mock tests that match the actual test format.",
                icon: <FiClock size={26} />
            },
            {
                title: "Instant Results",
                description: "Get immediate feedback on your answers with detailed explanations for each question.",
                icon: <FiTarget size={26} />
            }
        ],
        imageSrc: "/images/Screenshot_2026-01-02_015203-removebg-preview.png"
    },
    {
        title: "Road Signs & Traffic Laws",
        description: "Learn and master all Rwanda road signs, traffic rules, and road safety regulations with visual guides.",
        bullets: [
            {
                title: "Visual Road Signs",
                description: "Study all official Rwanda road signs with clear images and descriptions.",
                icon: <FiMap size={26} />
            },
            {
                title: "Traffic Laws",
                description: "Comprehensive coverage of all Rwanda traffic laws and regulations.",
                icon: <FiBookOpen size={26} />
            },
            {
                title: "Progress Tracking",
                description: "Track your learning progress and identify areas that need more practice.",
                icon: <FiTrendingUp size={26} />
            }
        ],
        imageSrc: "/road-background-pattern.jpg"
    },
    {
        title: "Multilingual Support",
        description: "Practice in the language you are most comfortable with. The app supports Kinyarwanda, English, and French.",
        bullets: [
            {
                title: "Kinyarwanda Language",
                description: "Full app experience in Kinyarwanda for local users.",
                icon: <FiGlobe size={26} />
            },
            {
                title: "English Language",
                description: "Complete English translation for international users.",
                icon: <FiGlobe size={26} />
            },
            {
                title: "French Language",
                description: "Full French support for Francophone users.",
                icon: <FiGlobe size={26} />
            },
            {
                title: "Easy Switching",
                description: "Switch between languages anytime from the app settings.",
                icon: <FiUser size={26} />
            }
        ],
        imageSrc: "/images/multilanguagesupport.jpeg"
    },
    {
        title: "Safe Learning Environment",
        description: "Build your driving knowledge safely from anywhere. No internet required after downloading questions.",
        bullets: [
            {
                title: "Offline Mode",
                description: "Practice without internet connection after the initial download.",
                icon: <FiShield size={26} />
            },
            {
                title: "Safe Data",
                description: "Your progress is securely saved and synced across devices.",
                icon: <FiShield size={26} />
            },
            {
                title: "Expert Content",
                description: "All content is reviewed by driving education professionals in Rwanda.",
                icon: <FiHelpCircle size={26} />
            }
        ],
        imageSrc: "/images/Screenshot_2026-01-02_015333-removebg-preview.png"
    },
]
