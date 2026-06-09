#!/bin/bash
# WC Predictor database backup script
# Usage: ./backup-db.sh
# Requires: rclone configured with a 'gdrive' remote

set -e

TIMESTAMP=$(date +%Y%m%d_%H%M)
BACKUP_DIR=/home/wcpredictor/backups
DB_BACKUP="$BACKUP_DIR/wcpredictor_${TIMESTAMP}.db"
RCLONE_REMOTE="gdrive:wc-predictor-backups"

mkdir -p "$BACKUP_DIR"

echo "Backing up database..."
docker compose cp wcpredictor:/app/data/wcpredictor.db "$DB_BACKUP" 2>/dev/null || {
    SRC=$(docker volume inspect wc-predictor_wcdata --format '{{ .Mountpoint }}' 2>/dev/null)
    if [ -n "$SRC" ] && [ -f "$SRC/wcpredictor.db" ]; then
        cp "$SRC/wcpredictor.db" "$DB_BACKUP"
    else
        echo "ERROR: Cannot locate database file"
        exit 1
    fi
}

echo "Backup saved: $DB_BACKUP ($(du -h "$DB_BACKUP" | cut -f1))"

ls -t "$BACKUP_DIR"/wcpredictor_*.db 2>/dev/null | tail -n +31 | xargs -r rm

if command -v rclone &> /dev/null && rclone listremotes 2>/dev/null | grep -q "^gdrive:"; then
    echo "Syncing to Google Drive..."
    rclone copy "$BACKUP_DIR" "$RCLONE_REMOTE" --include "wcpredictor_*.db"
    echo "Sync complete."
else
    echo "rclone not configured. Run 'rclone config' to set up Google Drive."
    echo "  Name the remote: gdrive"
    echo "  Create folder in Google Drive: wc-predictor-backups"
fi

echo "Done."
