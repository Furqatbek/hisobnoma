#!/bin/bash
# =================================================
# Database Restore Script for Hisobnoma Platform
# =================================================

set -euo pipefail

# Configuration
BACKUP_DIR="${BACKUP_DIR:-/backups}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-hisobnoma}"
DB_USER="${DB_USER:-hisobnoma}"
LOG_FILE="${BACKUP_DIR}/restore.log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Logging function
log() {
    local level=$1
    shift
    local message="$@"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo -e "${timestamp} [${level}] ${message}" | tee -a "${LOG_FILE}"
}

log_info() { log "INFO" "$@"; }
log_warn() { log "WARN" "${YELLOW}$@${NC}"; }
log_error() { log "ERROR" "${RED}$@${NC}"; }
log_success() { log "INFO" "${GREEN}$@${NC}"; }

# Show usage
usage() {
    echo -e "${CYAN}Usage: $0 [OPTIONS]${NC}"
    echo ""
    echo "Options:"
    echo "  -f, --file <path>     Backup file to restore (required if not using --latest)"
    echo "  -l, --latest          Restore from latest backup"
    echo "  -d, --database <name> Target database name (default: ${DB_NAME})"
    echo "  -h, --host <host>     Database host (default: ${DB_HOST})"
    echo "  -p, --port <port>     Database port (default: ${DB_PORT})"
    echo "  -u, --user <user>     Database user (default: ${DB_USER})"
    echo "  --drop-existing       Drop existing database before restore"
    echo "  --dry-run             Show what would be done without executing"
    echo "  --help                Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 --latest"
    echo "  $0 -f /backups/hisobnoma_20240115_120000.sql.gz"
    echo "  $0 --latest --drop-existing"
    exit 1
}

# Parse arguments
BACKUP_FILE=""
USE_LATEST=false
DROP_EXISTING=false
DRY_RUN=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -f|--file)
            BACKUP_FILE="$2"
            shift 2
            ;;
        -l|--latest)
            USE_LATEST=true
            shift
            ;;
        -d|--database)
            DB_NAME="$2"
            shift 2
            ;;
        -h|--host)
            DB_HOST="$2"
            shift 2
            ;;
        -p|--port)
            DB_PORT="$2"
            shift 2
            ;;
        -u|--user)
            DB_USER="$2"
            shift 2
            ;;
        --drop-existing)
            DROP_EXISTING=true
            shift
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --help)
            usage
            ;;
        *)
            log_error "Unknown option: $1"
            usage
            ;;
    esac
done

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."

    if ! command -v psql &> /dev/null; then
        log_error "psql is not installed"
        exit 1
    fi

    if ! command -v gunzip &> /dev/null; then
        log_error "gunzip is not installed"
        exit 1
    fi
}

# Resolve backup file
resolve_backup_file() {
    if [ "$USE_LATEST" = true ]; then
        if [ -L "${BACKUP_DIR}/latest.sql.gz" ]; then
            BACKUP_FILE=$(readlink -f "${BACKUP_DIR}/latest.sql.gz")
            log_info "Using latest backup: ${BACKUP_FILE}"
        else
            # Find most recent backup
            BACKUP_FILE=$(ls -t ${BACKUP_DIR}/hisobnoma_*.sql.gz 2>/dev/null | head -1)
            if [ -z "${BACKUP_FILE}" ]; then
                log_error "No backup files found in ${BACKUP_DIR}"
                exit 1
            fi
            log_info "Using most recent backup: ${BACKUP_FILE}"
        fi
    fi

    if [ -z "${BACKUP_FILE}" ]; then
        log_error "No backup file specified. Use --file or --latest"
        usage
    fi

    if [ ! -f "${BACKUP_FILE}" ]; then
        log_error "Backup file not found: ${BACKUP_FILE}"
        exit 1
    fi
}

# Verify backup file
verify_backup() {
    log_info "Verifying backup file integrity..."

    if gzip -t "${BACKUP_FILE}" 2>/dev/null; then
        log_success "Backup file is valid"
    else
        log_error "Backup file is corrupted: ${BACKUP_FILE}"
        exit 1
    fi

    local file_size=$(du -h "${BACKUP_FILE}" | cut -f1)
    log_info "Backup file size: ${file_size}"
}

# Confirm restore
confirm_restore() {
    echo ""
    echo -e "${YELLOW}=========================================${NC}"
    echo -e "${YELLOW}WARNING: Database Restore${NC}"
    echo -e "${YELLOW}=========================================${NC}"
    echo ""
    echo "This will restore data to:"
    echo "  Database: ${DB_NAME}"
    echo "  Host:     ${DB_HOST}:${DB_PORT}"
    echo "  User:     ${DB_USER}"
    echo "  From:     ${BACKUP_FILE}"
    echo ""

    if [ "$DROP_EXISTING" = true ]; then
        echo -e "${RED}WARNING: Existing database will be DROPPED!${NC}"
        echo ""
    fi

    if [ "$DRY_RUN" = true ]; then
        echo -e "${CYAN}DRY RUN - No changes will be made${NC}"
        return 0
    fi

    read -p "Are you sure you want to continue? (yes/no): " confirm
    if [ "$confirm" != "yes" ]; then
        log_info "Restore cancelled by user"
        exit 0
    fi
}

