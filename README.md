# AI-Powered Technical Support Assistant

A Java 21 / Spring Boot support desk by **Md Mizanul Haque**. Capture technical issues, generate troubleshooting suggestions, and track tickets from open to resolved in a responsive browser interface.

## Features

- Ticket creation, paginated history, detail view and status updates.
- Automatic rule-based categorization: database, networking, authentication, performance and application.
- Optional LLM integration using a configurable HTTPS chat-completions endpoint.
- Clearly labelled offline checklists when AI is disabled or the provider fails. Offline mode is **not** an LLM.
- MySQL persistence with Flyway migrations; file-backed H2 for a zero-setup local demo.
- Spring Security login, CSRF-protected writes, input validation and optimistic locking.
- Provider timeouts and a two-request analysis concurrency limit.
- Docker Compose setup, automated tests and GitHub Actions CI.

## Quick start (Java 21 + Maven 3.9+)

```bash
git clone https://github.com/MMHaque492/AI-Powered-Technical-Support-Assistant.git
cd AI-Powered-Technical-Support-Assistant
mvn spring-boot:run
```

Open **http://localhost:8080**. Local demo login: **demo / demo-support**.
The default application binds only to `127.0.0.1`. Tickets persist under `data/` between restarts. No API key or MySQL installation is needed for the offline demo.

1. Create a ticket with its title, environment and sanitized description.
2. Select **Analyze issue** to generate a checklist or AI suggestions.
3. Change status to **In progress** or **Resolved** as you investigate.

## Run with MySQL and Docker

Copy `.env.example` to `.env` and replace the three password values. Docker Compose reads this file automatically.

```bash
docker compose up --build -d
docker compose logs -f app
```

Open http://localhost:8080 and sign in with `APP_USERNAME` / `APP_PASSWORD` from `.env`. MySQL is internal to the Compose network and persists in a named volume. Stop with `docker compose down`; avoid `down -v` unless you intend to delete database data.

For an existing MySQL server, create the `support_assistant` database and a dedicated user with schema migration privileges. Set `SPRING_PROFILES_ACTIVE=mysql`, `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` in your shell or IDE, then run Maven. Flyway creates the tables; Hibernate validates them.

## Enable LLM suggestions

Use a provider supporting the chat-completions request/response contract. Configure these server-side variables:

| Variable | Value |
| --- | --- |
| `LLM_ENABLED` | `true` |
| `LLM_ENDPOINT` | Full HTTPS URL of the provider's chat-completions endpoint |
| `LLM_API_KEY` | Your provider API key |
| `LLM_MODEL` | A model supported by your account |

The adapter sends `model`, `messages` and `max_tokens`, and reads `choices[0].message.content`. Providers with a different API need an adapter change. Native Maven runs do **not** automatically load `.env`: set environment variables in the shell or IDE. Restart after configuration changes.

Only selecting **Analyze issue** sends the ticket title, description and environment to the configured provider. Keep credentials and personal data out of tickets. API usage may incur provider charges. Provider errors/timeouts produce `OFFLINE_FALLBACK`; the application never exposes raw provider errors or API keys to the browser. Suggestions are displayed as plain text and never executed.

## REST API

All endpoints require authentication. Use the browser session or HTTP Basic. Writes also require the session's CSRF token.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/api/session` | CSRF token/header name and LLM mode |
| POST | `/api/tickets` | Create ticket |
| GET | `/api/tickets?page=0` | List tickets, 20 per page |
| GET | `/api/tickets/{id}` | Ticket details |
| POST | `/api/tickets/{id}/analyze` | Generate and persist troubleshooting |
| PATCH | `/api/tickets/{id}/status` | Set `OPEN`, `IN_PROGRESS` or `RESOLVED` |

Example create body:

```json
{
  "title": "MySQL connection refused",
  "description": "Spring Boot cannot connect to localhost:3306. The database service may be stopped.",
  "environment": "Java 21, Spring Boot, Windows 11"
}
```

For Postman: use Basic Auth, GET `/api/session`, retain the session cookie, then send its `token` in the header named by `headerName` (usually `X-CSRF-TOKEN`) for POST/PATCH. Status update body: `{"status":"RESOLVED"}`.

## Architecture

The static HTML/CSS/JavaScript UI is served by Spring Boot on the same origin. Controller DTOs validate input; the service handles ticket workflows; Spring Data JPA persists entities. Analysis uses the LLM adapter when enabled, with deterministic troubleshooting as the fallback. Flyway owns the database schema.

```text
src/main/java/com/mizan/support/   Controllers, service, entity, security and LLM adapter
src/main/resources/static/        Browser UI
src/main/resources/db/migration/  Versioned schema
src/test/java/com/mizan/support/  API and service tests
```

## Tests

```bash
mvn verify
```

Tests cover the ticket lifecycle, authentication/CSRF, invalid requests, missing tickets and successful/failed provider behavior through mocks. H2 is used for integration tests; a real provider and Docker/MySQL require separate environment testing. CI runs the same Maven verification.

## Scope and deployment notes

This is a single-operator portfolio application, not a multi-tenant help desk. All authenticated access uses one configured support account. It does not include registration, per-user ownership, attachments, email notifications or a vector database. The concurrency limit is not a daily API budget or distributed rate limiter.

Before exposing it publicly, change demo credentials, deploy behind HTTPS, configure secure cookies (`SERVER_SERVLET_SESSION_COOKIE_SECURE=true`), add gateway rate limits and monitoring, review dependency updates, and use managed secrets/backups. Add per-user authorization before supporting multiple independent customers. AI diagnoses are suggestions and can be wrong.

## Author

[Md Mizanul Haque](https://github.com/MMHaque492) · B.E. Computer Science & Engineering, Chandigarh University
