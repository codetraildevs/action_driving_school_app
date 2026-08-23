/** @type {import('next').NextConfig} */
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

/** @type {import('next').NextConfig} */
const nextConfig = {
  outputFileTracingRoot: __dirname,
  serverExternalPackages: ['prisma', '@prisma/client', '@prisma/adapter-mariadb', 'firebase-admin', 'nodemailer', 'ffmpeg-static'],
  eslint: {
    ignoreDuringBuilds: true,
  },
  typescript: {
    ignoreBuildErrors: true,
  },
  images: {
    unoptimized: true,
  },
  // Fixes the "metadataBase not set" warning for social/OG images.
  metadataBase: new URL('https://console.amategekoyumuhanda.rw'),
  experimental: {
    cpus: 1,
    workerThreads: false,
    staticGenerationMinPagesPerWorker: 1000,
  },
}

export default nextConfig
