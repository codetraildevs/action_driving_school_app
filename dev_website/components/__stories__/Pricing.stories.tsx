import type { Meta, StoryObj } from "@storybook/react";
import PricingColumn from "@/components/Pricing/PricingColumn";
import { withLanguageProvider } from "./decorators";
import { IPricing } from "@/types";

const meta: Meta<typeof PricingColumn> = {
  title: "Components/Pricing",
  component: PricingColumn,
  decorators: [withLanguageProvider],
};

export default meta;
type Story = StoryObj<typeof meta>;

const basicTier: IPricing = {
  name: "Basic",
  price: 5000,
  currency: "RWF",
  features: [
    "Practice quizzes",
    "Road signs library",
    "Basic progress tracking",
  ],
};

const premiumTier: IPricing = {
  name: "Premium",
  price: 10000,
  currency: "RWF",
  features: [
    "Everything in Basic",
    "Unlimited practice exams",
    "Detailed analytics",
    "Priority support",
    "Offline mode",
  ],
};

export const BasicPlan: Story = {
  name: "Basic (not highlighted)",
  args: {
    tier: basicTier,
    highlight: false,
  },
};

export const PremiumPlan: Story = {
  name: "Premium (highlighted)",
  args: {
    tier: premiumTier,
    highlight: true,
  },
};

export const SideBySide: Story = {
  name: "Side by Side",
  render: () => (
    <div className="flex gap-6 items-start">
      <PricingColumn tier={basicTier} highlight={false} />
      <PricingColumn tier={premiumTier} highlight={true} />
    </div>
  ),
};

export const DarkMode: Story = {
  name: "Dark Mode",
  args: {
    tier: premiumTier,
    highlight: true,
  },
  decorators: [
    withLanguageProvider,
    (Story) => (
      <div className="dark">
        <Story />
      </div>
    ),
  ],
};
