"use client"
import { Disclosure, DisclosureButton, DisclosurePanel } from "@headlessui/react";
import { BiMinus, BiPlus } from "react-icons/bi";
import SectionTitle from "./SectionTitle";
import { useLanguage } from "@/lib/LanguageContext";

const FAQ: React.FC = () => {
    const { t } = useLanguage();
    return (
        <section id="faq" className="py-16 md:py-24 px-6">
            <div className="max-w-7xl mx-auto flex flex-col lg:flex-row gap-12 lg:gap-20">
                <div className="lg:w-1/3">
                    <p className="hidden lg:block text-sm font-semibold text-foreground-accent uppercase tracking-wider mb-4">{t.faq.label}</p>
                    <SectionTitle>
                        <h2 className="my-3 !leading-snug lg:max-w-sm text-center lg:text-left">{t.faq.title}</h2>
                    </SectionTitle>
                    <p className="lg:mt-8 text-foreground-accent text-center lg:text-left">
                        {t.faq.askUs}
                    </p>
                    <a href="mailto:info@amategekoyumuhanda.rw" className="mt-4 block text-xl lg:text-3xl text-foreground-accent hover:text-foreground font-semibold hover:underline text-center lg:text-left">
                        {t.faq.emailLabel}
                    </a>
                </div>

                <div className="lg:w-2/3 lg:max-w-2xl">
                    <div className="border-t border-border">
                        {t.faq.questions.map((faq, index) => (
                            <div key={index} className="mb-0">
                                <Disclosure>
                                    {({ open }) => (
                                        <>
                                            <DisclosureButton className="flex items-center justify-between w-full px-4 py-6 text-lg text-left border-b border-border">
                                                <span className="text-xl font-semibold pr-4">{faq.question}</span>
                                                {open
                                                    ? <BiMinus className="w-5 h-5 text-muted-foreground flex-shrink-0" />
                                                    : <BiPlus className="w-5 h-5 text-muted-foreground flex-shrink-0" />
                                                }
                                            </DisclosureButton>
                                            <DisclosurePanel className="px-4 pt-2 pb-6 text-foreground-accent leading-relaxed">
                                                {faq.answer}
                                            </DisclosurePanel>
                                        </>
                                    )}
                                </Disclosure>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </section>
    );
};

export default FAQ;
