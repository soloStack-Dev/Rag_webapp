# memInfo — Development Log & Known Issues

App: Enterprise Personal RAG (`memInfo`)
Stack: Spring Boot 4.1.1 / Java 21 / Spring AI 2.0.1 / Gemini / Chroma / MySQL / Thymeleaf + HTMX

## Issues found and fixed

### 0. Gemini chat quota exceeded on 2026-09-25
- Symptom: chat requests failed with HTTP `429` for `gemini-3.8-flash`, reporting the free-tier limit of 20 `generate_content` requests.
- Cause: the API key's project quota was exhausted; PDF indexing and Chroma retrieval were not the failing steps.
- Fix: the app now shows a quota-specific assistant message and logs the concise root cause. Wait for the quota reset or enable billing/increase quota.
- Model recommendation: keep `gemini-3.8-flash` for this key because the previously tested `gemini-2.5-flash` returned `404` and changing models does not guarantee additional quota.

### 1. PDF source highlighting in the chat sidebar
- Symptom: indexed documents appeared in the Documents section but were not visually connected to the sources shown under assistant answers.
- Fix: document items now expose their filename, source lines expose their source label, and the browser highlights documents cited by the current chat workspace.

### 2. Gemini model 404: `gemini-2.5-flash` and `text-embedding-004` not available for this API key
- Symptom: `404` / model not found when calling chat and embedding endpoints.
- Fix: switched to `gemini-3.8-flash` (chat) and `gemini-embedding-2` (embeddings). Verified working via `EmbeddingVectorStoreSmokeTests` (single + batch embed) and a live chat request in the smoke test.
- Keys live in `.env` as `GEMINI_CHAT_MODEL` / `GEMINI_EMBEDDING_MODEL` and are read in `application.properties`.

### 2. Spring AI 2.0.1 `Document` API: `getContent()` removed
- Symptom: compile error — `Document` has no `getContent()` in Spring AI 2.0.1.
- Fix: use `document.getText()` (3 call sites: `PdfParsingService`, `ChunkingService`, `DocumentIngestionService`).

### 3. Chroma base URL needs an explicit scheme
- Symptom: Chroma client failed with "invalid URI scheme" / connection errors pointing at `api.trychroma.com`.
- Root cause: `spring-ai` builds the base URL as `String.format("%s:%s", host, port)`, so a bare host (no `https://`) produces an invalid URI.
- Fix: `.env` uses `CHROMA_HOST=https://api.trychroma.com`. Default in `application.properties` is `https://localhost`.

### 4. `.env` must be loaded from the process working directory
- Symptom: server started but DB login failed as `root@localhost` ("Access denied ... using password: NO").
- Root cause: Spring Boot 4 has no built-in `.env` loader; we use `spring.config.import=optional:file:.env[.properties]`, which resolves relative to the working directory. The earlier `Start-Process` launched java from the wrong directory.
- Fix: launch with `Start-Process java -jar target\rag-app-0.0.1-SNAPSHOT.jar -WorkingDirectory <rag-app>`, or run `java -jar` from inside `rag-app/ rag-app/`.

### 5. Thymeleaf: `session` is a reserved word; fragment-parameter binding must be explicit
- Symptom: `GET /chat` → 500 `Cannot set variable called 'session' into web variables map`; after that, `Exception evaluating SpringEL expression: "sess.id"` and `"msg.role"` with `Property or field ... cannot be found on null`.
- Root cause: Thymeleaf 3.1 disallows `session` (etc.) as a context variable name because it shadows the web context; also, positional fragment-argument passing (`:: fragment(${x})`) did not bind the parameter into the fragment (evaluated as null). Both `history-item` and `message` fragments were affected.
- Fix: renamed the model/fragment variable from `session` to `sess` everywhere, and switched every parametrized fragment call to explicit named arguments: `:: item(sess=${sess})`, `:: message(msg=${msg})`, `:: doc-item(doc=${doc})`.

### 6. PDFBox 3.x: `PDType1Font.HELVETICA` removed
- Symptom: compile error generating sample PDFs.
- Fix: `new PDType1Font(Standard14Fonts.FontName.HELVETICA)` (PDFBox 3.0.7) and add `pdfbox-io` + `commons-logging` to the test classpath.

### 7. Java server process is reaped when the launching shell command returns
- Symptom: the dev server died as soon as the bash tool call ended.
- Fix: run launch + smoke checks + shutdown inside one long-lived PowerShell command (`Start-Process` with `-PassThru`, poll, `Stop-Process`), and add the `-WorkingDirectory` parameter (see #4).

## Config keys (verified working)

- `spring.ai.google.genai.api-key` → `GEMINI_API_KEY`
- `spring.ai.google.genai.chat.model` → `gemini-3.8-flash`
- `spring.ai.google.genai.embedding.api-key` → `GEMINI_API_KEY`
- `spring.ai.google.genai.embedding.text.model` → `gemini-embedding-2`
- `spring.ai.vectorstore.chroma.client.host` → `https://api.trychroma.com` (scheme required)
- `spring.ai.vectorstore.chroma.client.port` → `443`
- `spring.ai.vectorstore.chroma.client.key-token` → `CHROMA_API_KEY`
- `spring.ai.vectorstore.chroma.client.tenant-name`, `database-name`, `collection-name` → `rag-app`
- `spring.ai.vectorstore.chroma.initialize-schema=true` (safe; collection existence is checked)
- MySQL: `jdbc:mysql://{host}/{db}?useSSL=true&sslMode=REQUIRED&allowPublicKeyRetrieval=true&serverTimezone=UTC`

## Known issues / TODO

- [IN PROGRESS] Final runtime smoke test of `POST /chat/message` answering from an uploaded PDF (fragment-binding fix above applied; re-run smoke test and confirm answer contains Chroma + Sources).
- Server takes ~40s to start and needs `-WorkingDirectory` (see #4, #7).

## Test / build commands

- `.\mvnw.cmd test` → 2 tests, 0 failures (`RagAppApplicationTests` context load + `EmbeddingVectorStoreSmokeTests` against real Chroma/Gemini).
- `.\mvnw.cmd package -DskipTests` → BUILD SUCCESS.
### 8. Containerize the app (Docker + Compose) and decouple MySQL creds

- Issue: Local dev used oot@localhost (hard-coded in pplication.properties) and the demo had its own compose.yaml referencing .env paths. Docker networking requires the service name (mysql) as MYSQL_HOST. The existing Dockerfile exposed 10000 but server.port defaults to 8080.
- Fix:
  - Updated demo/src/main/resources/application.properties to read spring.datasource.* from MYSQL_* env vars (defaults: host mysql, port 3306, db mydb, user user, pass pass123). Kept sslMode removed to align with plain Docker MySQL (Aiven-style SSL only needed for cloud). Also preserved all Gemini/Chroma/RAG settings and spring.config.import=optional:file:.env[.properties].
  - Changed demo/Dockerfile EXPOSE from 10000 to 8080 to match server.port=.
  - Created root docker-compose.yml with mysql:8.0 (service mysql, named volumes, healthcheck using mysqladmin ping, network meminfo-net, published 3306 host->3306), and pp built from ./demo using the existing Dockerfile, injecting MYSQL_*/PORT, mounting ./demo/.env as /app/.env:ro, depends_on with service_healthy, published 8080:8080.
  - Removed superseded demo/compose.yaml.
- Verification: docker compose up --build brought up healthy meminfo-mysql and meminfo-rag-app (Tomcat on 8080), GET http://localhost:8080/ returned 200 OK.

