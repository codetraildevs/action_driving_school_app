import { IFAQ } from "@/types";
import { siteDetails } from "./siteDetails";

export const faqs: IFAQ[] = [
    {
        question: `How do I use ${siteDetails.siteName} to prepare for my driving exam?`,
        answer: 'Simply download the app from Google Play Store, create an account, and start practicing with real Rwanda driving theory questions. You can take mock tests, review road signs, and track your progress.',
    },
    {
        question: `Is ${siteDetails.siteName} available in Kinyarwanda?`,
        answer: 'Yes! The app supports both Kinyarwanda and English, so you can practice in the language you are most comfortable with.',
    },
    {
        question: 'Are the practice questions the same as the real exam?',
        answer: `Yes! ${siteDetails.siteName} uses real Rwanda driving theory exam questions from the Rwanda National Police. Our question bank is regularly updated to match the current exam format.`
    },
    {
        question: 'Is the app free to use?',
        answer: 'The app offers free practice questions and limited mock tests. For full access to all questions, mock tests, and advanced features, check out our premium subscription plans.',
    },
    {
        question: 'What if I need help using the app?',
        answer: 'Our dedicated support team is available to help you. You can reach us via email or phone. Plus, the app includes helpful tutorials to guide you through all features.'
    }
];