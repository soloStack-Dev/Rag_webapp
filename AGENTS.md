# AGENTS.md

Spring Boot 4.1.1 + Spring AI 2.0.1 (Google GenAI chat/embeddings, Chroma vector store, PDF/Tika readers, MySQL/JPA, Thymeleaf + HTMX). A working "memInfo" enterprise RAG app: upload a PDF in the UI, it is parsed/chunked/embedded and indexed into Chroma, then chat with Gemini against the indexed docs. See `rag-app/rag-app/devlog/DEVELOPMENT_LOG.md` for every error/fix and the verified config keys.

## Layout

- The Maven project lives at `rag-app/rag-app/` (double-nested). Run all Maven commands from there; don't create files at the outer level.
- `devlog/DEVELOPMENT_LOG.md` — development history, known issues, verified config.

## Commands (Windows / PowerShell)

- Build/test: `.\mvnw.cmd test` or `.\mvnw.cmd package` (run inside `rag-app/rag-app/`). The bare `./mvnw` shell script won't run from PowerShell.
- Java 21 required (`pom.xml`). No other toolchain config exists.

## Package conventions

- Root package is `com.example.rag_app` — underscore, because `rag-app` is an invalid Java package name (noted in `HELP.md`).
- Subpackages are PascalCase: `Controller`, `Service`, `Repository`, `Entity`, `Config`. Match whichever package style the surrounding code uses.

## Runtime requirements

All secrets + infra URLs come from `.env` in `rag-app/rag-app/` and are injected via `spring.config.import=optional:file:.env[.properties]` in `application.properties`. Because that import is relative to the **process working directory**, the app must be launched from `rag-app/rag-app/` (e.g. `Start-Process java -jar target\...jar -WorkingDirectory <rag-app/rag-app>`). Missing keys/URLs → startup or runtime failure.

Verified/in-use pieces:
- Google GenAI: chat model `gemini-3.8-flash`, embedding model `gemini-embedding-2` (`gemini-2.5-flash`/`text-embedding-004` 404 for this key — do not switch back). See context.properties prefixes: `spring.ai.google.genai.*`.
- Chroma: cloud API at `https://api.trychroma.com`, port `443`, tenant/database/collection diff, `initialize-schema=true`. **The `host` must include the `https://` scheme** — spring-ai builds the base URL as `host:port`, so a bare host breaks. Prefix: `spring.ai.vectorstore.chroma.client.*`.
- MySQL: Aiven (`sslMode=REQUIRED`). Prefix: `spring.datasource.url`.

## UI/HTMX conventions

- All fragment swaps are targeted: history lists `hx-target="#sidebar-history"` / `outerHTML`; chat workspace `hx-target="#chat-workspace"` / `innerHTML`; rename/delete target `closest li`.
- **Thymeleaf rules learned the hard way (use these):**
  - `session` is a reserved word — never use it as a model/fragment variable name (use `sess`).
  - Parametrized fragments (`th:fragment="message(msg)"`) MUST be invoked with explicit named args: `~{fragments/message :: message(msg=${msg})}`. Positional args (`:: message(${msg})`) silently bind null.
- New-session/message/edit/delete responses reuse the fragment templates (`fragments/workspace`, `fragments/history-item`, ...) so HTMX swaps stay consistent.

## Testing

- `.\mvnw.cmd test` runs 2 tests: default `@SpringBootTest contextLoads` + `EmbeddingVectorStoreSmokeTests` (real Chroma add/search/delete + Gemini single/batch embed; cleans up after itself). Both hit real infrastructure (no mocks).
- PDF uploads are validated (PDF only, 20MB max). Async indexing status: `UPLOADED → PROCESSING → INDEXED/FAILED`, polled from the UI via `GET /documents/{id}/status` (JS in `static/js/chat.js`).