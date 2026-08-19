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
  // Turbopack reads tsconfig.json paths natively (@/* → ./*), so no webpack
  // alias is needed.  The PrismaPlugin (webpack-only) is replaced by
  // serverExternalPackages which works with both Turbopack and webpack tracing.
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
    workerThreads: true,
    // Put ALL static pages into a single export worker so the build never
    // spawns more than one extra process (stays under the host's NPROC limit).
    staticGenerationMinPagesPerWorker: 1000,
  },
}

 

export default nextConfig
