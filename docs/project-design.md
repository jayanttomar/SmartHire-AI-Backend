# SmartHire AI - Project Design

SmartHire AI is an AI-powered hiring platform with two user roles: Candidate and Recruiter. Candidates upload resumes and apply to jobs. Recruiters post jobs, review applications, and see AI-generated resume insights plus match scores.

## 1. High-Level System Flow

```mermaid
flowchart TD
    A[User Opens SmartHire AI] --> B{User Type}

    B -->|Candidate| C[Candidate Register/Login]
    B -->|Recruiter| D[Recruiter Register/Login]

    C --> E[Candidate Dashboard]
    D --> F[Recruiter Dashboard]

    E --> G[Create/Update Profile]
    E --> H[Upload Resume PDF]
    H --> I[Extract Resume Text]
    I --> J[Claude API Resume Analyzer]
    J --> K[Save Skills, Summary, Experience]

    E --> L[Search Jobs]
    L --> M[View Job Details]
    M --> N[Apply to Job]
    N --> O[Create Application]
    O --> P[Claude API Match Score]
    P --> Q[Save Match Percentage and Reasoning]
    Q --> R[Candidate Tracks Status]

    F --> S[Post Job]
    F --> T[Manage Posted Jobs]
    F --> U[View Applications]
    U --> V[Open Candidate Profile]
    V --> W[View Resume Analysis and Match Score]
    W --> X[Update Application Status]
```

## 2. User Role Flow

```mermaid
flowchart LR
    subgraph Candidate
        C1[Register/Login] --> C2[Complete Profile]
        C2 --> C3[Upload Resume]
        C3 --> C4[Get AI Resume Analysis]
        C4 --> C5[Search Jobs]
        C5 --> C6[Apply]
        C6 --> C7[Track Application Status]
    end

    subgraph Recruiter
        R1[Register/Login] --> R2[Create Company/Profile]
        R2 --> R3[Post Job]
        R3 --> R4[View Applications]
        R4 --> R5[Check AI Match Score]
        R5 --> R6[Shortlist/Reject/Hire]
    end
```

## 3. Database Design ERD

```mermaid
erDiagram
    USER ||--o| CANDIDATE_PROFILE : has
    USER ||--o| RECRUITER_PROFILE : has
    USER ||--o{ JOB : posts
    USER ||--o{ APPLICATION : applies
    JOB ||--o{ APPLICATION : receives
    CANDIDATE_PROFILE ||--o{ RESUME : owns
    RESUME ||--o{ APPLICATION : used_for

    USER {
        bigint id PK
        string name
        string email UK
        string passwordHash
        enum role "CANDIDATE or RECRUITER"
        datetime createdAt
        datetime updatedAt
    }

    CANDIDATE_PROFILE {
        bigint id PK
        bigint userId FK
        string phone
        string location
        string linkedinUrl
        string githubUrl
        string portfolioUrl
        string bio
        datetime createdAt
        datetime updatedAt
    }

    RECRUITER_PROFILE {
        bigint id PK
        bigint userId FK
        string companyName
        string companyWebsite
        string designation
        string companyDescription
        datetime createdAt
        datetime updatedAt
    }

    RESUME {
        bigint id PK
        bigint candidateProfileId FK
        string fileUrl
        string originalFileName
        text extractedText
        json aiSkills
        text aiSummary
        json aiExperience
        datetime uploadedAt
    }

    JOB {
        bigint id PK
        bigint recruiterId FK
        string title
        string companyName
        string location
        enum jobType "FULL_TIME, PART_TIME, INTERNSHIP, CONTRACT"
        enum workMode "ONSITE, REMOTE, HYBRID"
        text description
        text requirements
        string salaryRange
        boolean isActive
        datetime createdAt
        datetime updatedAt
    }

    APPLICATION {
        bigint id PK
        bigint jobId FK
        bigint candidateId FK
        bigint resumeId FK
        enum status "APPLIED, REVIEWING, SHORTLISTED, REJECTED, HIRED"
        int matchScore
        text matchReasoning
        json missingSkills
        json strengths
        datetime appliedAt
        datetime updatedAt
    }
```

## 4. Microservices Architecture

