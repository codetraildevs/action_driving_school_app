/** @type {import('next').NextConfig} */
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

/** @type {import('next').NextConfig} */
const nextConfig = {
  // This repo lives inside a larger parent directory (D:\software\DRIVINGSCHOOL2)
  // that also has its own package-lock.json. Next.js then misdetects the parent
  // as a monorepo workspace root and its output file tracing walks up through
  // the parent — and beyond — into the user's home folder (causing EPERM crashes
  // on Windows, e.g. scandir 'C:\Users\HP\Cookies'). Pin the tracing root to the
  // app itself so the build only ever looks inside this project.
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
  experimental: {
    cpus: 1,
    workerThreads: false,
    // Put ALL static pages into a single export worker so the build never
    // spawns more than one extra process (stays under the host's NPROC limit).
    staticGenerationMinPagesPerWorker: 1000,
    serverActions: {
      // Allow Server Actions from the external IP (with and without port)
      // and the production domain. Fixes the x-forwarded-host mismatch
      // when the VPS is accessed on a non-standard external port.
      allowedOrigins: [
        "108.181.215.244:10041",
        "108.181.215.244",
        "console.amategekoyumuhanda.rw",
      ],
    },
  },
}

 

export default nextConfig
