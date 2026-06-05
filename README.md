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