```mermaid
flowchart TD
    A[React Frontend] --> B[API Gateway]
    B --> C[Auth Service]
    B --> D[Profile Service]
    B --> E[Job Service]
    B --> F[Resume Service]
    B --> G[Application Service]

    C --> H[(Auth DB)]
    D --> I[(Profile DB)]
    E --> J[(Job DB)]
    F --> K[(Resume DB)]
    G --> L[(Application DB)]

    F --> M[Apache PDFBox]
    F --> N[Cloud File Storage]
    F --> O[AI Service]
    G --> P[Message Broker]
    P --> O
    O --> Q[Claude API]
    O --> R[(AI Analysis DB)]
```

Each service is a separate Spring Boot application with its own database/schema. The API Gateway is the only public backend entry point; it routes requests, validates JWTs, and applies shared concerns such as CORS and rate limiting. Services communicate synchronously through internal REST APIs when an immediate response is required, and asynchronously through a message broker for long-running AI work.

| Service | Responsibility | Owns data |
| --- | --- | --- |
| API Gateway | Request routing, JWT validation, CORS, rate limiting | None |
| Auth Service | Registration, login, token generation, user roles | Users and credentials |
| Profile Service | Candidate and recruiter profiles | Candidate/recruiter profiles |
| Job Service | Job creation, search, updates, deactivation | Jobs |
| Resume Service | File upload, PDF text extraction, resume records | Resumes and file references |
| AI Service | Resume analysis and job-match generation using Claude | AI analyses and processing status |
| Application Service | Applications and recruiter status changes | Applications |

### Asynchronous AI Processing

Resume analysis and match scoring can take longer than a normal API request, so they should run asynchronously. Resume Service or Application Service publishes an event such as `resume.uploaded` or `application.created` to the message broker. AI Service consumes the event, calls Claude, saves the result, and publishes `resume.analyzed` or `application.match-scored`. This keeps upload/application APIs responsive and supports safe retries if the AI provider is temporarily unavailable.

## 5. AI Resume Analyzer Flow

```mermaid
sequenceDiagram
    participant Candidate
    participant Frontend
    participant Gateway
    participant ResumeService as Resume Service
    participant PDFParser
    participant Broker as Message Broker
    participant AIService as AI Service
    participant Claude
    participant ResumeDB as Resume DB

    Candidate->>Frontend: Upload resume PDF
    Frontend->>Gateway: POST /api/resumes/upload
    Gateway->>ResumeService: Route authenticated request
    ResumeService->>PDFParser: Extract text from PDF
    PDFParser-->>ResumeService: Resume text
    ResumeService->>ResumeDB: Save resume (PROCESSING)
    ResumeService-->>Gateway: Upload accepted
    Gateway-->>Frontend: Upload accepted
    ResumeService->>Broker: Publish resume.uploaded
    Broker->>AIService: Deliver event
    AIService->>Claude: Analyze resume text
    Claude-->>AIService: Summary, skills, experience, suggestions
    AIService->>ResumeDB: Save AI analysis (COMPLETED)
    Frontend-->>Candidate: Show AI resume insights
```

## 6. AI Match Score Flow

```mermaid
sequenceDiagram
    participant Candidate
    participant Frontend
    participant Gateway
    participant ApplicationService as Application Service
    participant JobService as Job Service
    participant ResumeService as Resume Service
    participant Broker as Message Broker
    participant AIService as AI Service
    participant Claude
    participant ApplicationDB as Application DB

    Candidate->>Frontend: Click Apply
    Frontend->>Gateway: POST /api/applications
    Gateway->>ApplicationService: Route authenticated request
    ApplicationService->>JobService: Fetch job description
    ApplicationService->>ResumeService: Fetch selected resume text
    ApplicationService->>ApplicationDB: Save application (MATCH_PENDING)
    ApplicationService-->>Gateway: Application submitted
    Gateway-->>Frontend: Application submitted
    ApplicationService->>Broker: Publish application.created
    Broker->>AIService: Deliver event
    AIService->>Claude: Generate match score
    Claude-->>AIService: Score, strengths, missing skills, reasoning
    AIService->>ApplicationDB: Save match result (MATCH_READY)
    Frontend-->>Candidate: Show applied status
```

## 7. Frontend Page Structure

