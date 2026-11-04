#!/usr/bin/env python3
"""Generate Mail Server Implementation Report PDF"""

import markdown2
from weasyprint import HTML

md_content = r"""
# Mail Server & Delivery System Integration in Kubernetes

## Complete Implementation Report

**Project:** ShopFast E-Platform  
**Date:** August 2026  
**Environment:** Kubernetes (k3s) on Single-Node Cluster  
**Domain:** shopfast.live  

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Architecture Overview](#2-architecture-overview)
3. [Component Details](#3-component-details)
4. [Kubernetes Deployment](#4-kubernetes-deployment)
5. [Email Delivery Flow](#5-email-delivery-flow)
6. [DNS Configuration](#6-dns-configuration)
7. [Security Implementation](#7-security-implementation)
8. [Troubleshooting Guide](#8-troubleshooting-guide)
9. [Appendices](#9-appendices)

---

## 1. Executive Summary

This document describes the complete implementation of a mail server and delivery system integrated into the ShopFast e-commerce platform running on Kubernetes. The system provides:

- **Inbound email reception** via Postfix SMTP (port 25)
- **Outbound email sending** via Postfix submission (ports 587/465)
- **Email storage** in mbox format at `/var/mail/`
- **Email access** via Dovecot IMAP (port 143)
- **Webmail interface** via RainLoop served at `https://shopfast.live/mail/`
- **DKIM signing** via OpenDKIM for email authentication

---

## 2. Architecture Overview

### 2.1 High-Level Architecture

```
Internet → [Port 25/587/465] → Postfix (SMTP Server)
                                ↓
                         Dovecot (Auth + IMAP)
                                ↓
                         /var/mail/ (Storage)
                                ↓
                         RainLoop (Webmail UI)
                                ↓
                         User Browser (HTTPS)
```

### 2.2 Component Interaction

| Component | Protocol | Port | Purpose |
|-----------|----------|------|---------|
| Postfix | SMTP | 25 | Inbound email reception |
| Postfix | Submission | 587 | Outbound email (STARTTLS) |
| Postfix | SMTPS | 465 | Outbound email (Implicit TLS) |
| Dovecot | IMAP | 143 | Email access for clients |
| Dovecot | Auth | 2525 | SASL authentication for Postfix |
| RainLoop | HTTP | 80 | Webmail interface |
| OpenDKIM | milter | 8891 | DKIM signature signing |

### 2.3 Pod Architecture

The mail system runs in two pods:

**mail-server pod** (hostNetwork: true):
- `postfix` container: Handles SMTP, queue management, delivery
- `dovecot` container: Handles IMAP, SASL authentication

**mail pod** (RainLoop UI):
- `mail` container: Nginx + PHP-FPM serving RainLoop webmail

---

## 3. Component Details

### 3.1 Postfix Configuration

**Image:** `localhost:5000/shopfast/postfix:v25`

**Key Configuration (`main.cf`):**

```ini
myhostname = mail.shopfast.live
mydomain = shopfast.live
myorigin = $mydomain
mydestination = localhost, shopfast.live
relay_domains = shopfast.live

# SASL Authentication via Dovecot
smtpd_sasl_auth_enable = yes
smtpd_sasl_type = dovecot
smtpd_sasl_path = inet:localhost:2525

# Restrictions
smtpd_recipient_restrictions = permit_mynetworks, permit_sasl_authenticated, reject_unauth_destination
smtpd_client_restrictions = permit

# Delivery Configuration
mailbox_command = /usr/local/bin/mbox-deliver "$USER"
queue_run_delay = 60s
mailbox_size_limit = 0
message_size_limit = 52428800

# TLS
smtpd_tls_cert_file = /opt/postfix-tls/cert.pem
smtpd_tls_key_file = /opt/postfix-tls/key.pem
smtpd_tls_security_level = may
```

**Delivery Script (`/usr/local/bin/mbox-deliver`):**

```sh
#!/bin/sh
FROM="From $SENDER $(date)"
{ echo "$FROM"; cat; } >> /var/mail/$1
```

This script appends incoming emails to the recipient's mbox file with the standard mbox `From ` separator line.

### 3.2 Dovecot Configuration

**Image:** `localhost:5000/shopfast/dovecot:v1`

**Key Configuration (`dovecot.conf`):**

```ini
protocols = imap
listen = *, ::
auth_mechanisms = plain login
auth_username_format = %Ln

passdb {
  driver = pam
}

userdb {
  driver = passwd
}

mail_location = mbox:/var/mail/:INBOX=/var/mail/%u
mail_uid = 5000
mail_gid = 5000

service auth {
  inet_listener {
    port = 2525
  }
}

ssl = no
```

### 3.3 RainLoop Webmail

**Image:** `localhost:5000/shopfast/mail:v13`

**Key Configuration:**

- **basePath:** `/mail/` - Makes RainLoop emit `/mail/*` URLs
- **Domain config:** `shopfast.live.ini` with IMAP/SMTP settings
- **SSL verification:** Disabled (for self-signed certs)

**Domain Configuration (`shopfast.live.ini`):**

```ini
imap_host = mail-imap
imap_port = 143
imap_secure = 0
smtp_host = mail-smtp
smtp_port = 587
smtp_secure = TLS
smtp_auth = 1
```

### 3.4 OpenDKIM Configuration

**Key Configuration (`opendkim.conf`):**

```ini
Socket                  inet:8891@127.0.0.1
UserID                  opendkim
SigningTable            refile:/etc/opendkim/SigningTable
KeyTable                refile:/etc/opendkim/KeyTable
SignatureAlgorithm       rsa-sha256
```

**DKIM DNS Record:**
- Selector: `shopfast2026`
- Record: `shopfast2026._domainkey.shopfast.live` (TXT)

---

## 4. Kubernetes Deployment

### 4.1 Namespace and Resources

All mail resources are deployed in the `shopfast` namespace:

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: shopfast
```

### 4.2 Persistent Storage

**PersistentVolume (`mail-storage.yaml`):**

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: mail-storage
spec:
  capacity:
    storage: 5Gi
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  storageClassName: manual
  hostPath:
    path: /var/mail
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: mail-storage
spec:
  storageClassName: manual
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 5Gi
```

### 4.3 Mail Server Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mail-server
  namespace: shopfast
spec:
  replicas: 1
  selector:
    matchLabels:
      app: mail-server
  template:
    metadata:
      labels:
        app: mail-server
    spec:
      hostNetwork: true
      dnsPolicy: ClusterFirstWithHostNet
      containers:
        - name: postfix
          image: localhost:5000/shopfast/postfix:v25
          ports:
            - containerPort: 25
            - containerPort: 587
            - containerPort: 465
          volumeMounts:
            - name: mail-storage
              mountPath: /var/mail
            - name: postfix-tls
              mountPath: /opt/postfix-tls
          command: ["sh", "-c", "adduser -D -u 1001 -g users admin 2>/dev/null; exec /usr/libexec/postfix/master -c /etc/postfix"]
        - name: dovecot
          image: localhost:5000/shopfast/dovecot:v1
          ports:
            - containerPort: 143
            - containerPort: 2525
          volumeMounts:
            - name: mail-storage
              mountPath: /var/mail
```

**Key Design Decisions:**

1. **hostNetwork: true** - Required for SMTP port 25 binding to node IP
2. **PersistentVolume** at `/var/mail` - Survives pod restarts
3. **TLS via Kubernetes Secret** - `mail-tls` secret with cert/key
4. **Admin user creation** at container startup - UID 1001 for mail delivery

### 4.4 Services

```yaml
# IMAP and Auth services
apiVersion: v1
kind: Service
metadata:
  name: mail-imap
spec:
  selector:
    app: mail-server
  ports:
    - name: imap
      port: 143
      targetPort: 143
    - name: auth-tcp
      port: 2525
      targetPort: 2525
  type: ClusterIP

# SMTP services
apiVersion: v1
kind: Service
metadata:
  name: mail-smtp
spec:
  selector:
    app: mail-server
  ports:
    - name: smtp
      port: 25
      targetPort: 25
    - name: submission
      port: 587
      targetPort: 587
    - name: submissions
      port: 465
      targetPort: 465
  type: ClusterIP
```

### 4.5 Ingress Configuration

```yaml
- path: /mail(/|$)(.*)
  pathType: Prefix
  backend:
    service:
      name: mail
      port:
        number: 80
```

The ingress rewrites `/mail/*` requests to `/` at the RainLoop pod, while RainLoop's basePath configuration generates `/mail/*` URLs back to the browser.

### 4.6 RainLoop Init Container

```yaml
initContainers:
  - name: init-rainloop-settings
    image: busybox:1.36
    command:
      - sh
      - -c
      - |
        mkdir -p /data/_data_/_default_/storage/cfg/ad/admin@shopfast.live
        cp /settings/settings_local /data/_data_/_default_/storage/cfg/ad/admin@shopfast.live/settings_local
        mkdir -p /data/_data_/_default_/configs
        cp /settings/application.ini /data/_data_/_default_/configs/application.ini
        mkdir -p /data/_data_/_default_/domains
        cp /settings/shopfast.live.ini /data/_data_/_default_/domains/shopfast.live.ini
```

---

## 5. Email Delivery Flow

### 5.1 Inbound Email (External → Admin)

```
1. External sender → SMTP connection to shopfast.live:25
2. Postfix accepts email, verifies recipient
3. Postfix queues email in /var/spool/postfix/
4. qmgr processes queue (every 60s)
5. local transport invokes mailbox_command
6. mbox-deliver script appends to /var/mail/admin
7. Dovecot serves email via IMAP to RainLoop
```

### 5.2 Outbound Email (Admin → External)

```
1. User composes email in RainLoop
2. RainLoop connects to mail-smtp:587 (STARTTLS)
3. Postfix verifies SASL auth via Dovecot (port 2525)
4. Postfix signs email with DKIM (OpenDKIM milter)
5. Postfix delivers to recipient's MX server
```

### 5.3 Queue Processing

| Setting | Value | Description |
|---------|-------|-------------|
| queue_run_delay | 60s | How often qmgr processes queue |
| minimal_backoff_time | 60s | Minimum delay before retry |
| maximal_queue_lifetime | 1d | Maximum time in queue |

---

## 6. DNS Configuration

### 6.1 Required Records

| Type | Name | Value | Purpose |
|------|------|-------|---------|
| A | mail.shopfast.live | 72.62.250.5 | Mail server IP |
| MX | shopfast.live | mail.shopfast.live | Mail exchange |
| TXT | shopfast2026._domainkey.shopfast.live | (DKIM public key) | DKIM authentication |
| TXT | shopfast.live | v=spf1 mx ~all | SPF record |

### 6.2 MX Record Configuration

```
shopfast.live.    IN  MX  10  mail.shopfast.live.
mail.shopfast.live.  IN  A  72.62.250.5
```

---

## 7. Security Implementation

### 7.1 TLS Configuration

- **Certificate:** Managed by cert-manager with Let's Encrypt
- **Secret:** `mail-tls` in `shopfast` namespace
- **Mount:** `/opt/postfix-tls/` in Postfix container
- **Levels:**
  - Port 25: `may` (opportunistic TLS)
  - Port 587: `encrypt` (required STARTTLS)
  - Port 465: `wrappermode` (implicit TLS)

### 7.2 Authentication

- **SASL Type:** Dovecot
- **Mechanisms:** PLAIN, LOGIN
- **Port:** 2525 (TCP loopback)
- **Path:** `inet:localhost:2525`

### 7.3 Firewall Rules (UFW)

```
25/tcp   ALLOW  Anywhere    # SMTP
143/tcp  ALLOW  10.42.1.0/24  # IMAP (internal)
587/tcp  ALLOW  Anywhere    # Submission
465/tcp  ALLOW  Anywhere    # SMTPS
```

### 7.4 DKIM Signing

- **Selector:** shopfast2026
- **Algorithm:** rsa-sha256
- **Socket:** inet:8891@127.0.0.1
- **milter_protocol:** 6

---

## 8. Troubleshooting Guide

### 8.1 Common Issues and Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| ConfigMap subPath caching | Kubernetes doesn't update subPath mounts | Bake config into Docker image |
| "mbox-deliver: not found" | Script missing from image | Add script to Dockerfile |
| Queue not processing | qmgr not triggering transport | Set queue_run_delay = 60s |
| Email stuck in queue | Permission issues | Ensure admin user exists (UID 1001) |
| "Domain is not allowed" | RainLoop config missing | Add domain config and basePath |
| Auth failure | SASL misconfiguration | Verify Dovecot auth on port 2525 |

### 8.2 Diagnostic Commands

```bash
# Check Postfix queue
kubectl exec -n shopfast <pod> -c postfix -- postqueue -p

# Check Postfix config
kubectl exec -n shopfast <pod> -c postfix -- postconf -n

# Test delivery manually
kubectl exec -n shopfast <pod> -c postfix -- postfix reload

# Check mail logs
kubectl exec -n shopfast <pod> -c postfix -- cat /var/log/maillog

# Verify IMAP access
kubectl exec -n shopfast <pod> -c dovecot -- doveadm auth test admin@shopfast.live Admin@123

# Check DKIM signing
kubectl exec -n shopfast <pod> -c postfix -- opendkim -e
```

### 8.3 Key Lessons Learned

1. **ConfigMap subPath mounts are cached** - Never use subPath for frequently updated configs
2. **Bake configs into Docker images** - More reliable for static configurations
3. **hostNetwork required for SMTP** - Port 25 needs node-level binding
4. **Delivery scripts must exist** - Postfix silently fails if mailbox_command path doesn't exist
5. **Queue delay matters** - Default 300s too slow for responsive email

---

## 9. Appendices

### Appendix A: File Structure

```
k8s/services/mail/
├── Dockerfile                    # RainLoop webmail image
├── nginx.conf                    # Nginx config for /mail/ path
├── php-fpm.conf                  # PHP-FPM pool config
├── supervisord.conf              # Process supervisor config
├── mail-deployment.yaml          # Postfix + Dovecot deployment
├── mail-services.yaml            # ClusterIP services
├── mail-storage.yaml             # PersistentVolume + PVC
├── configs/
│   ├── postfix.yaml              # Postfix ConfigMap (legacy)
│   └── dovecot.conf.yaml         # Dovecot ConfigMap
├── domains/
│   └── shopfast.live.ini         # RainLoop domain config
├── dovecot/
│   └── Dockerfile                # Dovecot image
├── postfix/
│   ├── Dockerfile                # Postfix + OpenDKIM image
│   ├── main.cf                   # Baked-in Postfix config
│   ├── master.cf                 # Baked-in master config
│   ├── opendkim.conf             # DKIM configuration
│   └── shopfast2026.private      # DKIM private key
└── rainloop-config/
    └── application.ini           # RainLoop application config
```

### Appendix B: Environment Variables

| Variable | Value | Used By |
|----------|-------|---------|
| RAINLOOP_VERSION | 1.17.0 | Mail UI image |
| MYHOSTNAME | mail.shopfast.live | Postfix |
| MYDOMAIN | shopfast.live | Postfix |

### Appendix C: Resource Requirements

| Component | CPU Request | CPU Limit | Memory Request | Memory Limit |
|-----------|-------------|-----------|----------------|--------------|
| Postfix | 100m | 250m | 128Mi | 256Mi |
| Dovecot | 100m | 250m | 128Mi | 256Mi |
| RainLoop (mail) | 50m | 250m | 128Mi | 256Mi |

### Appendix D: Version History

| Version | Date | Changes |
|---------|------|---------|
| v1 | Initial | Basic Postfix + Dovecot setup |
| v5 | Added | DKIM signing via OpenDKIM |
| v10 | Fixed | Domain config for RainLoop |
| v13 | Fixed | RainLoop basePath and SSL settings |
| v20 | Fixed | Added mbox-deliver script |
| v22 | Fixed | Removed ConfigMap dependency |
| v25 | Current | Baked config into image, 60s queue delay |

---

## Conclusion

The mail server implementation provides a complete email solution for the ShopFast platform. By baking configurations directly into Docker images and using a simple mbox-deliver script, the system achieves reliable email delivery with automatic queue processing every 60 seconds.

The architecture separates concerns:
- **Postfix** handles SMTP reception and delivery
- **Dovecot** handles authentication and IMAP access
- **RainLoop** provides a modern webmail interface
- **OpenDKIM** ensures email authentication

All components run as containers in Kubernetes with persistent storage, proper network configuration, and TLS security.
"""

