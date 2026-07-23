# ShopFast E-Commerce - Hostinger Deployment Guide

This guide provides step-by-step instructions for deploying the ShopFast microservices application to a Hostinger VPS.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Pre-Deployment Setup](#pre-deployment-setup)
3. [Deployment Methods](#deployment-methods)
4. [Post-Deployment Configuration](#post-deployment-configuration)
5. [Management & Monitoring](#management--monitoring)
6. [SSL/TLS Setup](#ssltls-setup)
7. [Troubleshooting](#troubleshooting)
8. [Backup & Recovery](#backup--recovery)
9. [Scaling](#scaling)

---

## Prerequisites

### Hostinger VPS Requirements

- **VPS Plan**:至少 4GB RAM, 2 CPU cores, 80GB SSD (推荐 8GB RAM for production)
- **OS**: Ubuntu 22.04 LTS (recommended) or 20.04 LTS
- **Root Access**: Required for Docker installation
- **Domain**: Optional but recommended (for SSL certificates)

### Open Ports on Hostinger

Ensure the following ports are open in your Hostinger firewall:

| Port | Service | Description |
|------|---------|-------------|
| 80 | HTTP | API Gateway (required) |
| 443 | HTTPS | API Gateway (SSL) |
| 22 | SSH | Management (already open) |
| 8761 | Eureka | Service Discovery Dashboard |
| 5432 | PostgreSQL | Database (optional - for direct access) |
| 6379 | Redis | Cache (optional) |
| 9092 | Kafka | Message Broker (optional) |
| 9200 | Elasticsearch | Search Engine (optional) |

**Note**: Only ports 80 and 443 need to be publicly accessible for the application to work. Other ports can be restricted to internal network or SSH tunnel.

---

## Pre-Deployment Setup

### Step 1: Connect to Your Hostinger VPS

```bash
ssh root@72.62.250.5
# Or use your actual VPS IP
```

### Step 2: Update System Packages

```bash
apt-get update && apt-get upgrade -y
```

### Step 3: Install Git (if not already installed)

```bash
apt-get install -y git
```

### Step 4: Clone or Upload Your Application

**Option A: Clone from Git Repository**

```bash
git clone <your-repository-url> /opt/shopfast
cd /opt/shopfast
```

**Option B: Upload via SCP/SFTP**

From your local machine:

```bash
# Upload the entire project directory
scp -r /path/to/backend/* root@72.62.250.5:/opt/shopfast/
```

Then SSH into the server:

```bash
ssh root@72.62.250.5
cd /opt/shopfast
```

---

## Deployment Methods

### Method 1: Automated Deployment (Recommended)

The easiest way is to use the provided deployment script.

#### 1.1 Setup Environment Configuration

```bash
# Copy the example environment file
cp .env.production.example .env.production

# Edit the file with your secure values
nano .env.production
```

**Required changes:**

```env
# Generate a strong password for PostgreSQL
POSTGRES_PASSWORD=YourVerySecurePassword123!@#

# Generate a JWT secret (minimum 32 characters)
# You can generate one with: openssl rand -base64 64
JWT_SECRET=your_jwt_secret_key_min_32_characters_long_for_security_1234567890
```

**Save and exit** (Ctrl+X, then Y, then Enter in nano).

#### 1.2 Run the Deployment

```bash
# Make scripts executable (if not already)
chmod +x deploy.sh start.sh stop.sh

# Run the deployment
./deploy.sh deploy
```

The script will:
- Install Docker if not present
- Build all service images
- Start all containers
- Configure networking
- Verify health checks

**First deployment takes 15-30 minutes** (building all services).

#### 1.3 Monitor Deployment

```bash
# Check service status
./status.sh

# View logs in real-time
./logs.sh
# Or view specific service logs
./logs.sh api-gateway
```

---

### Method 2: Manual Deployment

If you prefer more control, follow these steps:

#### 2.1 Install Docker Manually

```bash
# Update package index
apt-get update

# Install prerequisites
apt-get install -y apt-transport-https ca-certificates curl gnupg lsb-release

# Add Docker's official GPG key
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

# Add Docker repository
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null

# Install Docker Engine
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Enable Docker to start on boot
systemctl enable docker
systemctl start docker

# Verify installation
docker --version
docker-compose --version
```

#### 2.2 Build and Deploy

```bash
cd /opt/shopfast

# Create environment file
cp .env.production.example .env.production
nano .env.production  # Edit as described above

# Build all services (this takes time)
docker-compose -f docker-compose.prod.yml build

# Start all services
docker-compose -f docker-compose.prod.yml up -d

# Check status
docker-compose -f docker-compose.prod.yml ps

# View logs
docker-compose -f docker-compose.prod.yml logs -f
```

---

## Post-Deployment Configuration

### Verify Services Are Running

```bash
# Check all services
docker-compose -f docker-compose.prod.yml ps

# Expected output: All services should show "Up" and "healthy"
```

### Test API Gateway

```bash
# Get your server IP
SERVER_IP=$(hostname -I | awk '{print $1}')

# Test health endpoint
curl http://$SERVER_IP/actuator/health

# Test Eureka dashboard
curl http://$SERVER_IP:8761/eureka/apps
```

### Check Service Discovery

Open in browser: `http://YOUR_SERVER_IP:8761`

You should see the Eureka dashboard with all registered services.

---

## Management & Monitoring

### Quick Commands

```bash
# Start all services
./start.sh

# Stop all services
./stop.sh

# Restart all services
./restart.sh

# Check status
./status.sh

# View all logs
./logs.sh

# View specific service logs
./logs.sh api-gateway
./logs.sh auth-service
./logs.sh postgres

# Follow logs (tail)
./tail.sh
```

### Docker Compose Commands

```bash
# View logs for specific service
docker-compose -f docker-compose.prod.yml logs -f api-gateway

# Restart specific service
docker-compose -f docker-compose.prod.yml restart api-gateway

# View resource usage
docker stats

# Execute command in container
docker-compose -f docker-compose.prod.yml exec postgres psql -U postgres -d auth_db

# Backup database (see Backup section below)
./deploy.sh backup
```

### Health Checks

All services expose Actuator health endpoints:

```
http://YOUR_IP:8080/actuator/health      # API Gateway
http://YOUR_IP:8087/actuator/health      # Auth Service
http://YOUR_IP:8086/actuator/health      # User Service
http://YOUR_IP:8081/actuator/health      # Product Service
... (and so on for each service)
```

---

## SSL/TLS Setup

### Option 1: Let's Encrypt with Certbot (Free SSL)

#### Install Certbot

```bash
apt-get install -y certbot python3-certbot-nginx
```

#### Obtain SSL Certificate

```bash
# Stop nginx if running (we'll use standalone mode)
systemctl stop nginx 2>/dev/null || true

# Get certificate (replace with your domain)
certbot certonly --standalone -d yourdomain.com -d www.yourdomain.com

# Test auto-renewal
certbot renew --dry-run
```

#### Configure Docker Services for HTTPS

Update your `docker-compose.prod.yml` to include SSL certificates:

1. Mount SSL certificates:
```yaml
api-gateway:
  volumes:
    - /etc/letsencrypt/live/yourdomain.com/fullchain.pem:/etc/ssl/certs/fullchain.pem
    - /etc/letsencrypt/live/yourdomain.com/privkey.pem:/etc/ssl/certs/privkey.pem
```

2. Configure Spring Boot for HTTPS (update `api-gateway/src/main/resources/application.yml`):

```yaml
server:
  port: 8443
  ssl:
    key-store: /etc/ssl/certs/keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

3. Rebuild and restart:
```bash
docker-compose -f docker-compose.prod.yml build api-gateway
docker-compose -f docker-compose.prod.yml up -d api-gateway
```

### Option 2: Hostinger SSL (if provided)

If your Hostinger plan includes SSL:

1. Install SSL via Hostinger control panel
2. Configure API Gateway to use provided certificates
3. Update firewall to redirect HTTP to HTTPS

---

## Troubleshooting

### Common Issues

#### 1. Port Already in Use

```bash
# Check what's using port 80
ss -tulpn | grep :80

# Stop conflicting service (e.g., nginx, apache)
systemctl stop nginx
systemctl disable nginx
```

#### 2. Docker Permission Denied

```bash
# Add user to docker group (if not using root)
usermod -aG docker $USER
newgrp docker

# Or use sudo (already configured in scripts)
```

#### 3. Services Not Starting

```bash
# Check logs
docker-compose -f docker-compose.prod.yml logs

# Common issues:
# - Out of memory: Add swap space
# - Disk full: Clean up with docker system prune -a
# - Database connection: Check PostgreSQL is healthy first
```

#### 4. Database Connection Errors

```bash
# Check PostgreSQL logs
docker-compose -f docker-compose.prod.yml logs postgres

# Enter PostgreSQL container
docker-compose -f docker-compose.prod.yml exec postgres bash

# Check databases
psql -U postgres -l
```

#### 5. Out of Memory (OOM) Errors

Your VPS may need more RAM. Check with:

```bash
free -h
docker stats
```

**Solution**: Upgrade Hostinger VPS plan or reduce services (disable elasticsearch, logstash if not needed).

#### 6. Slow First Request (Cold Start)

Spring Boot apps need 30-60 seconds to fully start. Health checks handle this, but you may see delays on first request after deployment.

---

## Backup & Recovery

### Automated Daily Backups

Create a cron job for automatic backups:

```bash
crontab -e
```

Add this line (backs up daily at 2 AM):

```
0 2 * * * /opt/shopfast/deploy.sh backup > /var/log/shopfast-backup.log 2>&1
```

### Manual Backup

```bash
# Create backup
./deploy.sh backup

# This creates: backups/db_backup_YYYYMMDD_HHMMSS.sql.gz
```

### Restore from Backup

```bash
# List backups
ls -lh backups/

# Restore (will overwrite current data!)
./deploy.sh restore backups/db_backup_20240101_120000.sql.gz
```

### Backup Important Data

Besides databases, also backup:

```bash
# Docker volumes (persistent data)
docker run --rm -v shopfast_postgres_data:/data -v $(pwd):/backup alpine tar czf /backup/postgres_data.tar.gz /data

# Environment file (contains secrets!)
cp .env.production .env.production.backup

# Upload to safe location
# scp .env.production.backup user@remote-server:/backup/
```

---

## Scaling

### Horizontal Scaling (Multiple Instances)

To scale a specific service (e.g., product-service):

```bash
# Scale to 3 instances
docker-compose -f docker-compose.prod.yml up -d --scale product-service=3

# Note: You'll need to configure load balancing in API Gateway
```

### Vertical Scaling (More Resources)

Update `docker-compose.prod.yml` to allocate more resources:

```yaml
product-service:
  deploy:
    resources:
      limits:
        memory: 2G
        cpus: '1.0'
      reservations:
        memory: 1G
        cpus: '0.5'
```

Then restart:
```bash
docker-compose -f docker-compose.prod.yml up -d
```

---

## Security Recommendations

1. **Change Default Passwords**: Always use strong, unique passwords
2. **Firewall Configuration**: Only expose ports 80, 443, and 22
3. **Regular Updates**: Keep Docker images updated
4. **SSL/TLS**: Always use HTTPS in production
5. **JWT Secret**: Use a cryptographically secure random string (min 32 chars)
6. **Database Backups**: Automate daily backups
7. **Monitor Logs**: Check logs regularly for suspicious activity
8. **Disable Unused Services**: Comment out elasticsearch, kibana if not needed

---

## Uninstallation

To completely remove the application:

```bash
# Stop and remove everything
./deploy.sh clean

# Remove Docker (if desired)
apt-get purge -y docker-ce docker-ce-cli containerd.io
rm -rf /var/lib/docker

# Remove application files
cd /opt
rm -rf shopfast

# Remove backups (if desired)
rm -rf /opt/backups
```

---

## Support

If you encounter issues:

1. Check logs: `./logs.sh`
2. Verify Docker: `docker info`
3. Check resources: `free -h` and `df -h`
4. Review this guide's troubleshooting section

---

## Quick Reference Card

| Task | Command |
|------|---------|
| First deployment | `./deploy.sh deploy` |
| Start services | `./start.sh` |
| Stop services | `./stop.sh` |
| Restart services | `./restart.sh` |
| Check status | `./status.sh` |
| View logs | `./logs.sh [service]` |
| Backup DB | `./deploy.sh backup` |
| Restore DB | `./deploy.sh restore <file>` |
| Clean removal | `./deploy.sh clean` |

---

**Last Updated**: 2025-04-24
**Version**: 1.0.0
**Deployment Target**: Hostinger VPS (Ubuntu 22.04 LTS)
