#!/bin/bash

###############################################################################
# ShopFast E-Commerce - Production Deployment Script for Hostinger VPS
###############################################################################
# This script automates the deployment of the entire microservices stack
# to a Hostinger VPS using Docker Compose.
#
# Usage: ./deploy.sh [OPTION]
# Options:
#   install    - Install Docker & Docker Compose (first-time setup)
#   deploy     - Deploy/update the application (default)
#   start      - Start all services
#   stop       - Stop all services
#   restart    - Restart all services
#   status     - Show service status
#   logs       - Show logs for all services
#   clean      - Stop and remove all containers, volumes, and images
#   backup     - Backup PostgreSQL databases
#   restore    - Restore PostgreSQL databases from backup
###############################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
COMPOSE_FILE="docker-compose.prod.yml"
ENV_FILE=".env.production"
BACKUP_DIR="./backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Logging functions
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

# Check if running as root (required for Docker)
check_root() {
    if [[ $EUID -ne 0 ]]; then
        log_error "This script must be run as root (use sudo)"
        exit 1
    fi
}

# Install Docker and Docker Compose
install_docker() {
    log_info "Installing Docker and Docker Compose..."

    # Update package index
    apt-get update

    # Install prerequisites
    apt-get install -y \
        apt-transport-https \
        ca-certificates \
        curl \
        gnupg \
        lsb-release \
        software-properties-common

    # Add Docker's official GPG key
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

    # Add Docker repository
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null

    # Install Docker Engine
    apt-get update
    apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

    # Install Docker Compose (standalone)
    curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose

    # Enable Docker to start on boot
    systemctl enable docker
    systemctl start docker

    log_success "Docker and Docker Compose installed successfully"
}

# Check if Docker is installed and running
check_docker() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed. Run './deploy.sh install' first."
        exit 1
    fi

    if ! docker info &> /dev/null; then
        log_error "Docker daemon is not running. Start it with: systemctl start docker"
        exit 1
    fi
}

# Setup environment file
setup_env() {
    if [[ ! -f "$ENV_FILE" ]]; then
        log_warning "Environment file not found. Creating from template..."
        cp .env.production.example "$ENV_FILE"
        log_error "Please edit $ENV_FILE and set your secure passwords and secrets!"
        log_info "Required variables:"
        echo "  - POSTGRES_PASSWORD (strong password)"
        echo "  - JWT_SECRET (min 32 characters)"
        exit 1
    else
        log_success "Environment file found: $ENV_FILE"
    fi
}

# Build and start all services
deploy() {
    log_info "Starting deployment..."

    check_docker
    setup_env

    # Pull latest images (for services using pre-built images)
    log_info "Pulling base images..."
    docker-compose -f "$COMPOSE_FILE" pull || log_warning "Some images could not be pulled (this is normal for custom builds)"

    # Build services
    log_info "Building application services..."
    docker-compose -f "$COMPOSE_FILE" build --no-cache

    # Start services
    log_info "Starting all services..."
    docker-compose -f "$COMPOSE_FILE" up -d

    # Wait for services to be healthy
    log_info "Waiting for services to become healthy..."
    sleep 30

    # Show status
    docker-compose -f "$COMPOSE_FILE" ps

    log_success "Deployment completed!"
    log_info "API Gateway is accessible at: http://$(hostname -I | awk '{print $1}')"
    log_info "Eureka Dashboard: http://$(hostname -I | awk '{print $1}'):8761"
    log_info "Elasticsearch: http://$(hostname -I | awk '{print $1}'):9200"
}

# Start services
start_services() {
    check_docker
    docker-compose -f "$COMPOSE_FILE" start
    log_success "All services started"
}

# Stop services
stop_services() {
    check_docker
    docker-compose -f "$COMPOSE_FILE" stop
    log_success "All services stopped"
}

# Restart services
restart_services() {
    check_docker
    docker-compose -f "$COMPOSE_FILE" restart
    log_success "All services restarted"
}

# Show status
show_status() {
    check_docker
    docker-compose -f "$COMPOSE_FILE" ps
}

# Show logs
show_logs() {
    check_docker
    if [[ -n "$1" ]]; then
        docker-compose -f "$COMPOSE_FILE" logs -f "$1"
    else
        docker-compose -f "$COMPOSE_FILE" logs -f
    fi
}

