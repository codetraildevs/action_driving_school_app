import type { Meta, StoryObj } from "@storybook/react";
import React from "react";

/**
 * Contrast Accessibility Tests
 *
 * These stories verify that text and icon colors meet WCAG AA contrast
 * requirements (4.5:1 for normal text, 3:1 for large text and icons)
 * against both light and dark backgrounds.
 *
 * The root cause of the original bug: `text-primary` (#bbe9f2) was used
 * as a text color on light backgrounds, making it nearly invisible.
 * This story catches that pattern by testing all foreground color tokens
 * against all background color tokens.
 */

/* ─── Color tokens from globals.css ─── */
const COLORS = {
  light: {
    background: "#ffffff", // --background: oklch(1 0 0)
    foreground: "#1a1a1a", // --foreground: oklch(0.145 0 0)
    "foreground-accent": "#6b6b6b", // --foreground-accent: oklch(0.556 0 0)
    primary: "#bbe9f2", // --primary
    "muted-foreground": "#6b6b6b", // --muted-foreground: oklch(0.556 0 0)
  },
  dark: {
    background: "#1a1a1a", // --background: oklch(0.145 0 0)
    foreground: "#fafafa", // --foreground: oklch(0.985 0 0)
    "foreground-accent": "#b3b3b3", // --foreground-accent: oklch(0.708 0 0)
    primary: "#bbe9f2", // --primary (same in both themes)
    "muted-foreground": "#b3b3b3", // --muted-foreground: oklch(0.708 0 0)
  },
} as const;

/* ─── Helper: approximate relative luminance (sRGB) ─── */
function hexToRgb(hex: string) {
  const h = hex.replace("#", "");
  return {
    r: parseInt(h.substring(0, 2), 16) / 255,
    g: parseInt(h.substring(2, 4), 16) / 255,
    b: parseInt(h.substring(4, 6), 16) / 255,
  };
}

function luminance(r: number, g: number, b: number) {
  const toLinear = (c: number) =>
    c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
  return 0.2126 * toLinear(r) + 0.7152 * toLinear(g) + 0.0722 * toLinear(b);
}

function contrastRatio(fg: string, bg: string) {
  const fgL = luminance(...Object.values(hexToRgb(fg)));
  const bgL = luminance(...Object.values(hexToRgb(bg)));
  const lighter = Math.max(fgL, bgL);
  const darker = Math.min(fgL, bgL);
  return (lighter + 0.05) / (darker + 0.05);
}

function meetsWCAG(fg: string, bg: string, isLargeText = false) {
  const ratio = contrastRatio(fg, bg);
  return isLargeText ? ratio >= 3 : ratio >= 4.5;
}

/* ─── Demo component that shows a color swatch with label ─── */
interface SwatchProps {
  fg: string;
  bg: string;
  fgName: string;
  bgName: string;
  isLargeText?: boolean;
}

const ColorSwatch: React.FC<SwatchProps> = ({
  fg,
  bg,
  fgName,
  bgName,
  isLargeText = false,
}) => {
  const ratio = contrastRatio(fg, bg);
  const passes = meetsWCAG(fg, bg, isLargeText);
  const fontSize = isLargeText ? "text-2xl" : "text-sm";

  return (
    <div
      style={{ backgroundColor: bg, padding: "1rem", borderRadius: "0.5rem" }}
      className="border border-gray-200 dark:border-gray-700"
    >
      <div
        style={{ color: fg }}
        className={`${fontSize} font-semibold mb-1`}
      >
        Sample text Aa
      </div>
      <div className="text-xs" style={{ color: "#888" }}>
        <strong>
          {fgName} on {bgName}
        </strong>
        <br />
        Ratio: {ratio.toFixed(2)}:1
        <br />
        <span style={{ color: passes ? "#16a34a" : "#dc2626" }}>
          {passes ? "✓ PASSES" : "✗ FAILS"} WCAG AA{" "}
          {isLargeText ? "(large)" : "(normal)"}
        </span>
      </div>
    </div>
  );
};

/* ─── Story: Light Mode Tests ─── */
const LightModeContrast: React.FC = () => {
  const bg = COLORS.light.background;
  const fgEntries = Object.entries(COLORS.light) as [string, string][];

  return (
    <div>
      <h2 className="text-xl font-bold mb-4">
        Light Mode — Foreground colors on white background
      </h2>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {fgEntries.map(([name, color]) => (
          <ColorSwatch
            key={name}
            fg={color}
            bg={bg}
            fgName={name}
            bgName="background"
          />
        ))}
      </div>
      <h2 className="text-xl font-bold mt-8 mb-4">
        Light Mode — Large text (≥18pt bold) on white
      </h2>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {fgEntries.map(([name, color]) => (
          <ColorSwatch
            key={`lg-${name}`}
            fg={color}
            bg={bg}
            fgName={name}
            bgName="background"
            isLargeText
          />
        ))}
      </div>
    </div>
  );
};

/* ─── Story: Dark Mode Tests ─── */
const DarkModeContrast: React.FC = () => {
  const bg = COLORS.dark.background;
  const fgEntries = Object.entries(COLORS.dark) as [string, string][];

  return (
    <div>
      <h2 className="text-xl font-bold mb-4">
        Dark Mode — Foreground colors on dark background
      </h2>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {fgEntries.map(([name, color]) => (
          <ColorSwatch
            key={name}
            fg={color}
            bg={bg}
            fgName={name}
            bgName="background"
          />
        ))}
      </div>
      <h2 className="text-xl font-bold mt-8 mb-4">
        Dark Mode — Large text (≥18pt bold) on dark
      </h2>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {fgEntries.map(([name, color]) => (
          <ColorSwatch
            key={`lg-${name}`}
            fg={color}
            bg={bg}
            fgName={name}
            bgName="background"
            isLargeText
          />
        ))}
      </div>
    </div>
  );
};

