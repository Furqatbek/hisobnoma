#!/bin/bash
# =================================================
# Database Backup Script for Hisobnoma Platform
# =================================================

set -euo pipefail

# Configuration
BACKUP_DIR="${BACKUP_DIR:-/backups}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-hisobnoma}"
DB_USER="${DB_USER:-hisobnoma}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="hisobnoma_${TIMESTAMP}.sql.gz"
LOG_FILE="${BACKUP_DIR}/backup.log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
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

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."

    if ! command -v pg_dump &> /dev/null; then
        log_error "pg_dump is not installed"
        exit 1
    fi

    if ! command -v gzip &> /dev/null; then
        log_error "gzip is not installed"
        exit 1
    fi

    # Create backup directory if it doesn't exist
    if [ ! -d "${BACKUP_DIR}" ]; then
        mkdir -p "${BACKUP_DIR}"
        log_info "Created backup directory: ${BACKUP_DIR}"
    fi
}

# Perform database backup
perform_backup() {
    log_info "Starting database backup..."
    log_info "Database: ${DB_NAME}@${DB_HOST}:${DB_PORT}"
    log_info "Backup file: ${BACKUP_FILE}"

    local start_time=$(date +%s)

    # Perform backup with pg_dump
    PGPASSWORD="${DB_PASSWORD}" pg_dump \
        -h "${DB_HOST}" \
        -p "${DB_PORT}" \
        -U "${DB_USER}" \
        -d "${DB_NAME}" \
        --format=plain \
        --no-owner \
        --no-privileges \
        --verbose \
        2>> "${LOG_FILE}" | gzip > "${BACKUP_DIR}/${BACKUP_FILE}"

    local exit_code=$?
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))

    if [ $exit_code -eq 0 ]; then
        local file_size=$(du -h "${BACKUP_DIR}/${BACKUP_FILE}" | cut -f1)
        log_success "Backup completed successfully in ${duration}s"
        log_info "File size: ${file_size}"

        # Create latest symlink
        ln -sf "${BACKUP_FILE}" "${BACKUP_DIR}/latest.sql.gz"
        log_info "Updated latest.sql.gz symlink"
    else
        log_error "Backup failed with exit code: ${exit_code}"
        rm -f "${BACKUP_DIR}/${BACKUP_FILE}"
        exit 1
    fi
}

# Verify backup integrity
verify_backup() {
    log_info "Verifying backup integrity..."

    if gzip -t "${BACKUP_DIR}/${BACKUP_FILE}" 2>/dev/null; then
        log_success "Backup file is valid"
    else
        log_error "Backup file is corrupted!"
        exit 1
    fi

    # Check backup contains data
    local line_count=$(zcat "${BACKUP_DIR}/${BACKUP_FILE}" | wc -l)
    if [ $line_count -gt 100 ]; then
        log_info "Backup contains ${line_count} lines"
    else
        log_warn "Backup seems too small (${line_count} lines)"
    fi
}

# Clean old backups
cleanup_old_backups() {
    log_info "Cleaning up backups older than ${RETENTION_DAYS} days..."

    local deleted_count=0
    while IFS= read -r -d '' file; do
        rm -f "$file"
        log_info "Deleted: $(basename "$file")"
        ((deleted_count++))
    done < <(find "${BACKUP_DIR}" -name "hisobnoma_*.sql.gz" -type f -mtime +${RETENTION_DAYS} -print0)

    log_info "Deleted ${deleted_count} old backup(s)"
}

# Upload to S3 (optional)
upload_to_s3() {
    if [ -n "${S3_BUCKET:-}" ] && command -v aws &> /dev/null; then
        log_info "Uploading backup to S3: ${S3_BUCKET}"

        aws s3 cp "${BACKUP_DIR}/${BACKUP_FILE}" "s3://${S3_BUCKET}/backups/${BACKUP_FILE}" \
            --storage-class STANDARD_IA

        if [ $? -eq 0 ]; then
            log_success "Backup uploaded to S3 successfully"
        else
            log_warn "Failed to upload backup to S3"
        fi
    fi
}

# Send notification
send_notification() {
    local status=$1
    local message=$2

    # Slack notification
    if [ -n "${SLACK_WEBHOOK_URL:-}" ]; then
        local color="good"
        [ "$status" != "success" ] && color="danger"

        curl -s -X POST "${SLACK_WEBHOOK_URL}" \
            -H 'Content-type: application/json' \
            -d "{
                \"attachments\": [{
                    \"color\": \"${color}\",
                    \"title\": \"Database Backup - ${status^^}\",
                    \"text\": \"${message}\",
                    \"fields\": [
                        {\"title\": \"Database\", \"value\": \"${DB_NAME}\", \"short\": true},
                        {\"title\": \"File\", \"value\": \"${BACKUP_FILE}\", \"short\": true}
                    ],
                    \"footer\": \"Hisobnoma Backup System\",
                    \"ts\": $(date +%s)
                }]
            }" > /dev/null
    fi
}

# Main execution
main() {
    log_info "========================================="
    log_info "Hisobnoma Database Backup"
    log_info "========================================="

    check_prerequisites
    perform_backup
    verify_backup
    cleanup_old_backups
    upload_to_s3

    log_success "========================================="
    log_success "Backup process completed successfully!"
    log_success "========================================="

    send_notification "success" "Database backup completed successfully"
}

# Handle errors
trap 'log_error "Backup failed! Check the log at ${LOG_FILE}"; send_notification "failed" "Database backup failed"' ERR

# Run main
main "$@"
