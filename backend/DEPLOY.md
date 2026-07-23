# Quick Deployment Guide for Hostinger

## One-Line Deployment

After connecting to your Hostinger VPS:

```bash
git clone <your-repo> /opt/shopfast && cd /opt/shopfast && cp .env.production.example .env.production && nano .env.production && ./deploy.sh deploy
```

## Step-by-Step (5 Minutes)

### 1. Connect to Your VPS
```bash
ssh root@72.62.250.5
```

### 2. Upload Project Files
```bash
# From your local machine (not on VPS)
scp -r /path/to/backend/* root@72.62.250.5:/opt/shopfast/
```

### 3. Configure Environment
```bash
cd /opt/shopfast
cp .env.production.example .env.production
nano .env.production
# Set POSTGRES_PASSWORD and JWT_SECRET, save and exit
```

### 4. Deploy
```bash
./deploy.sh deploy
```

**Wait 15-30 minutes** for all services to build and start.

### 5. Verify
```bash
./status.sh
```

Your application should now be accessible at:
- **API**: http://YOUR_SERVER_IP
- **Eureka Dashboard**: http://YOUR_SERVER_IP:8761

---

## Management Commands

| Command | Action |
|---------|--------|
| `./start.sh` | Start all services |
| `./stop.sh` | Stop all services |
| `./restart.sh` | Restart all services |
| `./status.sh` | Show service status |
| `./logs.sh` | View all logs |
| `./logs.sh api-gateway` | View specific service logs |
| `./deploy.sh backup` | Backup databases |
| `./deploy.sh clean` | Remove everything |

---

## Important Files

- `docker-compose.prod.yml` - Production configuration
- `.env.production` - Environment variables (create from example)
- `deploy.sh` - Main deployment & management script
- `README-DEPLOYMENT.md` - Complete documentation

---

## Need Help?

See [README-DEPLOYMENT.md](README-DEPLOYMENT.md) for:
- SSL/TLS setup
- Troubleshooting
- Backup & recovery
- Security hardening
- Scaling instructions
