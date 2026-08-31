import { BsBarChartFill, BsFillStarFill } from "react-icons/bs";
import { PiGlobeFill } from "react-icons/pi";

import { IStats } from "@/types";

export const stats: IStats[] = [
    {
        title: "50K+",
        icon: <BsBarChartFill size={34} className="text-blue-500" />,
        description: "Students have used our app to prepare for their driving exams."
    },
    {
        title: "4.8",
        icon: <BsFillStarFill size={34} className="text-yellow-500" />,
        description: "Star rating on Google Play Store from thousands of reviews."
    },
    {
        title: "1000+",
        icon: <PiGlobeFill size={34} className="text-green-600" />,
        description: "Real Rwanda driving exam questions with accurate answers and explanations."
    }
];