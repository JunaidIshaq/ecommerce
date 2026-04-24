# Hostinger Deployment Checklist

Use this checklist to ensure a smooth deployment to your Hostinger VPS.

---

## Pre-Deployment (Local Machine)

- [ ] All code changes are committed and pushed to Git
- [ ] Application builds successfully locally: `mvn clean package` or `./mvnw package`
- [ ] Docker images build successfully: `docker-compose -f docker-compose.prod.yml build`
- [ ] Environment variables are configured in `.env.production`
- [ ] JWT_SECRET is set to a strong random string (min 32 chars)
- [ ] POSTGRES_PASSWORD is set to a strong password
- [ ] `.env.production` is added to `.gitignore` (never commit secrets!)
- [ ] Deployment files are ready:
  - [x] `docker-compose.prod.yml`
  - [x] `deploy.sh` (and helper scripts)
  - [x] `.env.production.example`
  - [x] `README-DEPLOYMENT.md`
  - [x] `DEPLOY.md`

---

## VPS Setup (First Time Only)

- [ ] Hostinger VPS is provisioned (Ubuntu 22.04 LTS recommended)
- [ ] Root SSH access configured
- [ ] Firewall ports open: 22, 80, 443
- [ ] Sufficient disk space (至少 20GB free)
- [ ] Sufficient RAM (至少 4GB, 推荐 8GB)

---

## Deployment Steps

### Step 1: Connect to VPS
```bash
ssh root@72.62.250.5
```

### Step 2: Upload Project Files
```bash
# From local machine
scp -r /path/to/backend/* root@72.62.250.5:/opt/shopfast/
```

### Step 3: Navigate and Configure
```bash
cd /opt/shopfast
cp .env.production.example .env.production
nano .env.production  # Edit POSTGRES_PASSWORD and JWT_SECRET
chmod +x deploy.sh start.sh stop.sh
```

### Step 4: Deploy
```bash
./deploy.sh deploy
```

**Expected time**: 15-30 minutes (first deployment)

### Step 5: Verify
```bash
./status.sh
```

All services should show `Up` and `healthy`.

---

## Post-Deployment Verification

- [ ] All containers are running: `docker ps`
- [ ] All health checks pass: `./deploy.sh status`
- [ ] API Gateway accessible: `curl http://YOUR_IP/actuator/health`
- [ ] Eureka dashboard: `http://YOUR_IP:8761` shows all services
- [ ] Database initialized: `docker-compose -f docker-compose.prod.yml exec postgres psql -U postgres -l`
- [ ] No errors in logs: `./logs.sh` (check for ERROR or WARN)
- [ ] Ports 80 and 443 are listening: `ss -tulpn | grep -E ":80|:443"`

---

## Optional Configuration

### SSL/TLS Setup
- [ ] Domain DNS points to VPS IP
- [ ] SSL certificate obtained (Let's Encrypt or Hostinger SSL)
- [ ] API Gateway configured for HTTPS
- [ ] HTTP to HTTPS redirect configured
- [ ] SSL test passes: `https://yourdomain.com/actuator/health`

### Monitoring & Backups
- [ ] Daily backup cron job configured: `crontab -e`
- [ ] Backup tested: `./deploy.sh backup` creates file in `backups/`
- [ ] Log rotation configured (if needed)
- [ ] Monitoring/alerting setup (optional)

### Security Hardening
- [ ] Firewall configured (only 80, 443, 22 open)
- [ ] Fail2ban installed (optional)
- [ ] SSH keys configured (disable password login)
- [ ] Unused services disabled
- [ ] Regular updates scheduled

---

## Common Issues & Quick Fixes

| Issue | Solution |
|-------|----------|
| Port 80 in use | `systemctl stop nginx && systemctl disable nginx` |
| Out of memory | Add swap: `fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile` |
| Disk full | `docker system prune -a` (cleanup unused images) |
| Services not starting | Check logs: `./logs.sh` |
| Database errors | `docker-compose -f docker-compose.prod.yml logs postgres` |

---

## Rollback Plan

If deployment fails:

```bash
# Stop all services
./stop.sh

# Remove everything
./deploy.sh clean

# Restore from backup (if available)
./deploy.sh restore backups/db_backup_YYYYMMDD_HHMMSS.sql.gz
```

---

## Success Criteria

✅ All microservices are running and healthy
✅ API Gateway responds on port 80
✅ Service discovery (Eureka) shows all services
✅ Databases are initialized with seed data
✅ No error logs in any service
✅ Application accessible via browser or API client
✅ Backup system in place
✅ SSL certificate installed (if using HTTPS)

---

## Support Resources

- **Full Documentation**: [README-DEPLOYMENT.md](README-DEPLOYMENT.md)
- **Quick Reference**: [DEPLOY.md](DEPLOY.md)
- **Docker Docs**: https://docs.docker.com/
- **Hostinger Support**: https://www.hostinger.com/support

---

**Deployment Date**: _______________
**VPS IP**: _______________
**Domain**: _______________
**JWT Secret**: [stored securely]
**DB Password**: [stored securely]
