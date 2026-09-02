import type { Preview } from "@storybook/react";

const preview: Preview = {
  globalTypes: {
    theme: {
      description: "Global theme",
      toolbar: {
        title: "Theme",
        icon: "circlehollow",
        items: [
          { value: "light", title: "Light Mode" },
          { value: "dark", title: "Dark Mode" },
        ],
        dynamicTitle: true,
      },
    },
  },
  initialGlobals: {
    theme: "light",
  },
  decorators: [
    (Story, context) => {
      const theme = context.globals.theme || "light";
      return (
        <div className={theme === "dark" ? "dark" : ""}>
          <div
            style={{
              padding: "2rem",
              background: theme === "dark" ? "#1a1a1a" : "#ffffff",
              minHeight: "100vh",
            }}
          >
            <Story />
          </div>
        </div>
      );
    },
  ],
  parameters: {
    layout: "centered",
  },
};

export default preview;
