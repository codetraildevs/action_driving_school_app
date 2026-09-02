import type { StorybookConfig } from "@storybook/react-vite";
import path from "path";

const config: StorybookConfig = {
  stories: ["../components/**/*.stories.@(ts|tsx)"],
  addons: ["@storybook/addon-essentials", "@storybook/addon-a11y"],
  framework: {
    name: "@storybook/react-vite",
    options: {},
  },
  viteFinal: async (config) => {
    config.resolve = config.resolve || {};
    config.resolve.alias = {
      ...config.resolve.alias,
      "@": path.resolve(__dirname, ".."),
      // Mock next/link since Next.js Router is not available in Storybook
      "next/link": path.resolve(__dirname, "mocks/next-link.tsx"),
    };
    // Override PostCSS config to avoid the string-based plugin format in
    // postcss.config.mjs (Tailwind v4) which Vite/Rolldown doesn't support.
    config.css = {
      ...config.css,
      postcss: {
        plugins: [],
      },
    };
    return config;
  },
};

export default config;
