// next-seo.config.js
const defaultSEOConfig = {
  title: "Talk To Me | Mental Wellness & Emotional Support NGO",
  description:
    "Talk To Me is a non-profit mental wellness organization helping individuals through empathy, connection, and listening — not diagnosis.",
  canonical: "https://www.talktome.org.rw",
  openGraph: {
    url: "https://www.talktome.org.rw",
    title: "Talk To Me | Mental Wellness & Emotional Support NGO",
    description:
      "Join Talk To Me to make mental wellness accessible through empathy and genuine human connection.",
    images: [
      {
        url: "https://www.talktome.org.rw/og-image.jpg",
        width: 1200,
        height: 630,
        alt: "Talk To Me - Mental Wellness NGO",
      },
    ],
    site_name: "Talk To Me",
  },
  twitter: {
    handle: "@talktomeorgrw",
    site: "@talktomeorgrw",
    cardType: "summary_large_image",
  },
};

export default defaultSEOConfig;
