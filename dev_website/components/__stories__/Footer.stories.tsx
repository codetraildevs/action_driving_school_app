import type { Meta, StoryObj } from "@storybook/react";
import Footer from "@/components/Footer";
import { withLanguageProvider } from "./decorators";

const meta: Meta<typeof Footer> = {
  title: "Components/Footer",
  component: Footer,
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

export const NarrowViewport: Story = {
  name: "Mobile (320px)",
  parameters: {
    viewport: { defaultViewport: "mobile1" },
  },
};
