import type { Meta, StoryObj } from "@storybook/react";
import Stats from "@/components/Stats";
import { withLanguageProvider } from "./decorators";

const meta: Meta<typeof Stats> = {
  title: "Components/Stats",
  component: Stats,
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
