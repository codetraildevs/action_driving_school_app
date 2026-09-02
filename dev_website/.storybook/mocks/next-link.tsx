import React from "react";

/**
 * Mock for next/link that renders a plain <a> tag.
 * Used in Storybook where Next.js Router is not available.
 */
export default function Link({
  href,
  children,
  ...props
}: {
  href: string;
  children: React.ReactNode;
  [key: string]: any;
}) {
  return (
    <a href={href} {...props}>
      {children}
    </a>
  );
}