/* ─── Story: Hero Background Tests ─── */
const HeroBackgroundContrast: React.FC = () => {
  const heroBg = "#e9eeff"; // rgb(233, 238, 255)
  const fgEntries = [
    ["foreground", COLORS.light.foreground],
    ["foreground-accent", COLORS.light["foreground-accent"]],
    ["primary", COLORS.light.primary],
    ["muted-foreground", COLORS.light["muted-foreground"]],
  ] as [string, string][];

  return (
    <div>
      <h2 className="text-xl font-bold mb-4">
        Hero Background (#e9eeff) — Common use case (Footer)
      </h2>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {fgEntries.map(([name, color]) => (
          <ColorSwatch
            key={name}
            fg={color}
            bg={heroBg}
            fgName={name}
            bgName="hero-background"
          />
        ))}
      </div>
    </div>
  );
};

/* ─── Story: Card Background Tests ─── */
const CardBackgroundContrast: React.FC = () => {
  const cardBg = "#ffffff"; // --card in light mode
  const fgEntries = [
    ["foreground", COLORS.light.foreground],
    ["foreground-accent", COLORS.light["foreground-accent"]],
    ["primary", COLORS.light.primary],
    ["muted-foreground", COLORS.light["muted-foreground"]],
  ] as [string, string][];

  return (
    <div>
      <h2 className="text-xl font-bold mb-4">
        Card Background (white) — Pricing cards
      </h2>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {fgEntries.map(([name, color]) => (
          <ColorSwatch
            key={name}
            fg={color}
            bg={cardBg}
            fgName={name}
            bgName="card"
          />
        ))}
      </div>
    </div>
  );
};

/* ─── Story: Specific Bug Pattern Regression ─── */
const RegressionTests: React.FC = () => {
  const testCases = [
    {
      name: "Footer credit link (was text-primary on hero-bg)",
      fg: COLORS.light.primary,
      bg: "#e9eeff",
      shouldPass: false,
    },
    {
      name: "Footer credit link AFTER FIX (text-foreground-accent)",
      fg: COLORS.light["foreground-accent"],
      bg: "#e9eeff",
      shouldPass: true,
    },
    {
      name: "FAQ email link (was text-primary on white)",
      fg: COLORS.light.primary,
      bg: COLORS.light.background,
      shouldPass: false,
    },
    {
      name: "FAQ email link AFTER FIX (text-foreground-accent)",
      fg: COLORS.light["foreground-accent"],
      bg: COLORS.light.background,
      shouldPass: true,
    },
    {
      name: "Pricing price (was text-primary on card)",
      fg: COLORS.light.primary,
      bg: COLORS.light.background,
      shouldPass: false,
    },
    {
      name: "Pricing price AFTER FIX (text-foreground)",
      fg: COLORS.light.foreground,
      bg: COLORS.light.background,
      shouldPass: true,
    },
    {
      name: "Stats icon (was text-primary on white)",
      fg: COLORS.light.primary,
      bg: COLORS.light.background,
      shouldPass: false,
    },
    {
      name: "Stats icon AFTER FIX (text-foreground-accent)",
      fg: COLORS.light["foreground-accent"],
      bg: COLORS.light.background,
      shouldPass: true,
    },
  ];

  return (
    <div>
      <h2 className="text-xl font-bold mb-4">
        Regression Tests — Bug pattern: text-primary on light backgrounds
      </h2>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {testCases.map((tc) => {
          const ratio = contrastRatio(tc.fg, tc.bg);
          const passes = ratio >= 4.5;
          const expectedPass = tc.shouldPass;
          const regressionDetected =
            (expectedPass && !passes) || (!expectedPass && passes);

          return (
            <div
              key={tc.name}
              style={{
                backgroundColor: tc.bg,
                padding: "1rem",
                borderRadius: "0.5rem",
                borderLeft: `4px solid ${
                  regressionDetected
                    ? "#dc2626"
                    : expectedPass
                    ? "#16a34a"
                    : "#f59e0b"
                }`,
              }}
            >
              <div style={{ color: tc.fg }} className="text-sm font-semibold">
                Sample text — {tc.name}
              </div>
              <div className="text-xs mt-1" style={{ color: "#888" }}>
                Ratio: {ratio.toFixed(2)}:1
                <br />
                Expected: {expectedPass ? "PASS" : "FAIL"}
                {regressionDetected && (
                  <span className="text-red-600 font-bold">
                    {" "}
                    ← REGRESSION DETECTED
                  </span>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

/* ─── Meta & Exports ─── */
const meta: Meta<typeof ColorSwatch> = {
  title: "Accessibility/ColorContrast",
  parameters: {
    layout: "padded",
    a11y: {
      config: {
        rules: [
          {
            id: "color-contrast",
            enabled: true,
          },
        ],
      },
    },
  },
};

export default meta;

type Story = StoryObj<typeof meta>;

export const LightMode: Story = {
  render: () => <LightModeContrast />,
};

export const DarkMode: Story = {
  render: () => <DarkModeContrast />,
  parameters: {
    backgrounds: { default: "dark" },
  },
};

export const HeroBackground: Story = {
  render: () => <HeroBackgroundContrast />,
};

export const CardBackground: Story = {
  render: () => <CardBackgroundContrast />,
};

export const RegressionSuite: Story = {
  render: () => <RegressionTests />,
  parameters: {
    a11y: {
      config: {
        rules: [
          {
            id: "color-contrast",
            enabled: true,
          },
        ],
      },
    },
  },
};
