# Feature verification — 2026-09-26

## Fixed

- Anonymous access to recruiter job listings now returns 401 instead of a server error.
- The frontend uses localhost for the local Google OAuth callback, matching the configured Google redirect.
- Google callback exchange runs once in React StrictMode; failed/cancelled sign-in returns a readable message.
- Google login requires a verified email, clears the temporary OAuth session, and preserves existing account roles.
- Google client setup no longer performs discovery over the network during backend startup.
- Password-reset links open a reset form with confirmation, error handling, and return to sign-in after success.
- Incorrect passwords display an invalid-credentials message; API validation errors have readable messages.
- Malformed browser session data no longer crashes the sign-in screen.
- Invalid PDFs are parsed before storage; invalid filenames and excessive extracted text return 400 errors.
- SMTP and AI requests have timeouts. Placeholder AI keys immediately use local matching.
- Local skill matching distinguishes Java from JavaScript and does not detect AI inside unrelated words.
- Spring web debug logging is disabled to avoid logging authentication request/response contents.

## Checked

- Backend automated tests: isolated H2 database; registration/login, duplicate/invalid registration, invalid JWTs, CORS,
  reset and token reuse, Google account role/linking, verified-email handling, one-time exchange, profile persistence,
  jobs, resume generation/PDF upload/download, application submission, duplicate applications, recruiter inbox,
  shortlist notification invocation, withdrawal, job deactivation, cross-account permissions, and local matching.
- Headless Edge: real wrong-password and invalid-reset requests, reset success screen with a mocked API response,
  OAuth failure display, real Google button navigation, mocked successful OAuth callback under StrictMode,
  dashboard render, logout, and malformed local-storage recovery. No browser runtime errors.
- Frontend production build passes. Lint has only three pre-existing unused legacy component warnings in Dashboard.jsx.
- Real Google authorization request reaches Google's sign-in page without redirect/client error markers.
- SMTP authentication succeeds. No email was sent during verification; delivery was not tested.

## External checks still needed

- Complete Google sign-in with a real account to verify consent and Google's token exchange. Automated callback
  tests do not prove the configured client secret or an individual account's consent settings.
- `ANTHROPIC_API_KEY` is currently a placeholder. Resume generation and local matching work; Claude-backed
  analysis requires a valid key and has not been verified.
- Password-reset and shortlist delivery were tested with mocked mail; SMTP authentication alone does not prove delivery.

Local app: `http://localhost:5173`. Backend health: `http://localhost:8080/api/health`.
