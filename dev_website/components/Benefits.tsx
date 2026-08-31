import BenefitSection from "./BenefitSection"
import { useLanguage } from "@/lib/LanguageContext"
import { IBenefit } from "@/types"
import { FiCheckCircle, FiClock, FiTarget, FiMap, FiBookOpen, FiTrendingUp, FiGlobe, FiUser, FiShield, FiHelpCircle } from "react-icons/fi"

const iconMap = [
    [<FiCheckCircle size={26} />, <FiClock size={26} />, <FiTarget size={26} />],
    [<FiMap size={26} />, <FiBookOpen size={26} />, <FiTrendingUp size={26} />],
    [<FiGlobe size={26} />, <FiGlobe size={26} />, <FiGlobe size={26} />, <FiUser size={26} />],
    [<FiShield size={26} />, <FiShield size={26} />, <FiHelpCircle size={26} />],
];

const imageSrcs = [
    '/images/Screenshot_2026-01-02_015203-removebg-preview.png',
    '/road-background-pattern.jpg',
    '/images/multilanguagesupport.jpeg',
    '/images/Screenshot_2026-01-02_015333-removebg-preview.png',
];

const Benefits: React.FC = () => {
    const { t } = useLanguage();
    const benefits: IBenefit[] = t.benefits.sections.map((section, index) => ({
        title: section.title,
        description: section.description,
        bullets: section.bullets.map((bullet, bulletIndex) => ({
            title: bullet.title,
            description: bullet.description,
            icon: iconMap[index]?.[bulletIndex] || <FiCheckCircle size={26} />,
        })),
        imageSrc: imageSrcs[index] || imageSrcs[0],
    }));
    return (
        <div id="features">
            <h2 className="sr-only">{t.benefits.sectionTitle}</h2>
            {benefits.map((item, index) => {
                return <BenefitSection key={index} benefit={item} imageAtRight={index % 2 !== 0} />
            })}
        </div>
    )
}

export default Benefits