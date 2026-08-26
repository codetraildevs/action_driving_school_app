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

### Architecture

```
Internet → DatabaseMart LB (SSL) → VPS:80 (HTTP) → nginx → Next.js (PM2 :3000)
```

- **SSL**: Managed by DatabaseMart load balancer (Let's Encrypt)
- **HTTP**: VPS listens on port 80, receives traffic from LB
- **App**: Next.js runs on PM2 at `localhost:3000`

### Nginx Configuration

| File | Purpose | Deployed To |
|------|---------|-------------|
| `nginx-main.conf` | Main nginx.conf (http context) | `/etc/nginx/nginx.conf` |
| `nginx-databasemart.conf` | Site config (server block) | `/etc/nginx/sites-available/driving-school` |

### Deploy App (code changes only)

```bash
cd /home/project3
./deploy-vps.sh
```

### Deploy Nginx (config changes only)

```bash
cd /home/project3
git pull origin backend-deploy
sudo cp nginx-main.conf /etc/nginx/nginx.conf
sudo cp nginx-databasemart.conf /etc/nginx/sites-available/driving-school
sudo nginx -t && sudo systemctl reload nginx
```

### Full Deploy (app + nginx)

```bash
cd /home/project3
git pull origin backend-deploy
sudo cp nginx-main.conf /etc/nginx/nginx.conf
sudo cp nginx-databasemart.conf /etc/nginx/sites-available/driving-school
sudo nginx -t && sudo systemctl reload nginx
./deploy-vps.sh
```

### Rollback

```bash
# Nginx
sudo cp /etc/nginx/nginx.conf.backup /etc/nginx/nginx.conf
sudo nginx -t && sudo systemctl reload nginx

# App (PM2 keeps previous builds)
pm2 reload driving-school
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
| `./deploy-vps.sh` | Deploy to VPS |
| `./debug-login.sh` | Debug login issues (local only) |
| `./test-ssl.sh` | Test SSL/connectivity (local only) |

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
├── nginx-main.conf        # Main nginx config (for VPS)
├── nginx-databasemart.conf # Site nginx config (for VPS)
├── deploy-vps.sh          # VPS deployment script
├── ecosystem.config.js    # PM2 configuration
└── package.json           # Dependencies
```

---

## 🔧 Troubleshooting

### App Not Starting

```bash
# Check PM2 status
pm2 status

# View logs
pm2 logs driving-school --err --lines 50
pm2 logs driving-school --out --lines 50

# Restart app
pm2 restart driving-school

# Full restart
pm2 reload ecosystem.config.js --env production
```

### Nginx Issues

```bash
# Test config
sudo nginx -t

# View error logs
sudo tail -50 /var/log/nginx/driving-school_error.log

# Reload nginx
sudo systemctl reload nginx

# Check nginx status
sudo systemctl status nginx
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

### SSL/HTTPS Issues

The site uses DatabaseMart's load balancer for SSL. If HTTPS stops working:

1. Check the DatabaseMart control panel
2. Verify SSL certificate is active
3. Test locally: `curl -I http://localhost`
4. Test externally: `curl -I https://console.amategekoyumuhanda.rw/`

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
