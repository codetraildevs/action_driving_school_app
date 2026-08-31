"use client"
import clsx from "clsx";
import { motion, Variants } from "framer-motion"

import BenefitBullet from "./BenefitBullet";
import SectionTitle from "./SectionTitle";
import { IBenefit } from "@/types";

interface Props {
    benefit: IBenefit;
    imageAtRight?: boolean;
}

const containerVariants: Variants = {
    offscreen: {
        opacity: 0,
        y: 100
    },
    onscreen: {
        opacity: 1,
        y: 0,
        transition: {
            type: "spring",
            bounce: 0.2,
            duration: 0.9,
            delayChildren: 0.2,
            staggerChildren: 0.1,
        }
    }
};

export const childVariants = {
    offscreen: {
        opacity: 0,
        x: -50,
    },
    onscreen: {
        opacity: 1,
        x: 0,
        transition: {
            type: "spring",
            bounce: 0.2,
            duration: 1,
        }
    },
};

const BenefitSection: React.FC<Props> = ({ benefit, imageAtRight }: Props) => {
    const { title, description, imageSrc, bullets } = benefit;

    return (
        <section className="benefit-section py-12 md:py-20 px-6">
            <div className="max-w-7xl mx-auto">
                <motion.div
                    className="flex flex-wrap flex-col items-center justify-center gap-8 lg:flex-row lg:gap-20 lg:flex-nowrap"
                    variants={containerVariants}
                    initial="offscreen"
                    whileInView="onscreen"
                    viewport={{ once: true }}
                >
                    <div
                        className={clsx("flex flex-wrap items-center w-full max-w-lg", { "justify-start": imageAtRight, "lg:order-1 justify-end": !imageAtRight })}
                    >
                        <div className="w-full text-center lg:text-left">
                            <motion.div className="flex flex-col w-full" variants={childVariants}>
                                <SectionTitle>
                                    <h3 className="lg:max-w-2xl">
                                        {title}
                                    </h3>
                                </SectionTitle>
                                <p className="mt-3 mx-auto lg:ml-0 leading-relaxed text-foreground-accent">
                                    {description}
                                </p>
                            </motion.div>
                            <div className="mx-auto lg:ml-0 w-full mt-6">
                                {bullets.map((item, index) => (
                                    <BenefitBullet key={index} title={item.title} icon={item.icon} description={item.description} />
                                ))}
                            </div>
                        </div>
                    </div>

                    <div className={clsx("mt-8 lg:mt-0", { "lg:order-2": imageAtRight })}>
                        <div className={clsx("w-fit flex", { "justify-start": imageAtRight, "justify-end": !imageAtRight })}>
                            <img src={imageSrc} alt={title} className="lg:ml-0 max-w-[280px] w-full h-auto object-cover rounded-2xl" />
                        </div>
                    </div>
                </motion.div>
            </div>
        </section>
    );
}

export default BenefitSection
