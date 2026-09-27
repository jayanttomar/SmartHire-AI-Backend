# SmartHire AI Backend

Java Spring Boot backend for SmartHire AI, an AI-powered hiring platform with candidate/recruiter workflows, JWT authentication, Google OAuth, resume parsing, and Claude-powered matching.

## Tech Stack

- Java 17
- Spring Boot 3
- Spring Web
- Spring Data JPA + Hibernate
- PostgreSQL
- Spring Security + JWT
- Google OAuth2
- Apache PDFBox
- Claude API

## Local Setup

1. Create a PostgreSQL database named `smarthire_ai`:

```sql
CREATE DATABASE smarthire_ai;
```

2. Copy `.env.example` values into your local environment and set `DB_PASSWORD` to the password chosen during PostgreSQL installation.
3. Run the app with Maven:

```powershell
mvn spring-boot:run
```

Health check:

```text
GET http://localhost:8080/api/health
```

## Google sign-in locally

Run the frontend at `http://localhost:5173` and keep `FRONTEND_URL` set to that origin.
Configure the Google web client with the authorized redirect URI
`http://localhost:8080/login/oauth2/code/google` and set both `GOOGLE_CLIENT_ID` and
`GOOGLE_CLIENT_SECRET`. Start sign-in at `/oauth2/authorization/google`; new accounts
use the selected candidate/recruiter role, while existing accounts keep their role.
The frontend exchanges a single-use code for its session. Google cancellation or
failure returns to the frontend with a readable error.

## Verification

Run `mvn test`. Workflow tests use an isolated H2 database, generated test PDFs,
mock outgoing email, and local matching; they do not modify the local PostgreSQL
database or send mail. They cover registration, login, password reset, Google
account linking and callback handling, profiles, jobs, resumes, applications,
ownership checks, and matching. Real Google account consent and delivered email
still require external end-to-end checks.

## Production deployment

Build the deployment artifact with `mvn clean package` and run it with Java 17:

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
java -jar target/smarthire-ai-backend-0.0.1-SNAPSHOT.jar
```

Set these environment variables in the hosting provider (never commit them): `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (unique, 32+ characters), `FRONTEND_URL` (your HTTPS frontend URL), `UPLOAD_DIR`, and SMTP settings. Set both Google variables together if Google login is enabled. The `prod` profile disables SQL/debug output, blocks localhost CORS origins, uses Hibernate schema validation instead of schema mutation, and refuses unsafe/missing production configuration at startup. Run the health endpoint after deploy and configure the host's health check to use `/api/health`.

## Password reset email

Configure an SMTP account in `.env` (for Gmail, use a Google App Password):

```text
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_gmail_app_password
```

The login page should call `POST /api/auth/forgot-password` with `{"email":"user@example.com"}`. The email contains a 30-minute, single-use link to `FRONTEND_URL/reset-password?token=...`. Submit the token and new password to `POST /api/auth/reset-password` with `{"token":"...","password":"new-password"}`.

## API coverage

- Auth: register, login, and current-user endpoint (`/api/auth/me`)
- Candidate/recruiter profile management
- Public active-job listing and details; recruiter job create, update, and deactivation
- Candidate PDF resume upload, text extraction, stored analysis, and `/api/resumes/me`
- Candidate applications with a selected resume and optional cover letter; withdrawal and live status tracking
- Recruiter applicant inbox, role-scoped pipeline, secure candidate/resume review, match recalculation, and status changes
- Trustworthy match lifecycle (`PENDING`, `COMPLETED`, `INSUFFICIENT_DATA`, `FAILED`) so incomplete job posts never appear as a misleading 0% match

Set `ANTHROPIC_API_KEY` to enable Claude resume analysis and match reasoning. Without it, uploads and applications still complete with a safe local fallback result.
