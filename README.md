# World Cup Predictor 2026

Spring Boot + Thymeleaf + SQLite prediction game for the 2026 FIFA World Cup.

## Quick Start
```bash
./mvnw spring-boot:run
```
Opens on `http://localhost:8090`. Default admin: `wcpredictor@thatcher.africa` / `password`

## Docker
```bash
docker-compose up -d
```

Database is stored in `wcpredictor.db` (SQLite, auto-created on first run).

## Email Setup (SendGrid)

The app sends account confirmation and password-reset emails. Production uses SendGrid SMTP.

1. **Create a SendGrid account** at [sendgrid.com](https://sendgrid.com)
2. **Verify a single sender** — Settings → Sender Authentication → Verify a Single Sender. Add `predictor@thatcher.africa` (or any address on your domain). Click the verification link in the email SendGrid sends you. No DNS records needed.
3. **Create an API key** — Settings → API Keys → Create API Key. Choose "Restricted Access" and grant only **Mail Send** permission. Copy the key (starts with `SG.`).
4. **Set the key locally:**
   ```bash
   echo "SENDGRID_API_KEY=SG.your-key-here" > .env
   ```
5. **Rebuild and start:**
   ```bash
   docker compose down && docker compose up -d --build
   ```
6. **Test** — visit `/register`, create an account. You should receive a confirmation email.

### How it works
- `application-prod.properties` configures Spring Mail to use `smtp.sendgrid.net:587` with TLS
- The API key is injected into the container via `docker-compose.yml` → `SENDGRID_API_KEY` env var
- Spring's `@Value("${app.base-url}")` in `UserRegistrationService` builds clickable confirmation links

## Tournaments

The app supports multiple tournaments. Each tournament has its own match schedule via a CSV import.

### CSV schedule format

Create a `*-schedule.csv` file with these columns:

```
Match No,Date/Time in UTC,Estimated Time,Team 1,Team 2,Venue,Local Time,Timezone
1,2026-06-11 19:00,FALSE,Mexico,South Africa,Mexico City Stadium,2026-06-11 13:00,UTC-6
...
104,2026-07-19 19:00,FALSE,Winner Match 101,Winner Match 102,MetLife Stadium,2026-07-19 15:00,UTC-4
```

- **Match No** — 1-104 (72 group + 32 knockout)
- **Date/Time in UTC** — format `YYYY-MM-DD HH:MM` in UTC
- **Estimated Time** — `TRUE` if kickoff time is not yet confirmed
- **Team 1 / Team 2** — team names or placeholder (e.g. `Winner Group A`)
- **Venue** — stadium name
- **Local Time / Timezone** — informational, not used by the import

### Adding a new tournament

**Method 1 — Auto-import on first startup**

1. Name your CSV file with underscores for spaces, e.g. `FIFA_World_Cup_2030-schedule.csv`
2. Place it in `src/main/resources/data/`
3. On the next app restart, the DataLoader scans the classpath for `*-schedule.csv` files. For each one, it:
   - Creates a new Tournament entity named from the filename (underscores → spaces)
   - Imports all 104 matches with UTC dates, venues, and round types
   - Skips if the tournament already exists
4. Knockout matches (73-104) are created with `predictionsLocked = true` and null teams for admin to fill later

**Method 2 — Admin UI import**

1. Log in as admin → **Manage Tournaments**
2. Click **Import Schedule** next to any tournament
3. Upload a `*-schedule.csv` file
4. Existing match dates, venues, and estimated flags are overwritten. Missing knockout matches are created.

### Timezone handling

All match dates are stored in UTC. The CSV's `Date/Time in UTC` column is parsed directly — no timezone conversion needed. The `matchDateEstimated` flag is set per-match from the CSV's `Estimated Time` column. Predictions lock at the earliest kickoff time per round type (Matchday 1, 2, 3, or knockout round).

## Backups

Automated daily database backups via `rclone` to Google Drive.

### Setup

1. **Install rclone:**
   ```bash
   sudo apt install rclone
   ```

2. **Configure Google Drive access:**
   ```bash
   rclone config
   ```
   - Name the remote: `gdrive`
   - Type: `drive`
   - Follow the OAuth flow (opens browser to grant access)

3. **Create the backup folder in Google Drive:** `wc-predictor-backups`

4. **Test the backup script:**
   ```bash
   cd ~/wc-predictor
   chmod +x scripts/backup-db.sh
   ./scripts/backup-db.sh
   ```

5. **Schedule daily backups at 3am:**
   ```
   crontab -e
   0 3 * * * /home/wcpredictor/wc-predictor/scripts/backup-db.sh
   ```

### How it works

- `scripts/backup-db.sh` copies the SQLite database from the Docker volume
- Timestamped backups are stored locally in `/home/wcpredictor/backups/` (30-day retention)
- `rclone` syncs the backup directory to Google Drive
- If rclone isn't configured, backups stay local only
