#!/bin/bash

###############################################################################
# ShopFast - Automated Deployment to Hostinger VPS
# This script automates the entire deployment process from your local machine
###############################################################################

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Configuration - UPDATE THESE VALUES
VPS_IP="72.62.250.5"
VPS_USER="root"
REMOTE_DIR="/opt/shopfast"
LOCAL_PROJECT_DIR="/home/junaid/Documents/junaid/Ecommerce Java + Spring Boot/ecommerce/backend"

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if running on Linux/Mac
check_os() {
    if [[ ! -d "$LOCAL_PROJECT_DIR" ]]; then
        log_error "Project directory not found: $LOCAL_PROJECT_DIR"
        log_info "Please update LOCAL_PROJECT_DIR in this script if your path is different."
        exit 1
    fi
}

# Step 1: Upload files to VPS
upload_files() {
    log_info "Uploading project files to VPS..."
    scp -r "$LOCAL_PROJECT_DIR"/* ${VPS_USER}@${VPS_IP}:${REMOTE_DIR}/
    log_success "Files uploaded successfully"
}

# Step 2: Execute remote commands
execute_remote() {
    local command="$1"
    ssh ${VPS_USER}@${VPS_IP} "$command"
}

# Step 3: Configure environment on VPS
configure_env() {
    log_info "Setting up environment configuration on VPS..."

    ssh ${VPS_USER}@${VPS_IP} "cd ${REMOTE_DIR} && \
        cp .env.production.example .env.production && \
        echo 'Environment file created.'"
    
    log_warning "IMPORTANT: You must edit .env.production on the VPS and set:"
    echo "  1. POSTGRES_PASSWORD (strong password)"
    echo "  2. JWT_SECRET (min 32 characters)"
    echo ""
    log_info "To edit, run: ssh root@${VPS_IP} 'nano ${REMOTE_DIR}/.env.production'"
}

# Step 4: Deploy on VPS
deploy_remote() {
    log_info "Starting deployment on VPS..."
    ssh ${VPS_USER}@${VPS_IP} "cd ${REMOTE_DIR} && chmod +x deploy.sh && ./deploy.sh deploy"
}

# Step 5: Verify deployment
verify_deployment() {
    log_info "Verifying deployment..."
    sleep 5
    ssh ${VPS_USER}@${VPS_IP} "cd ${REMOTE_DIR} && ./status.sh" || true
}

# Main deployment flow
main() {
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}  ShopFast Hostinger Deployment Tool    ${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    log_info "Target VPS: ${VPS_USER}@${VPS_IP}"
    log_info "Local directory: $LOCAL_PROJECT_DIR"
    log_info "Remote directory: $REMOTE_DIR"
    echo ""

    # Confirmation
    read -p "Is this correct? (yes/no): " -r
    if [[ ! $REPLY =~ ^yes$ ]]; then
        log_info "Aborted. Please update the script with correct values."
        exit 0
    fi

    # Check local project exists
    check_os

    # Execute steps
    upload_files
    configure_env
    log_warning "Next step: Edit .env.production on VPS with secure credentials"
    echo ""
    read -p "Have you edited .env.production on the VPS? (yes/no): " -r
    if [[ ! $REPLY =~ ^yes$ ]]; then
        log_info "Please edit the file first:"
        echo "  ssh root@${VPS_IP} 'nano ${REMOTE_DIR}/.env.production'"
        exit 0
    fi

    log_info "Starting deployment... This will take 15-30 minutes."
    read -p "Continue? (yes/no): " -r
    if [[ ! $REPLY =~ ^yes$ ]]; then
        log_info "Aborted."
        exit 0
    fi

    deploy_remote
    verify_deployment

    log_success "Deployment completed!"
    echo ""
    log_info "Access your application:"
    echo "  API Gateway: http://$(ssh ${VPS_USER}@${VPS_IP} 'hostname -I | awk "{print \$1}"')"
    echo "  Eureka: http://$(ssh ${VPS_USER}@${VPS_IP} 'hostname -I | awk "{print \$1}"'):8761"
    echo ""
    log_info "Management commands:"
    echo "  ssh root@${VPS_IP} 'cd ${REMOTE_DIR} && ./status.sh'"
    echo "  ssh root@${VPS_IP} 'cd ${REMOTE_DIR} && ./logs.sh'"
}

# Run main
main "$@"
