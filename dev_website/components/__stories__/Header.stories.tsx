import type { Meta, StoryObj } from "@storybook/react";
import Header from "@/components/Header";
import { withLanguageProvider } from "./decorators";
import React from "react";

/**
 * Header stories for testing:
 * - Language dropdown (verifies text-foreground fix for selected item)
 * - Dark mode toggle
 * - Mobile menu
 * - Scroll state
 */

/* Mock localStorage for dark mode toggle */
const withLocalStorageMock = (theme: string = "light") =>
  ((Story: React.FC) => {
    const originalGetItem = Storage.prototype.getItem;
    const originalSetItem = Storage.prototype.setItem;

    Storage.prototype.getItem = (key: string) => {
      if (key === "theme") return theme;
      if (key === "locale") return "en";
      return originalGetItem.call(localStorage, key);
    };
    Storage.prototype.setItem = (key: string, value: string) => {
      originalSetItem.call(localStorage, key, value);
    };

    return <Story />;
  }) as any;

/* Apply dark class to html element */
const withDarkClass = ((Story: React.FC) => (
  <div className="dark" style={{ minHeight: "100vh" }}>
    <Story />
  </div>
)) as any;

const meta: Meta<typeof Header> = {
  title: "Components/Header",
  component: Header,
  decorators: [withLanguageProvider],
  parameters: {
    layout: "fullscreen",
  },
};

export default meta;
type Story = StoryObj<typeof meta>;

/* ─── Desktop Light Mode ─── */
export const DesktopLight: Story = {
  name: "Desktop — Light Mode",
  decorators: [withLocalStorageMock("light")],
  parameters: {
    viewport: { defaultViewport: "desktop" },
  },
};

/* ─── Desktop Dark Mode ─── */
export const DesktopDark: Story = {
  name: "Desktop — Dark Mode",
  decorators: [withLocalStorageMock("dark"), withDarkClass],
  parameters: {
    viewport: { defaultViewport: "desktop" },
  },
};

/* ─── Desktop with language dropdown open ─── */
export const LanguageDropdownOpen: Story = {
  name: "Desktop — Language Dropdown Open",
  decorators: [
    withLocalStorageMock("light"),
    (Story: React.FC) => {
      /* Auto-open the language dropdown after mount */
      React.useEffect(() => {
        const btn = document.querySelector(
          "[data-testid='lang-toggle']"
        ) as HTMLButtonElement;
        /* Fallback: find the globe button by content */
        if (!btn) {
          const buttons = document.querySelectorAll("button");
          for (const b of buttons) {
            if (b.textContent?.trim() === "EN") {
              b.click();
              break;
            }
          }
        } else {
          btn.click();
        }
      }, []);
      return <Story />;
    },
  ],
  parameters: {
    viewport: { defaultViewport: "desktop" },
  },
};

/* ─── Desktop with French selected ─── */
export const FrenchLanguage: Story = {
  name: "Desktop — French Selected",
  decorators: [
    withLocalStorageMock("light"),
    (Story: React.FC) => {
      React.useEffect(() => {
        const buttons = document.querySelectorAll("button");
        for (const b of buttons) {
          if (b.textContent?.trim() === "FR") {
            b.click();
            break;
          }
        }
      }, []);
      return <Story />;
    },
  ],
  parameters: {
    viewport: { defaultViewport: "desktop" },
  },
};

/* ─── Mobile Light Mode ─── */
export const MobileLight: Story = {
  name: "Mobile — Light Mode",
  decorators: [withLocalStorageMock("light")],
  parameters: {
    viewport: { defaultViewport: "mobile1" },
  },
};

/* ─── Mobile Dark Mode ─── */
export const MobileDark: Story = {
  name: "Mobile — Dark Mode",
  decorators: [withLocalStorageMock("dark"), withDarkClass],
  parameters: {
    viewport: { defaultViewport: "mobile1" },
  },
};

/* ─── Mobile with menu open ─── */
export const MobileMenuOpen: Story = {
  name: "Mobile — Menu Open",
  decorators: [
    withLocalStorageMock("light"),
    (Story: React.FC) => {
      React.useEffect(() => {
        /* Open the hamburger menu */
        const buttons = document.querySelectorAll("button");
        for (const b of buttons) {
          if (b.getAttribute("aria-label")?.includes("Menu")) {
            b.click();
            break;
          }
        }
      }, []);
      return <Story />;
    },
  ],
  parameters: {
    viewport: { defaultViewport: "mobile1" },
  },
};