# Convert markdown to HTML
html_content = markdown2.markdown(md_content, extras=['fenced-code-blocks', 'tables', 'toc'])

# Add CSS styling
styled_html = f"""
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        @page {{
            size: A4;
            margin: 2cm;
            @bottom-center {{
                content: "Page " counter(page) " of " counter(pages);
                font-size: 10px;
                color: #666;
            }}
        }}
        body {{
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            font-size: 11pt;
            line-height: 1.6;
            color: #333;
        }}
        h1 {{
            color: #1a5276;
            border-bottom: 3px solid #2980b9;
            padding-bottom: 10px;
            font-size: 24pt;
        }}
        h2 {{
            color: #2874a6;
            border-bottom: 1px solid #aed6f1;
            padding-bottom: 5px;
            margin-top: 30px;
            font-size: 18pt;
        }}
        h3 {{
            color: #2e86c1;
            font-size: 14pt;
        }}
        h4 {{
            color: #3498db;
            font-size: 12pt;
        }}
        code {{
            background-color: #f4f6f7;
            padding: 2px 6px;
            border-radius: 3px;
            font-family: 'Courier New', monospace;
            font-size: 9pt;
            color: #c0392b;
        }}
        pre {{
            background-color: #2c3e50;
            color: #ecf0f1;
            padding: 15px;
            border-radius: 5px;
            overflow-x: auto;
            font-size: 9pt;
            line-height: 1.4;
        }}
        pre code {{
            background-color: transparent;
            color: #ecf0f1;
            padding: 0;
        }}
        table {{
            border-collapse: collapse;
            width: 100%;
            margin: 15px 0;
        }}
        th, td {{
            border: 1px solid #bdc3c7;
            padding: 8px 12px;
            text-align: left;
        }}
        th {{
            background-color: #2980b9;
            color: white;
        }}
        tr:nth-child(even) {{
            background-color: #f2f3f4;
        }}
        blockquote {{
            border-left: 4px solid #3498db;
            margin: 15px 0;
            padding: 10px 20px;
            background-color: #ebf5fb;
        }}
        strong {{
            color: #1a5276;
        }}
        ul, ol {{
            margin: 10px 0;
        }}
        li {{
            margin: 5px 0;
        }}
    </style>
</head>
<body>
{html_content}
</body>
</html>
"""

# Generate PDF
output_path = "/home/junaid/Documents/junaid/Ecommerce Java + Spring Boot/ecommerce/mail-server-implementation-report.pdf"
HTML(string=styled_html).write_pdf(output_path)

print(f"PDF report generated: {output_path}")
