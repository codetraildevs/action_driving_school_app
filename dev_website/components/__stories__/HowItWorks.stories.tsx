import type { Meta, StoryObj } from "@storybook/react";
import HowItWorksContent from "@/components/pages/HowItWorksContent";
import { withLanguageProvider } from "./decorators";

const meta: Meta<typeof HowItWorksContent> = {
  title: "Components/HowItWorks",
  component: HowItWorksContent,
  decorators: [withLanguageProvider],
  parameters: {
    layout: "fullscreen",
  },
};

export default meta;
type Story = StoryObj<typeof meta>;

export const LightMode: Story = {
  name: "Light Mode",
};

export const DarkMode: Story = {
  name: "Dark Mode",
  decorators: [
    withLanguageProvider,
    (Story) => (
      <div className="dark">
        <Story />
      </div>
    ),
  ],
};