# Clean up everything
clean() {
    log_warning "This will remove ALL containers, volumes, and images!"
    read -p "Are you sure? (yes/no): " -r
    if [[ $REPLY =~ ^yes$ ]]; then
        check_docker
        docker-compose -f "$COMPOSE_FILE" down -v --rmi all
        log_success "Cleanup completed"
    else
        log_info "Cleanup cancelled"
    fi
}

# Backup databases
backup() {
    log_info "Creating backup directory: $BACKUP_DIR"
    mkdir -p "$BACKUP_DIR"

    log_info "Backing up PostgreSQL databases..."
    docker-compose -f "$COMPOSE_FILE" exec -T postgres pg_dumpall -U postgres > "$BACKUP_DIR/db_backup_$TIMESTAMP.sql"

    # Compress backup
    gzip "$BACKUP_DIR/db_backup_$TIMESTAMP.sql"

    log_success "Backup created: $BACKUP_DIR/db_backup_$TIMESTAMP.sql.gz"
}

# Restore databases
restore() {
    if [[ -z "$1" ]]; then
        log_error "Usage: $0 restore <backup_file.sql.gz>"
        exit 1
    fi

    if [[ ! -f "$1" ]]; then
        log_error "Backup file not found: $1"
        exit 1
    fi

    log_warning "This will overwrite all databases!"
    read -p "Are you sure? (yes/no): " -r
    if [[ $REPLY =~ ^yes$ ]]; then
        log_info "Restoring from backup: $1"
        gunzip -c "$1" | docker-compose -f "$COMPOSE_FILE" exec -T postgres psql -U postgres
        log_success "Restore completed"
    else
        log_info "Restore cancelled"
    fi
}

# Update application (pull new images, rebuild, restart)
update_app() {
    log_info "Updating application..."

    check_docker
    setup_env

    # Pull latest changes (if using git)
    if [[ -d ".git" ]]; then
        log_info "Pulling latest code from git..."
        git pull
    fi

    # Rebuild services
    log_info "Rebuilding services..."
    docker-compose -f "$COMPOSE_FILE" build

    # Restart services
    log_info "Restarting services..."
    docker-compose -f "$COMPOSE_FILE" down
    docker-compose -f "$COMPOSE_FILE" up -d

    # Wait and check health
    sleep 30
    docker-compose -f "$COMPOSE_FILE" ps

    log_success "Update completed!"
}

# Show service logs (tail)
tail_logs() {
    check_docker
    docker-compose -f "$COMPOSE_FILE" logs -f --tail=100
}

# Main menu
show_help() {
    cat << EOF
${GREEN}ShopFast E-Commerce - Deployment Script${NC}

Usage: ./deploy.sh [OPTION]

Options:
  install    Install Docker & Docker Compose (first-time setup)
  deploy     Deploy/update the application (default)
  start      Start all services
  stop       Stop all services
  restart    Restart all services
  status     Show service status
  logs       Show logs for all services (follow)
  logs [svc] Show logs for specific service (e.g., logs api-gateway)
  tail       Show last 100 log lines and follow
  clean      Stop and remove all containers, volumes, and images
  backup     Backup PostgreSQL databases
  restore    Restore databases from backup (requires backup file argument)
  update     Update application from git and redeploy
  help       Show this help message

Examples:
  ./deploy.sh install              # Install Docker (first time)
  ./deploy.sh                      # Deploy the application
  ./deploy.sh logs api-gateway     # View API Gateway logs
  ./deploy.sh backup               # Create database backup
  ./deploy.sh restore backup.sql.gz # Restore from backup

${YELLOW}Note:${NC} This script must be run as root (use sudo)

EOF
}

# Main execution
main() {
    local action="${1:-deploy}"

    case "$action" in
        install)
            check_root
            install_docker
            ;;
        deploy)
            check_root
            deploy
            ;;
        start)
            check_root
            start_services
            ;;
        stop)
            check_root
            stop_services
            ;;
        restart)
            check_root
            restart_services
            ;;
        status)
            check_root
            show_status
            ;;
        logs)
            check_root
            shift
            show_logs "$@"
            ;;
        tail)
            check_root
            tail_logs
            ;;
        clean)
            check_root
            clean
            ;;
        backup)
            check_root
            backup
            ;;
        restore)
            check_root
            shift
            restore "$@"
            ;;
        update)
            check_root
            update_app
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            log_error "Unknown option: $action"
            show_help
            exit 1
            ;;
    esac
}

main "$@"
