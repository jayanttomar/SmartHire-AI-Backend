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

## 4. Backend Architecture

```mermaid
flowchart TD
    A[React Frontend] --> B[REST API]

    B --> C[Spring Security + JWT]
    B --> D[Controller Layer]
    D --> E[Service Layer]
    E --> F[Spring Data JPA Repositories]
    F --> G[(PostgreSQL Database)]

    E --> H[Resume Service]
    H --> I[Apache PDFBox Text Extractor]
    I --> J[Claude AI Service]
    J --> K[Claude API]

    H --> L[Local/Cloud File Storage]
    E --> M[Job Service]
    E --> N[Application Service]
    N --> J
```

## 5. AI Resume Analyzer Flow

```mermaid
sequenceDiagram
    participant Candidate
    participant Frontend
    participant Backend
    participant PDFParser
    participant Claude
    participant Database

    Candidate->>Frontend: Upload resume PDF
    Frontend->>Backend: POST /api/resumes/upload
    Backend->>PDFParser: Extract text from PDF
    PDFParser-->>Backend: Resume text
    Backend->>Claude: Analyze resume text
    Claude-->>Backend: Summary, skills, experience, suggestions
    Backend->>Database: Save resume + AI analysis
    Backend-->>Frontend: Resume analysis response
    Frontend-->>Candidate: Show AI resume insights
```

## 6. AI Match Score Flow

```mermaid
sequenceDiagram
    participant Candidate
    participant Frontend
    participant Backend
    participant Claude
    participant Database

    Candidate->>Frontend: Click Apply
    Frontend->>Backend: POST /api/applications
    Backend->>Database: Fetch job description
    Backend->>Database: Fetch candidate resume text
    Backend->>Claude: Compare resume with job description
    Claude-->>Backend: Match score, strengths, missing skills, reasoning
    Backend->>Database: Save application with AI match result
    Backend-->>Frontend: Application submitted
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
| Database | PostgreSQL |
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
    src/
      main/
        java/
          com/
            smarthire/
              SmartHireApplication.java
              config/
              controller/
              dto/
              entity/
              enums/
              exception/
              repository/
              security/
              service/
        resources/
          application.properties
    uploads/
    pom.xml

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
3. Connect PostgreSQL database.
4. Create User entity with role enum.
5. Add UserRepository with Spring Data JPA.
6. Add register/login APIs.
7. Add Spring Security JWT filter.
8. Test auth APIs with Postman/Thunder Client.

## 12. Interview Explanation Pitch

"SmartHire AI is a Java full-stack recruitment platform where candidates can upload resumes and recruiters can post jobs. I built the backend with Spring Boot, Spring Security, JWT authentication, Spring Data JPA, Hibernate, and PostgreSQL. I integrated Claude API in two places: first for resume analysis, where the uploaded PDF is converted into text using Apache PDFBox and sent to Claude for structured skill and summary extraction; second for AI match scoring, where Claude compares the resume with the job description and returns a match percentage, strengths, missing skills, and reasoning. The frontend is built with React and Vite, and the system uses role-based dashboards for candidates and recruiters."
