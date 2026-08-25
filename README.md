# 🚗 Driving School Rwanda - Backend

A modern Next.js backend powering the **Driving School Rwanda** admin panel and mobile app API.

## 📋 Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Database Setup](#database-setup)
- [Running the App](#running-the-app)
- [Deployment](#deployment)
- [API Endpoints](#api-endpoints)
- [Scripts](#scripts)
- [Project Structure](#project-structure)
- [License](#license)

---

## ✨ Features

- 🔐 **Authentication** - JWT-based auth with refresh tokens
- 👥 **Role-Based Access Control** - Admin, user roles with permissions
- 📊 **Admin Dashboard** - User management, analytics, subscriptions
- 💳 **Subscription Management** - Plans, payments, user requests
- 📚 **Learning Materials** - PDF uploads, downloads, previews
- 📝 **Exam System** - Questions, tests, attempts, translations
- 🌍 **Multi-Language** - English, French, Kinyarwanda support
- 🔔 **Push Notifications** - Firebase Cloud Messaging
- 📱 **SSE Real-time** - Server-Sent Events for live updates
- 🛡️ **Security** - Rate limiting, input validation, CORS

---

## 🛠️ Tech Stack

| Component | Technology |
|-----------|------------|
| **Framework** | Next.js 15 (App Router) |
| **Language** | TypeScript |
| **Database** | MySQL 8 |
| **ORM** | Prisma |
| **Auth** | NextAuth + JWT |
| **Runtime** | Node.js 22 |
| **Process Manager** | PM2 (Cluster Mode) |
| **Web Server** | Nginx (Reverse Proxy) |
| **Email** | Nodemailer |
| **Push Notifications** | Firebase Admin SDK |

---

## 📦 Prerequisites

- **Node.js** 22.x or higher
- **npm** 10.x or higher
- **MySQL** 8.x
- **Git**

---

## 🚀 Installation

### 1. Clone the Repository

```bash
git clone -b backend-deploy https://github.com/codetraildevs/action_driving_school_app.git
cd action_driving_school_app
```

### 2. Install Dependencies

```bash
npm install
```

### 3. Generate Prisma Client

```bash
npx prisma generate
```

---

## ⚙️ Configuration

### Create `.env` File

```bash
cp .env.example .env
```

### Environment Variables

```env
# ── Database ──
DATABASE_URL="mysql://username:password@localhost:3306/driving_school"

# ── App ──
NODE_ENV=production
PORT=3000

# ── Authentication ──
NEXTAUTH_URL="https://your-domain.com"
NEXTAUTH_SECRET="your-secret-key-here"

# ── Email (Optional) ──
SMTP_HOST="smtp.gmail.com"
SMTP_PORT=587
SMTP_USER="your-email@gmail.com"
SMTP_PASS="your-app-password"

# ── Firebase (Optional) ──
FIREBASE_PROJECT_ID="your-project-id"
FIREBASE_PRIVATE_KEY="your-private-key"
FIREBASE_CLIENT_EMAIL="your-client-email"
```

---

## 🗄️ Database Setup

### 1. Create Database

```sql
CREATE DATABASE driving_school CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Run Migrations

```bash
npx prisma migrate dev
```

### 3. Seed Database (Optional)

```bash
npx prisma db seed
```

### 4. Import Existing Data

If you have a SQL dump:

```bash
mysql -u username -p driving_school < database-dump.sql
```

---

## ▶️ Running the App

### Development Mode

```bash
npm run dev
```

Access at: `http://localhost:3000`

### Production Mode

```bash
npm run build
npm start
```

### With PM2 (Recommended for Production)

```bash
pm2 start ecosystem.config.js --env production
pm2 save
pm2 startup
```

---

## 🚢 Deployment

### Quick Deploy (VPS)

```bash
# One-command setup
bash go-live.sh

# Or manual setup
sudo bash server-setup.sh
```

### Deploy Updates

```bash
bash go-live.sh update
```

### Manual Deployment

```bash
# 1. Pull latest code
git pull origin backend-deploy

# 2. Install dependencies
npm install

# 3. Generate Prisma client
npx prisma generate

# 4. Build the app
npm run build

# 5. Restart PM2
pm2 reload ecosystem.config.js --env production
```

### SSL Setup

```bash
certbot --nginx -d your-domain.com
```

---

## 📡 API Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | User login |
| POST | `/api/auth/register` | User registration |
| POST | `/api/auth/logout` | User logout |
| POST | `/api/auth/refresh` | Refresh token |
| POST | `/api/auth/forgot-password` | Send reset email |
| POST | `/api/auth/reset-password` | Reset password |
| POST | `/api/auth/verify-otp` | Verify OTP |

### Users

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users/profile` | Get user profile |
| PUT | `/api/users/profile` | Update profile |
| DELETE | `/api/users/delete` | Delete account |

### Subscriptions

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/subscriptions` | List plans |
| GET | `/api/subscriptions/:id` | Get plan details |
| POST | `/api/subscriptions/user` | Get user subscription |
| POST | `/api/subscriptions/cancel` | Cancel subscription |

### Learning Materials

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/learning-materials` | List materials |
| GET | `/api/learning-materials/:id` | Get material |
| POST | `/api/learning-materials/upload` | Upload material |
| GET | `/api/learning-materials/:id/download` | Download |
| GET | `/api/learning-materials/:id/preview` | Preview |

### Tests & Questions

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/tests` | List tests |
| GET | `/api/tests/:id` | Get test |
| POST | `/api/tests/:id/attempt` | Start attempt |
| GET | `/api/questions` | List questions |
| GET | `/api/questions/:id` | Get question |

### Admin

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/users` | List users |
| GET | `/api/admin/users/:id` | Get user details |
| PUT | `/api/admin/users/:id` | Update user |
| GET | `/api/admin/analytics/dashboard` | Dashboard stats |
| GET | `/api/admin/subscriptions` | List subscriptions |
| PUT | `/api/admin/subscriptions/:id` | Update subscription |

### Health Check

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Server health status |

---

## 📜 Scripts

| Command | Description |
|---------|-------------|
| `npm run dev` | Start development server |
| `npm run build` | Build for production |
| `npm start` | Start production server |
| `npm run lint` | Run ESLint |
| `bash go-live.sh` | One-command VPS setup |
| `bash go-live.sh update` | Deploy updates |
| `bash server-setup.sh check` | Check server health |
| `bash server-setup.sh repair` | Repair common issues |

---

## 📁 Project Structure

```
├── app/                    # Next.js App Router
│   ├── api/               # API routes
│   │   ├── auth/          # Authentication endpoints
│   │   ├── admin/         # Admin endpoints
│   │   ├── users/         # User endpoints
│   │   ├── subscriptions/ # Subscription endpoints
│   │   └── health/        # Health check
│   ├── admin/             # Admin dashboard pages
│   ├── landing/           # Landing page
│   └── layout.tsx         # Root layout
├── components/            # React components
│   ├── admin/             # Admin components
│   ├── ui/                # UI components (shadcn)
│   └── profile-page/      # Profile components
├── lib/                   # Utility functions
│   ├── auth/              # Authentication helpers
│   ├── middleware/        # Custom middleware
│   └── prismaDB.ts        # Database client
├── prisma/                # Database schema
├── public/                # Static assets
├── types/                 # TypeScript types
├── data/                  # Configuration data
├── server.js              # Custom Node.js server
├── ecosystem.config.js    # PM2 configuration
├── server-setup.sh        # VPS setup script
├── go-live.sh             # One-command deploy
└── package.json           # Dependencies
```

---

## 🔧 Troubleshooting

### App Not Starting

```bash
# Check PM2 status
pm2 status

# View logs
pm2 logs driving-school

# Restart app
pm2 restart driving-school
```

### Database Connection Issues

```bash
# Test connection
mysql -u username -p -h localhost

# Check MySQL status
sudo systemctl status mysql
```

### Build Errors

```bash
# Clear cache and rebuild
rm -rf .next node_modules
npm install
npx prisma generate
npm run build
```

### Permission Errors

```bash
# Fix permissions
sudo chown -R deploy:deploy /home/project3
```

---

## 📞 Support

For issues or questions:

- 📧 Email: support@amategekoyumuhanda.rw
- 🌐 Website: https://amategekoyumuhanda.rw

---

## 📄 License

© 2026 Action Driving School Rwanda. All rights reserved.

---

## 🙏 Acknowledgments

- [Next.js](https://nextjs.org/)
- [Prisma](https://www.prisma.io/)
- [Tailwind CSS](https://tailwindcss.com/)
- [shadcn/ui](https://ui.shadcn.com/)
