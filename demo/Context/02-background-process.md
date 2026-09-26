# memInfo — Background Process

## Objective

Implement the RAG application using the existing project instead of replacing its architecture.

The project already contains configuration for:

- MySQL
- Chroma
- Gemini AI
- Bootstrap
- application environment variables

Before coding, inspect the existing project structure, `pom.xml`, configuration files, controllers, services, repositories, entities, templates/static assets, and Chroma setup instructions.

Do not invent dependencies that are already available.

## Technology Direction

Use the technologies already present in the project.

Frontend:

- HTMX
- HTML
- CSS
- Bootstrap

Backend:

- Spring Boot
- Java
- Existing Spring Boot modules identified from `pom.xml`

Data:

- MySQL for application/chat persistence
- Chroma for vector storage

AI:

- Gemini Chat model
- Gemini Embedding model

## Spring Boot Feature Analysis Requirement

Before implementation, inspect `pom.xml` and determine exactly which Spring Boot features are already available.

Identify existing dependencies for categories such as:

- Spring Web / MVC
- Spring Boot Thymeleaf if present
- Spring Data JPA if present
- MySQL driver
- validation
- file upload / multipart support
- HTTP client
- security if present
- JSON serialization
- actuator if present
- test dependencies
- any AI/Gemini SDK
- any Chroma/vector database client
- any other project-specific dependency

Use only the required existing dependencies whenever possible.

If a required capability is missing, add the smallest appropriate dependency and explain why.

Do not replace Spring Boot with another backend framework.

## Request Flow

### PDF Upload

```text
Browser
  ↓ HTMX multipart request
Spring Boot Controller
  ↓
Upload Validation
  ↓
PDF Parser
  ↓
Text Extraction
  ↓
Chunking Service
  ↓
Gemini Embedding Service
  ↓
Chroma Service
  ↓
MySQL Metadata Service
  ↓
HTMX Status Response
```

### User Question

```text
Browser
  ↓ HTMX chat request
Chat Controller
  ↓
Chat Service
  ↓
Gemini Query Embedding
  ↓
Chroma Similarity Search
  ↓
Context Builder
  ↓
Gemini Chat Model
  ↓
Persist Message in MySQL
  ↓
HTMX Response Fragment
  ↓
Chat Window
```

## Background Processing

PDF indexing can take longer than a normal UI request.

Structure the application so that PDF processing can be moved to asynchronous/background execution without changing the public UI flow.

Recommended logical states:

```text
UPLOADED
PROCESSING
INDEXED
FAILED
```

During processing:

1. Store upload metadata.
2. Mark status as PROCESSING.
3. Extract text.
4. Chunk text.
5. Generate embeddings.
6. Store vectors in Chroma.
7. Mark the document INDEXED.
8. If an exception occurs, mark FAILED and store a safe error message.

Do not block the browser with unnecessary long-running operations if the existing project architecture supports asynchronous processing.

## Error Handling

Handle:

- invalid file type
- empty PDF
- unreadable PDF
- oversized file
- embedding failure
- Chroma unavailable
- Gemini API failure
- MySQL failure
- empty retrieval result
- malformed AI response

Return user-friendly messages through HTMX.

Never expose:

- API keys
- database passwords
- internal stack traces
- Chroma credentials
- raw server configuration

## Environment Variables

The existing configured environment variables are the source of truth.

Use the already configured:

```text
MySQL credentials
Chroma credentials/configuration
Gemini AI credentials
```

Do not hard-code credentials.

Do not create duplicate configuration keys unless the existing project requires them.

## Security Basics

- Validate uploaded files.
- Restrict accepted content to PDF.
- Configure upload size limits.
- Never expose secrets to browser JavaScript.
- Keep Gemini and Chroma credentials server-side.
- Validate chat/session identifiers.
- Use parameterized/JPA database operations.
- Escape/sanitize rendered user-controlled content where appropriate.