```mermaid
flowchart TD
    A[App] --> B[Public Routes]
    A --> C[Candidate Routes]
    A --> D[Recruiter Routes]

    B --> B1[Login]
    B --> B2[Register]

    C --> C1[Candidate Dashboard]
    C --> C2[Profile Page]
    C --> C3[Resume Upload + Analysis]
    C --> C4[Job Search/List]
    C --> C5[Job Details]
    C --> C6[My Applications]

    D --> D1[Recruiter Dashboard]
    D --> D2[Create Job]
    D --> D3[My Jobs]
    D --> D4[Applications for Job]
    D --> D5[Candidate Review Page]
```

## 8. Main API Endpoints

All public endpoints are exposed through the API Gateway. The gateway forwards each request to its owning internal service; clients never call a service directly.

### Auth

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/auth/register` | Register candidate or recruiter |
| POST | `/api/auth/login` | Login and return JWT |
| GET | `/api/auth/me` | Get logged-in user |

### Candidate/Profile

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/api/candidates/profile` | Get candidate profile |
| PUT | `/api/candidates/profile` | Update candidate profile |
| POST | `/api/resumes/upload` | Upload resume and run AI analyzer |
| GET | `/api/resumes/me` | Get candidate resume analysis |

### Recruiter/Jobs

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/jobs` | Create job |
| GET | `/api/jobs` | List/search active jobs |
| GET | `/api/jobs/:id` | Get job details |
| PUT | `/api/jobs/:id` | Update job |
| DELETE | `/api/jobs/:id` | Deactivate job |

### Applications

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/applications` | Apply to job and generate AI match score |
| GET | `/api/applications/me` | Candidate application tracking |
| GET | `/api/jobs/:id/applications` | Recruiter views applications for a job |
| PATCH | `/api/applications/:id/status` | Recruiter updates application status |

## 9. Recommended Tech Stack

| Layer | Technology |
| --- | --- |
| Frontend | React + Vite |
| Backend | Java + Spring Boot |
| Architecture | Spring Boot microservices + API Gateway |
| Database | PostgreSQL |
| Service Communication | Internal REST + RabbitMQ/Kafka events |
| ORM | Spring Data JPA + Hibernate |
| Auth | Spring Security + JWT + BCrypt |
| File Upload | Spring MultipartFile |
| PDF Text Extraction | Apache PDFBox |
| AI | Claude API |
| Styling | Tailwind CSS |
| Deployment | Render/Railway/Supabase |

## 10. Folder Structure

```text
SmartHire-AI/
  backend/
    api-gateway/
    auth-service/
      src/main/java/com/smarthire/auth/
    profile-service/
      src/main/java/com/smarthire/profile/
    job-service/
      src/main/java/com/smarthire/job/
    resume-service/
      src/main/java/com/smarthire/resume/
    application-service/
      src/main/java/com/smarthire/application/
    ai-service/
      src/main/java/com/smarthire/ai/
    shared-contracts/
      events/
      dto/
    docker-compose.yml

  frontend/
    src/
      api/
      components/
      context/
      pages/
      routes/
      styles/
      App.jsx
      main.jsx
    package.json

  docs/
    project-design.md
    api-design.md
```

## 11. Day 1 Implementation Target

Day 1 should focus only on the foundation:

1. Create backend and frontend folders.
2. Setup Spring Boot backend project.
3. Configure PostgreSQL and create an isolated Auth Service database/schema.
4. Create Auth Service User entity with role enum and UserRepository.
5. Add register/login APIs and Spring Security JWT handling.
6. Add API Gateway routes for Auth Service.
7. Create Docker Compose configuration for PostgreSQL, gateway, and Auth Service.
8. Test gateway-to-auth APIs with Postman/Thunder Client.

## 12. Interview Explanation Pitch

"SmartHire AI is a Java full-stack recruitment platform where candidates can upload resumes and recruiters can post jobs. The backend uses Spring Boot microservices behind an API Gateway: Auth, Profile, Job, Resume, Application, and AI services. Each service owns its database/schema, which keeps domains independently deployable and prevents direct cross-service database access. Services use internal REST calls for immediate reads and event-driven messaging for long-running AI jobs. Resume uploads are parsed with Apache PDFBox and analyzed by Claude asynchronously; application match scoring is also processed asynchronously, then the score, strengths, missing skills, and reasoning are saved for recruiters and candidates. The frontend is built with React and Vite, with role-based dashboards for candidates and recruiters."
