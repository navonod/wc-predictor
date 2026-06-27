#!/bin/bash
# Update Matchday 3 times to official FIFA 2026 schedule
# Match numbers aligned with production DB, times from official fixture list

DB="data/wcpredictor.db"

sqlite3 "$DB" <<'SQL'
-- June 24
UPDATE matches SET match_date = 1782327600000 WHERE match_number = 51; -- Gr B  19:00 UTC
UPDATE matches SET match_date = 1782327600000 WHERE match_number = 52;
UPDATE matches SET match_date = 1782338400000 WHERE match_number = 49; -- Gr C  22:00 UTC
UPDATE matches SET match_date = 1782338400000 WHERE match_number = 50;

-- June 25
UPDATE matches SET match_date = 1782349200000 WHERE match_number = 53; -- Gr A  01:00 UTC
UPDATE matches SET match_date = 1782349200000 WHERE match_number = 54;
UPDATE matches SET match_date = 1782406800000 WHERE match_number = 55; -- Gr E  17:00 UTC
UPDATE matches SET match_date = 1782406800000 WHERE match_number = 56;
UPDATE matches SET match_date = 1782417600000 WHERE match_number = 57; -- Gr F  20:00 UTC
UPDATE matches SET match_date = 1782417600000 WHERE match_number = 58;
UPDATE matches SET match_date = 1782428400000 WHERE match_number = 59; -- Gr D  23:00 UTC
UPDATE matches SET match_date = 1782428400000 WHERE match_number = 60;

-- June 26
UPDATE matches SET match_date = 1782489600000 WHERE match_number = 61; -- Gr I  16:00 UTC
UPDATE matches SET match_date = 1782489600000 WHERE match_number = 62;
UPDATE matches SET match_date = 1782507600000 WHERE match_number = 65; -- Gr H  21:00 UTC
UPDATE matches SET match_date = 1782507600000 WHERE match_number = 66;

-- June 27
UPDATE matches SET match_date = 1782518400000 WHERE match_number = 63; -- Gr G  00:00 UTC
UPDATE matches SET match_date = 1782518400000 WHERE match_number = 64;
UPDATE matches SET match_date = 1782583200000 WHERE match_number = 67; -- Gr L  18:00 UTC
UPDATE matches SET match_date = 1782583200000 WHERE match_number = 68;
UPDATE matches SET match_date = 1782592200000 WHERE match_number = 71; -- Gr K  20:30 UTC
UPDATE matches SET match_date = 1782592200000 WHERE match_number = 72;
UPDATE matches SET match_date = 1782601200000 WHERE match_number = 69; -- Gr J  23:00 UTC
UPDATE matches SET match_date = 1782601200000 WHERE match_number = 70;
SQL

echo "--- Updated Matchday 3 times ---"
sqlite3 "$DB" "SELECT match_number, group_letter, datetime(match_date/1000, 'unixepoch') || ' UTC' FROM matches WHERE round = 'GROUP_MD3' ORDER BY match_number;"
