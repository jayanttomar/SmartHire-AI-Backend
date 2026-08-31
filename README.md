# SmartHire AI Backend

Java Spring Boot backend for SmartHire AI, an AI-powered hiring platform with candidate/recruiter workflows, JWT authentication, Google OAuth, resume parsing, and Claude-powered matching.

## Tech Stack

- Java 17
- Spring Boot 3
- Spring Web
- Spring Data JPA + Hibernate
- MySQL
- Spring Security + JWT
- Google OAuth2
- Apache PDFBox
- Claude API

## Local Setup

1. Create a MySQL database named `smarthire_ai`, or let the configured JDBC URL create it automatically.
2. Set environment variables for real secrets before production use.
3. Run the app with Maven:

```powershell
mvn spring-boot:run
```

Health check:

```text
GET http://localhost:8080/api/health
```
