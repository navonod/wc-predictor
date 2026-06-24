#!/bin/bash
# Update Matchday 3 times to official FIFA 2026 schedule
# UTC kickoff times confirmed June 2026

DB="data/wcpredictor.db"

sqlite3 "$DB" <<'SQL'
UPDATE matches SET match_date = 1782349200000 WHERE group_letter = 'A' AND round = 'GROUP_MD3';
UPDATE matches SET match_date = 1782327600000 WHERE group_letter = 'B' AND round = 'GROUP_MD3';
UPDATE matches SET match_date = 1782338400000 WHERE group_letter = 'C' AND round = 'GROUP_MD3';
UPDATE matches SET match_date = 1782428400000 WHERE group_letter = 'D' AND round = 'GROUP_MD3';
UPDATE matches SET match_date = 1782406800000 WHERE group_letter = 'E' AND round = 'GROUP_MD3';
UPDATE matches SET match_date = 1782417600000 WHERE group_letter = 'F' AND round = 'GROUP_MD3';
UPDATE matches SET match_date = 1782518400000 WHERE group_letter = 'G' AND round = 'GROUP_MD3';
UPDATE matches SET match_date = 1782507600000 WHERE group_letter = 'H' AND round = 'GROUP_MD3';
UPDATE matches SET match_date = 1782489600000 WHERE group_letter = 'I' AND round = 'GROUP_MD3';
UPDATE matches SET match_date = 1782601200000 WHERE group_letter = 'J' AND round = 'GROUP_MD3';
UPDATE matches SET match_date = 1782592200000 WHERE group_letter = 'K' AND round = 'GROUP_MD3';
UPDATE matches SET match_date = 1782583200000 WHERE group_letter = 'L' AND round = 'GROUP_MD3';
SQL

echo "--- Updated Matchday 3 times ---"
sqlite3 "$DB" "SELECT group_letter, match_number, datetime(match_date/1000, 'unixepoch') || ' UTC' FROM matches WHERE round = 'GROUP_MD3' ORDER BY group_letter, match_number;"