# Stop application (optional)
stop_application() {
    if command -v docker &> /dev/null; then
        log_info "Stopping application container..."
        docker stop hisobnoma-app 2>/dev/null || true
    fi
}

# Start application (optional)
start_application() {
    if command -v docker &> /dev/null; then
        log_info "Starting application container..."
        docker start hisobnoma-app 2>/dev/null || true
    fi
}

# Drop and recreate database
drop_and_recreate_db() {
    if [ "$DROP_EXISTING" = true ]; then
        log_info "Dropping existing database..."

        if [ "$DRY_RUN" = true ]; then
            log_info "[DRY RUN] Would drop database: ${DB_NAME}"
            return 0
        fi

        # Terminate existing connections
        PGPASSWORD="${DB_PASSWORD}" psql \
            -h "${DB_HOST}" \
            -p "${DB_PORT}" \
            -U "${DB_USER}" \
            -d postgres \
            -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '${DB_NAME}' AND pid <> pg_backend_pid();" \
            2>/dev/null || true

        # Drop database
        PGPASSWORD="${DB_PASSWORD}" psql \
            -h "${DB_HOST}" \
            -p "${DB_PORT}" \
            -U "${DB_USER}" \
            -d postgres \
            -c "DROP DATABASE IF EXISTS ${DB_NAME};"

        # Recreate database
        PGPASSWORD="${DB_PASSWORD}" psql \
            -h "${DB_HOST}" \
            -p "${DB_PORT}" \
            -U "${DB_USER}" \
            -d postgres \
            -c "CREATE DATABASE ${DB_NAME} OWNER ${DB_USER};"

        log_success "Database recreated successfully"
    fi
}

# Perform restore
perform_restore() {
    log_info "Starting database restore..."

    if [ "$DRY_RUN" = true ]; then
        log_info "[DRY RUN] Would restore from: ${BACKUP_FILE}"
        log_info "[DRY RUN] To database: ${DB_NAME}@${DB_HOST}:${DB_PORT}"
        return 0
    fi

    local start_time=$(date +%s)

    # Restore from backup
    gunzip -c "${BACKUP_FILE}" | PGPASSWORD="${DB_PASSWORD}" psql \
        -h "${DB_HOST}" \
        -p "${DB_PORT}" \
        -U "${DB_USER}" \
        -d "${DB_NAME}" \
        --set ON_ERROR_STOP=off \
        2>&1 | tee -a "${LOG_FILE}"

    local exit_code=${PIPESTATUS[1]}
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))

    if [ $exit_code -eq 0 ]; then
        log_success "Restore completed successfully in ${duration}s"
    else
        log_warn "Restore completed with some errors (exit code: ${exit_code})"
        log_info "Check the log for details: ${LOG_FILE}"
    fi
}

# Verify restore
verify_restore() {
    log_info "Verifying restore..."

    if [ "$DRY_RUN" = true ]; then
        log_info "[DRY RUN] Would verify restore"
        return 0
    fi

    # Check table count
    local table_count=$(PGPASSWORD="${DB_PASSWORD}" psql \
        -h "${DB_HOST}" \
        -p "${DB_PORT}" \
        -U "${DB_USER}" \
        -d "${DB_NAME}" \
        -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';")

    log_info "Tables in database: ${table_count}"

    # Check some key tables
    local key_tables=("users" "tenants" "products" "locations")
    for table in "${key_tables[@]}"; do
        local count=$(PGPASSWORD="${DB_PASSWORD}" psql \
            -h "${DB_HOST}" \
            -p "${DB_PORT}" \
            -U "${DB_USER}" \
            -d "${DB_NAME}" \
            -t -c "SELECT COUNT(*) FROM ${table};" 2>/dev/null || echo "N/A")
        log_info "  - ${table}: ${count} rows"
    done
}

# Main execution
main() {
    log_info "========================================="
    log_info "Hisobnoma Database Restore"
    log_info "========================================="

    check_prerequisites
    resolve_backup_file
    verify_backup
    confirm_restore

    if [ "$DRY_RUN" = false ]; then
        stop_application
    fi

    drop_and_recreate_db
    perform_restore
    verify_restore

    if [ "$DRY_RUN" = false ]; then
        start_application
    fi

    log_success "========================================="
    log_success "Restore process completed!"
    log_success "========================================="
}

# Handle errors
trap 'log_error "Restore failed! Check the log at ${LOG_FILE}"; start_application' ERR

# Run main
main "$@"
