import React from "react";
import { Decorator } from "@storybook/react";
import { LanguageProvider } from "@/lib/LanguageContext";

/**
 * Wraps a Storybook story with the LanguageProvider so that
 * components using useLanguage() render with English translations.
 */
export const withLanguageProvider: Decorator = (Story) => (
  <LanguageProvider>
    <Story />
  </LanguageProvider>
);
