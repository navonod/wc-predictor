#!/bin/bash
# Fix knockout matches that have no tournament_id assigned
# This is a one-time migration for existing data.
# The DataLoader now sets tournament_id on all matches going forward.

DB="data/wcpredictor.db"

MISSING=$(sqlite3 "$DB" "SELECT COUNT(*) FROM matches WHERE match_number >= 73 AND tournament_id IS NULL;")
echo "Knockout matches without tournament_id: $MISSING"

if [ "$MISSING" -gt 0 ]; then
    sqlite3 "$DB" "UPDATE matches SET tournament_id = (SELECT id FROM tournaments LIMIT 1) WHERE match_number >= 73 AND tournament_id IS NULL;"
    echo "Fixed $MISSING matches."
fi
